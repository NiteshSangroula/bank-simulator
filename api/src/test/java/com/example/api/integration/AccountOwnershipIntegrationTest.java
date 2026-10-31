package com.example.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountOwnershipIntegrationTest {

  private static final String OWNER_EMAIL = "owner@example.com";
  private static final String OTHER_EMAIL = "other@example.com";

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

  @BeforeEach
  void setUp() {
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
    userRepository.deleteAll();
    owner = userRepository.saveAndFlush(newUser("Owner", OWNER_EMAIL));
    otherUser = userRepository.saveAndFlush(newUser("Other", OTHER_EMAIL));
  }

  @Test
  void listingReturnsOnlyAccountsOwnedByAuthenticatedUser() throws Exception {
    Account ownedAccount = saveAccount(owner, "Owned");
    saveAccount(otherUser, "Other user's account");

    mockMvc.perform(get("/api/accounts").with(user(OWNER_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(ownedAccount.getId()))
        .andExpect(jsonPath("$[0].name").value("Owned"));
  }

  @Test
  void createAssignsAuthenticatedOwnerAndIgnoresClientOwnerId() throws Exception {
    mockMvc.perform(post("/api/accounts")
        .with(user(OWNER_EMAIL))
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "name": "New account",
              "initialAmount": 100,
              "userId": %d
            }
            """.formatted(otherUser.getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("New account"));

    Account created = accountRepository.findAllByUserEmail(OWNER_EMAIL).stream()
        .filter(account -> account.getName().equals("New account"))
        .findFirst()
        .orElseThrow();
    assertEquals(owner.getId(), created.getUser().getId());
  }

  @Test
  void ownerCanRetrieveAndDeleteAccountButCannotAccessAnotherUsersAccount() throws Exception {
    Account ownedAccount = saveAccount(owner, "Owned", BigDecimal.ZERO);
    Account foreignAccount = saveAccount(otherUser, "Foreign", BigDecimal.ZERO);

    mockMvc.perform(get("/api/accounts/{id}", ownedAccount.getId()).with(user(OWNER_EMAIL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ownedAccount.getId()));

    mockMvc.perform(get("/api/accounts/{id}", foreignAccount.getId()).with(user(OWNER_EMAIL)))
        .andExpect(status().isNotFound());

    mockMvc.perform(delete("/api/accounts/{id}", ownedAccount.getId())
        .with(user(OWNER_EMAIL))
        .with(csrf()))
        .andExpect(status().isNoContent());
    assertTrue(accountRepository.findById(ownedAccount.getId()).isEmpty());

    mockMvc.perform(delete("/api/accounts/{id}", foreignAccount.getId())
        .with(user(OWNER_EMAIL))
        .with(csrf()))
        .andExpect(status().isNotFound());
    assertTrue(accountRepository.findById(foreignAccount.getId()).isPresent());
  }

  @Test
  void accountWithNonzeroBalanceCannotBeDeleted() throws Exception {
    Account account = saveAccount(owner, "Funded", new BigDecimal("25.00"));

    mockMvc.perform(delete("/api/accounts/{id}", account.getId())
        .with(user(OWNER_EMAIL))
        .with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(
            "Account cannot be deleted while it has a nonzero balance or transaction history"));

    assertTrue(accountRepository.findById(account.getId()).isPresent());
  }

  @Test
  void accountWithTransactionHistoryCannotBeDeletedEvenWithZeroBalance() throws Exception {
    Account account = saveAccount(owner, "Used", BigDecimal.ZERO);
    Transaction transaction = new Transaction();
    transaction.setAmount(new BigDecimal("10.00"));
    transaction.setType(TransactionType.DEPOSIT);
    transaction.setToAccountId(account.getId());
    transaction.setCreatedAt(LocalDateTime.now());
    transaction.setDescription("Existing history");
    transaction = transactionRepository.saveAndFlush(transaction);

    mockMvc.perform(delete("/api/accounts/{id}", account.getId())
        .with(user(OWNER_EMAIL))
        .with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(
            "Account cannot be deleted while it has a nonzero balance or transaction history"));

    assertTrue(accountRepository.findById(account.getId()).isPresent());
    assertTrue(transactionRepository.findById(transaction.getId()).isPresent());
  }

  private User newUser(String displayName, String email) {
    User user = new User();
    user.setDisplayName(displayName);
    user.setEmail(email);
    user.setPasswordHash("not-used");
    return user;
  }

  private Account saveAccount(User owner, String name) {
    return saveAccount(owner, name, new BigDecimal("100.00"));
  }

  private Account saveAccount(User owner, String name, BigDecimal balance) {
    Account account = new Account();
    account.setUser(owner);
    account.setName(name);
    account.setBalance(balance);
    account.setCreatedAt(LocalDateTime.now());
    return accountRepository.saveAndFlush(account);
  }
}
