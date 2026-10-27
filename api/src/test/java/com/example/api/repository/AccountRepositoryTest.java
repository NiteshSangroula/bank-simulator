package com.example.api.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.api.entity.Account;
import com.example.api.entity.User;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void savesAccountWithGeneratedId() {
    User owner = userRepository.saveAndFlush(newOwner());
    Account savedAccount = accountRepository.saveAndFlush(newAccount("100.00", owner));

    assertNotNull(savedAccount.getId());
    assertEquals(0, new BigDecimal("100.00").compareTo(savedAccount.getBalance()));
    assertEquals("Checking", accountRepository.findById(savedAccount.getId()).orElseThrow().getName());
    assertEquals(owner.getId(), savedAccount.getUser().getId());
  }

  @Test
  void changesBalanceOnlyWhenUpdatedBalanceIsNonnegative() {
    User owner = userRepository.saveAndFlush(newOwner());
    Account account = accountRepository.saveAndFlush(newAccount("100.00", owner));

    assertEquals(1, accountRepository.changeBalance(account.getId(), new BigDecimal("-25.00")));
    assertTrue(accountRepository.changeBalance(account.getId(), new BigDecimal("-100.00")) <= 0);
    assertEquals(0, new BigDecimal("75.00").compareTo(
        accountRepository.findById(account.getId()).orElseThrow().getBalance()));
  }

      @Test
      void changesBalanceOnlyForMatchingOwner() {
      User owner = userRepository.saveAndFlush(newOwner());
      User other = new User();
      other.setDisplayName("Other User");
      other.setEmail("other@example.com");
      other.setPasswordHash("not-used");
      userRepository.saveAndFlush(other);
      Account account = accountRepository.saveAndFlush(newAccount("100.00", owner));

      assertEquals(0, accountRepository.changeBalanceForOwner(
        account.getId(), "other@example.com", new BigDecimal("25.00")));
      assertEquals(0, new BigDecimal("100.00").compareTo(
        accountRepository.findById(account.getId()).orElseThrow().getBalance()));

      assertEquals(1, accountRepository.changeBalanceForOwner(
        account.getId(), owner.getEmail(), new BigDecimal("25.00")));
      assertEquals(0, new BigDecimal("125.00").compareTo(
        accountRepository.findById(account.getId()).orElseThrow().getBalance()));
      }

  private Account newAccount(String balance, User owner) {
    Account account = new Account();
    account.setUser(owner);
    account.setName("Checking");
    account.setBalance(new BigDecimal(balance));
    account.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
    return account;
  }

  private User newOwner() {
    User user = new User();
    user.setDisplayName("Test User");
    user.setEmail("owner@example.com");
    user.setPasswordHash("not-used");
    return user;
  }
}