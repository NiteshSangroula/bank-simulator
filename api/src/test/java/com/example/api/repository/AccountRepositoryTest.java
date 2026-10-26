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

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

  @Autowired
  private AccountRepository accountRepository;

  @Test
  void savesAccountWithGeneratedId() {
    Account savedAccount = accountRepository.saveAndFlush(newAccount("100.00"));

    assertNotNull(savedAccount.getId());
    assertEquals(0, new BigDecimal("100.00").compareTo(savedAccount.getBalance()));
    assertEquals("Checking", accountRepository.findById(savedAccount.getId()).orElseThrow().getName());
  }

  @Test
  void changesBalanceOnlyWhenUpdatedBalanceIsNonnegative() {
    Account account = accountRepository.saveAndFlush(newAccount("100.00"));

    assertEquals(1, accountRepository.changeBalance(account.getId(), new BigDecimal("-25.00")));
    assertTrue(accountRepository.changeBalance(account.getId(), new BigDecimal("-100.00")) <= 0);
    assertEquals(0, new BigDecimal("75.00").compareTo(
        accountRepository.findById(account.getId()).orElseThrow().getBalance()));
  }

  private Account newAccount(String balance) {
    Account account = new Account();
    account.setName("Checking");
    account.setBalance(new BigDecimal(balance));
    account.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
    return account;
  }
}