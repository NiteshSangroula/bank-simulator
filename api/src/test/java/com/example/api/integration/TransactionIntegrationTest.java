package com.example.api.integration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.api.entity.Account;
import com.example.api.entity.Transaction;
import com.example.api.entity.TransactionType;
import com.example.api.entity.User;
import com.example.api.repository.AccountRepository;
import com.example.api.repository.TransactionRepository;
import com.example.api.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TransactionIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User otherUser;
    private User thirdUser;

  @BeforeEach
  void setup() {
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
    userRepository.deleteAll();

    owner = new User();
    owner.setDisplayName("Integration User");
    owner.setEmail("integration@example.com");
    owner.setPasswordHash("not-used");
    owner = userRepository.saveAndFlush(owner);

    otherUser = new User();
    otherUser.setDisplayName("Other User");
    otherUser.setEmail("other@example.com");
    otherUser.setPasswordHash("not-used");
    otherUser = userRepository.saveAndFlush(otherUser);

    thirdUser = new User();
    thirdUser.setDisplayName("Third User");
    thirdUser.setEmail("third@example.com");
    thirdUser.setPasswordHash("not-used");
    thirdUser = userRepository.saveAndFlush(thirdUser);
  }

  // Transfer Success
  @Test
  void transfer_success() throws Exception {
    Account from = new Account();
    from.setUser(owner);
    from.setName("A");
    from.setBalance(BigDecimal.valueOf(1000));
    from.setCreatedAt(LocalDateTime.now());

    Account to = new Account();
    to.setUser(owner);
    to.setName("B");
    to.setBalance(BigDecimal.valueOf(500));
    to.setCreatedAt(LocalDateTime.now());

    accountRepository.save(from);
    accountRepository.save(to);

    mockMvc.perform(post("/api/transactions/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
                {
                "fromAccountId": %d,
                "toAccountId": %d,
                "amount": 200,
                "description": "test"
                }
                """.formatted(from.getId(), to.getId()))
            .with(user(owner.getEmail()))
            .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.amount").value(200));

    Account updatedFrom = accountRepository.findById(from.getId()).get();

    Account updatedTo = accountRepository.findById(to.getId()).get();

    Iterable<Transaction> transactions = transactionRepository.findAll();

    Transaction transaction = transactions.iterator().next();

    assertEquals(
        0,
        BigDecimal.valueOf(200)
            .compareTo(transaction.getAmount()));

    assertEquals(
        from.getId(), transaction.getFromAccountId());

    assertEquals(
        to.getId(), transaction.getToAccountId());

    assertEquals(
        0,
        BigDecimal.valueOf(800)
            .compareTo(updatedFrom.getBalance()));

    assertEquals(
        0,
        BigDecimal.valueOf(700)
            .compareTo(updatedTo.getBalance()));
  }

    @Test
    void transferFromOwnAccountToAnotherUsersAccountSucceeds() throws Exception {
        Account from = saveAccount(owner, "Sender", BigDecimal.valueOf(1000));
        Account to = saveAccount(otherUser, "Recipient", BigDecimal.valueOf(500));

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "fromAccountId": %d,
                            "toAccountId": %d,
                            "amount": 200,
                            "description": "cross-user transfer"
                        }
                        """.formatted(from.getId(), to.getId()))
                .with(user(owner.getEmail()))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.fromAccountId").value(from.getId()))
                .andExpect(jsonPath("$.toAccountId").value(to.getId()));

        assertEquals(0, BigDecimal.valueOf(800).compareTo(
                accountRepository.findById(from.getId()).orElseThrow().getBalance()));
        assertEquals(0, BigDecimal.valueOf(700).compareTo(
                accountRepository.findById(to.getId()).orElseThrow().getBalance()));

        Transaction transaction = transactionRepository.findAll().get(0);
        assertEquals(1, transactionRepository.count());
        assertEquals(TransactionType.TRANSFER, transaction.getType());
        assertEquals(from.getId(), transaction.getFromAccountId());
        assertEquals(to.getId(), transaction.getToAccountId());
        assertEquals("cross-user transfer", transaction.getDescription());
    }

  // Transfer Fails
  @Test
  void transfer_insufficientBalance_shouldFailAndRollback() throws Exception {

    Account from = new Account();
    from.setUser(owner);
    from.setName("A");
    from.setBalance(BigDecimal.valueOf(100));
    from.setCreatedAt(LocalDateTime.now());

    Account to = new Account();
    to.setUser(owner);
    to.setName("B");
    to.setBalance(BigDecimal.valueOf(500));
    to.setCreatedAt(LocalDateTime.now());

    accountRepository.save(from);
    accountRepository.save(to);

    mockMvc.perform(post("/api/transactions/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
            "fromAccountId": %d,
            "toAccountId": %d,
            "amount": 200,
            "description": "fail test"
            }
            """.formatted(from.getId(), to.getId()))
        .with(user(owner.getEmail()))
        .with(csrf()))
        .andExpect(status().isBadRequest());

    Account updatedFrom = accountRepository.findById(from.getId()).get();

    Account updatedTo = accountRepository.findById(to.getId()).get();

    assertEquals(0,
        BigDecimal.valueOf(100)
            .compareTo(updatedFrom.getBalance()));

    assertEquals(0,
        BigDecimal.valueOf(500)
            .compareTo(updatedTo.getBalance()));

    assertEquals(0, transactionRepository.count());

  }

    @Test
    void transferFromAnotherUsersAccountToOwnAccountIsRejectedWithoutChanges() throws Exception {
        Account from = saveAccount(otherUser, "Foreign source", BigDecimal.valueOf(500));
        Account to = saveAccount(owner, "Owned destination", BigDecimal.valueOf(300));

        performTransfer(from, to)
                .andExpect(status().isNotFound());

        assertUnchangedAndNoTransaction(from, to, BigDecimal.valueOf(500), BigDecimal.valueOf(300));
    }

    @Test
    void transferFromAnotherUsersAccountToThirdUsersAccountIsRejectedWithoutChanges() throws Exception {
        Account from = saveAccount(otherUser, "Foreign source", BigDecimal.valueOf(500));
        Account to = saveAccount(thirdUser, "Third-party destination", BigDecimal.valueOf(300));

        performTransfer(from, to)
                .andExpect(status().isNotFound());

        assertUnchangedAndNoTransaction(from, to, BigDecimal.valueOf(500), BigDecimal.valueOf(300));
    }

    @Test
    void transferToNonexistentDestinationIsRejectedWithoutChangingSource() throws Exception {
        Account from = saveAccount(owner, "Owned source", BigDecimal.valueOf(500));

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "fromAccountId": %d,
                            "toAccountId": 999999,
                            "amount": 200,
                            "description": "missing destination"
                        }
                        """.formatted(from.getId()))
                .with(user(owner.getEmail()))
                .with(csrf()))
                .andExpect(status().isNotFound());

        assertEquals(0, BigDecimal.valueOf(500).compareTo(
                accountRepository.findById(from.getId()).orElseThrow().getBalance()));
        assertEquals(0, transactionRepository.count());
    }

  // Deposit Success
  @Test
  void deposit_success() throws Exception {
    Account acc = new Account();
    acc.setUser(owner);
    acc.setName("A");
    acc.setBalance(BigDecimal.valueOf(500));
    acc.setCreatedAt(LocalDateTime.now());

    accountRepository.save(acc);

    mockMvc.perform(post("/api/transactions/deposit")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
            "toAccountId": %d,
            "amount": 200,
            "description": "deposit success"
            }
            """.formatted(acc.getId()))
        .with(user(owner.getEmail()))
        .with(csrf()))
        .andExpect(status().isCreated());

    Account updated = accountRepository.findById(acc.getId()).get();

    Iterable<Transaction> transactions = transactionRepository.findAll();

    Transaction transaction = transactions.iterator().next();

    assertEquals(
        0,
        BigDecimal.valueOf(200)
            .compareTo(transaction.getAmount()));

    assertEquals(0,
        BigDecimal.valueOf(700)
            .compareTo(updated.getBalance()));

    assertEquals(
        transaction.getToAccountId(), updated.getId());
  }

  // Withdraw Success
  @Test
  void withdraw_success() throws Exception {
    Account acc = new Account();
    acc.setUser(owner);
    acc.setName("A");
    acc.setBalance(BigDecimal.valueOf(500));
    acc.setCreatedAt(LocalDateTime.now());

    accountRepository.save(acc);

    mockMvc.perform(post("/api/transactions/withdraw")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
            "fromAccountId": %d,
            "amount": 200,
            "description": "withdraw success"
            }
            """.formatted(acc.getId()))
        .with(user(owner.getEmail()))
        .with(csrf()))
        .andExpect(status().isCreated());

    Account updated = accountRepository.findById(acc.getId()).get();

    Iterable<Transaction> transactions = transactionRepository.findAll();

    Transaction transaction = transactions.iterator().next();

    assertEquals(
        0,
        BigDecimal.valueOf(200)
            .compareTo(transaction.getAmount()));

    assertEquals(0,
        BigDecimal.valueOf(300)
            .compareTo(updated.getBalance()));

    assertEquals(
        transaction.getFromAccountId(), updated.getId());
  }

    @Test
    void deposit_intoAnotherUsersAccountReturnsNotFoundWithoutChangingState() throws Exception {
        Account account = new Account();
        account.setUser(otherUser);
        account.setName("Other user's account");
        account.setBalance(BigDecimal.valueOf(500));
        account.setCreatedAt(LocalDateTime.now());
        account = accountRepository.saveAndFlush(account);

        mockMvc.perform(post("/api/transactions/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "toAccountId": %d,
                            "amount": 200,
                            "description": "unauthorized deposit"
                        }
                        """.formatted(account.getId()))
                .with(user(owner.getEmail()))
                .with(csrf()))
                .andExpect(status().isNotFound());

        assertEquals(0, BigDecimal.valueOf(500).compareTo(
                accountRepository.findById(account.getId()).orElseThrow().getBalance()));
        assertEquals(0, transactionRepository.count());
    }

    @Test
    void withdrawFromAnotherUsersAccountReturnsNotFoundWithoutChangingState() throws Exception {
        Account account = new Account();
        account.setUser(otherUser);
        account.setName("Other user's account");
        account.setBalance(BigDecimal.valueOf(500));
        account.setCreatedAt(LocalDateTime.now());
        account = accountRepository.saveAndFlush(account);

        mockMvc.perform(post("/api/transactions/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "fromAccountId": %d,
                            "amount": 200,
                            "description": "unauthorized withdrawal"
                        }
                        """.formatted(account.getId()))
                .with(user(owner.getEmail()))
                .with(csrf()))
                .andExpect(status().isNotFound());

        assertEquals(0, BigDecimal.valueOf(500).compareTo(
                accountRepository.findById(account.getId()).orElseThrow().getBalance()));
        assertEquals(0, transactionRepository.count());
    }

    private org.springframework.test.web.servlet.ResultActions performTransfer(Account from, Account to)
            throws Exception {
        return mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "fromAccountId": %d,
                            "toAccountId": %d,
                            "amount": 200,
                            "description": "unauthorized transfer"
                        }
                        """.formatted(from.getId(), to.getId()))
                .with(user(owner.getEmail()))
                .with(csrf()));
    }

    private void assertUnchangedAndNoTransaction(
            Account from,
            Account to,
            BigDecimal fromBalance,
            BigDecimal toBalance) {
        assertEquals(0, fromBalance.compareTo(
                accountRepository.findById(from.getId()).orElseThrow().getBalance()));
        assertEquals(0, toBalance.compareTo(
                accountRepository.findById(to.getId()).orElseThrow().getBalance()));
        assertEquals(0, transactionRepository.count());
    }

    private Account saveAccount(User accountOwner, String name, BigDecimal balance) {
        Account account = new Account();
        account.setUser(accountOwner);
        account.setName(name);
        account.setBalance(balance);
        account.setCreatedAt(LocalDateTime.now());
        return accountRepository.saveAndFlush(account);
    }

}
