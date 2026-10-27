package com.example.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionHistoryIntegrationTest {

  private static final String ALICE_EMAIL = "alice@example.com";
  private static final String BOB_EMAIL = "bob@example.com";
  private static final String CHARLIE_EMAIL = "charlie@example.com";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private TransactionRepository transactionRepository;

  private Account aliceAccount;
  private Account aliceSecondAccount;
  private Account bobAccount;
  private Account charlieAccount;
  private Transaction aliceDeposit;
  private Transaction aliceWithdrawal;
  private Transaction aliceBobTransfer;
  private Transaction bobDeposit;
  private Transaction bobCharlieTransfer;
  private Transaction charlieWithdrawal;

  @BeforeEach
  void setUp() {
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
    userRepository.deleteAll();

    User alice = saveUser("Alice", ALICE_EMAIL);
    User bob = saveUser("Bob", BOB_EMAIL);
    User charlie = saveUser("Charlie", CHARLIE_EMAIL);

    aliceAccount = saveAccount(alice, "Alice checking");
    aliceSecondAccount = saveAccount(alice, "Alice savings");
    bobAccount = saveAccount(bob, "Bob checking");
    charlieAccount = saveAccount(charlie, "Charlie checking");

    aliceDeposit = saveTransaction(TransactionType.DEPOSIT, null, aliceAccount, 1);
    aliceWithdrawal = saveTransaction(TransactionType.WITHDRAW, aliceSecondAccount, null, 2);
    aliceBobTransfer = saveTransaction(TransactionType.TRANSFER, aliceAccount, bobAccount, 3);
    bobDeposit = saveTransaction(TransactionType.DEPOSIT, null, bobAccount, 4);
    bobCharlieTransfer = saveTransaction(TransactionType.TRANSFER, bobAccount, charlieAccount, 5);
    charlieWithdrawal = saveTransaction(TransactionType.WITHDRAW, charlieAccount, null, 6);
  }

  @Test
  void globalPagesContainOnlyTransactionsInvolvingOwnedAccountsAndCountOnlyThoseRows() throws Exception {
    mockMvc.perform(get("/api/transactions")
        .param("page", "0")
        .param("size", "2")
        .with(user(ALICE_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].id").value(aliceBobTransfer.getId()))
        .andExpect(jsonPath("$.content[1].id").value(aliceWithdrawal.getId()))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2));

    mockMvc.perform(get("/api/transactions")
        .param("page", "1")
        .param("size", "2")
        .with(user(ALICE_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].id").value(aliceDeposit.getId()))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2));

    mockMvc.perform(get("/api/transactions").with(user(BOB_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.content[0].id").value(bobCharlieTransfer.getId()))
        .andExpect(jsonPath("$.content[1].id").value(bobDeposit.getId()))
        .andExpect(jsonPath("$.content[2].id").value(aliceBobTransfer.getId()));
  }

  @Test
  void accountSpecificHistoryRequiresOwnershipAndIncludesEitherTransferDirection() throws Exception {
    mockMvc.perform(get("/api/transactions")
        .param("accountId", aliceAccount.getId().toString())
        .with(user(ALICE_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[0].id").value(aliceBobTransfer.getId()))
        .andExpect(jsonPath("$.content[1].id").value(aliceDeposit.getId()));

    mockMvc.perform(get("/api/transactions")
        .param("accountId", bobAccount.getId().toString())
        .with(user(ALICE_EMAIL)))
        .andExpect(status().isNotFound());
  }

  @Test
  void singleTransactionIsVisibleToEitherTransferOwnerButNotUnrelatedUsers() throws Exception {
    mockMvc.perform(get("/api/transactions/{id}", aliceBobTransfer.getId())
        .with(user(ALICE_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(aliceBobTransfer.getId()));

    mockMvc.perform(get("/api/transactions/{id}", aliceBobTransfer.getId())
        .with(user(BOB_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(aliceBobTransfer.getId()));

    mockMvc.perform(get("/api/transactions/{id}", bobDeposit.getId())
        .with(user(ALICE_EMAIL)))
        .andExpect(status().isNotFound());

    mockMvc.perform(get("/api/transactions/{id}", charlieWithdrawal.getId())
        .with(user(ALICE_EMAIL)))
        .andExpect(status().isNotFound());
  }

  @Test
  void eachOwnerSeesCrossUserTransferButNoTransactionsExclusiveToOtherUsers() throws Exception {
    mockMvc.perform(get("/api/transactions").with(user(CHARLIE_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content[0].id").value(charlieWithdrawal.getId()))
        .andExpect(jsonPath("$.content[1].id").value(bobCharlieTransfer.getId()));

    assertEquals(6, transactionRepository.count());
  }

  private User saveUser(String name, String email) {
    User user = new User();
    user.setDisplayName(name);
    user.setEmail(email);
    user.setPasswordHash("not-used");
    return userRepository.saveAndFlush(user);
  }

  private Account saveAccount(User owner, String name) {
    Account account = new Account();
    account.setUser(owner);
    account.setName(name);
    account.setBalance(new BigDecimal("100.00"));
    account.setCreatedAt(LocalDateTime.now());
    return accountRepository.saveAndFlush(account);
  }

  private Transaction saveTransaction(
      TransactionType type,
      Account fromAccount,
      Account toAccount,
      int day) {
    Transaction transaction = new Transaction();
    transaction.setAmount(new BigDecimal("25.00"));
    transaction.setType(type);
    transaction.setFromAccountId(fromAccount == null ? null : fromAccount.getId());
    transaction.setToAccountId(toAccount == null ? null : toAccount.getId());
    transaction.setCreatedAt(LocalDateTime.of(2024, 1, day, 12, 0));
    transaction.setDescription(type.name());
    return transactionRepository.saveAndFlush(transaction);
  }
}
