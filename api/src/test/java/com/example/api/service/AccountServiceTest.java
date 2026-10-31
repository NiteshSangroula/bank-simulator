package com.example.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.api.dto.request.CreateAccountRequest;
import com.example.api.dto.response.AccountResponse;
import com.example.api.entity.Account;
import com.example.api.entity.User;
import com.example.api.exception.AccountDeletionException;
import com.example.api.exception.AccountNotFoundException;
import com.example.api.repository.AccountRepository;
import com.example.api.repository.TransactionRepository;
import com.example.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {
  private static final String EMAIL = "owner@example.com";

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private AccountService accountService;

  // ========= CREATE ACCOUNT TESTS ===========

  @Test
  public void createAccount_shouldPass() {
    CreateAccountRequest createAccountRequest = new CreateAccountRequest(
        "Cole Palmer",
        new BigDecimal(1000));

    User owner = new User();
    owner.setId(7L);
    owner.setEmail(EMAIL);

    Account savedAccount = new Account();
    savedAccount.setId(1L);
    savedAccount.setUser(owner);
    savedAccount.setName("Cole Palmer");
    savedAccount.setBalance(new BigDecimal(1000));
    savedAccount.setCreatedAt(LocalDateTime.now());

    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));
    when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

    AccountResponse response = accountService.createAccount(
      createAccountRequest, EMAIL);

    assertNotNull(response);
    assertEquals(1L, response.id());
    assertEquals("Cole Palmer", response.name());
    assertEquals(new BigDecimal(1000), response.balance());
    assertNotNull(response.createdAt());
    verify(userRepository).findByEmail(EMAIL);
    verify(accountRepository).save(any(Account.class));
  }

  /*
   * @Test
   * public void createAccount_shouldThrowException_whenRepositoryFails() {
   * 
   * CreateAccountRequest request = new CreateAccountRequest(
   * "Cole Palmer",
   * new BigDecimal(1000));
   * 
   * when(accountRepository.save(any()))
   * .thenThrow(new RuntimeException("DB error"));
   * 
   * RuntimeException ex = assertThrows(RuntimeException.class, () ->
   * accountService.createAccount(request));
   * 
   * assertEquals("DB error", ex.getMessage());
   * 
   * }
   */

  // ========= GET ALL ACCOUNTS TESTS ============
  @Test
  public void getAllAccounts_shouldReturnListOfAccountResponse() {
    Account account1 = new Account();
    account1.setId(1L);
    account1.setName("A");
    account1.setBalance(new BigDecimal("1000"));
    account1.setCreatedAt(LocalDateTime.now());

    Account account2 = new Account();
    account2.setId(2L);
    account2.setName("B");
    account2.setBalance(new BigDecimal("2000"));
    account2.setCreatedAt(LocalDateTime.now());

    when(accountRepository.findAllByUserEmail(EMAIL)).thenReturn(List.of(account1, account2));

    // act
    List<AccountResponse> result = accountService.getAllAccounts(EMAIL);

    assertEquals(2, result.size());
    assertEquals("A", result.get(0).name());
    assertEquals("B", result.get(1).name());
    verify(accountRepository).findAllByUserEmail(EMAIL);
  }

  @Test
  public void getAllAccounts_shouldReturnEmptyList_whenNoAccountsExist() {
    when(accountRepository.findAllByUserEmail(EMAIL)).thenReturn(List.of());

    List<AccountResponse> result = accountService.getAllAccounts(EMAIL);

    assertTrue(result.isEmpty());
  }

  // ====== GET ACCOUNT BY ID TEST ========
  @Test
  public void getAccountById_shouldReturnAccountResponse_WhenAccountExists() {

    Account acc = new Account();
    acc.setId(1L);
    acc.setName("Cole");
    acc.setBalance(new BigDecimal("100"));
    acc.setCreatedAt(LocalDateTime.now());

    when(accountRepository.findByIdAndUserEmail(1L, EMAIL)).thenReturn(Optional.of(acc));

    AccountResponse result = accountService.getAccountById(1L, EMAIL);

    assertEquals(1L, result.id());
    assertEquals("Cole", result.name());
    assertEquals(new BigDecimal("100"), result.balance());

    verify(accountRepository).findByIdAndUserEmail(1L, EMAIL);
  }

  @Test
  public void getAccountById_shouldThrowException_whenAccountDoesNotExist() {

    when(accountRepository.findByIdAndUserEmail(1L, EMAIL)).thenReturn(Optional.empty());

    assertThrows(AccountNotFoundException.class, () -> accountService.getAccountById(1L, EMAIL));

    verify(accountRepository).findByIdAndUserEmail(1L, EMAIL);
  }

  // ====== DELETE ACCOUNT TESTS ======
  @Test
  public void deleteAccount_shouldDeleteEmptyZeroBalanceAccount() {
    Account account = new Account();
    account.setBalance(BigDecimal.ZERO);
    when(accountRepository.findByIdAndUserEmail(1L, EMAIL)).thenReturn(Optional.of(account));
    when(transactionRepository.existsByFromAccountIdOrToAccountId(1L, 1L)).thenReturn(false);

    accountService.deleteAccount(1L, EMAIL);

    verify(transactionRepository).existsByFromAccountIdOrToAccountId(1L, 1L);
    verify(accountRepository).delete(account);
  }

  @Test
  public void deleteAccount_shouldRejectNonzeroBalance() {
    Account account = new Account();
    account.setBalance(new BigDecimal("0.01"));
    when(accountRepository.findByIdAndUserEmail(1L, EMAIL)).thenReturn(Optional.of(account));

    assertThrows(AccountDeletionException.class, () -> accountService.deleteAccount(1L, EMAIL));

    verify(transactionRepository, never()).existsByFromAccountIdOrToAccountId(1L, 1L);
    verify(accountRepository, never()).delete(any(Account.class));
  }

  @Test
  public void deleteAccount_shouldRejectAccountWithTransactionHistory() {
    Account account = new Account();
    account.setBalance(BigDecimal.ZERO);
    when(accountRepository.findByIdAndUserEmail(1L, EMAIL)).thenReturn(Optional.of(account));
    when(transactionRepository.existsByFromAccountIdOrToAccountId(1L, 1L)).thenReturn(true);

    assertThrows(AccountDeletionException.class, () -> accountService.deleteAccount(1L, EMAIL));

    verify(accountRepository, never()).delete(any(Account.class));
  }

  @Test
  public void deleteAccount_shouldThrowException_whenAccountDoesNotExist() {
    when(accountRepository.findByIdAndUserEmail(1L, EMAIL)).thenReturn(Optional.empty());

    assertThrows(AccountNotFoundException.class, () -> accountService.deleteAccount(1L, EMAIL));

    verify(transactionRepository, never()).existsByFromAccountIdOrToAccountId(1L, 1L);
    verify(accountRepository, never()).delete(any(Account.class));
  }

}
