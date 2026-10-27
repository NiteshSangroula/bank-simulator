package com.example.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.api.dto.request.DepositRequest;
import com.example.api.dto.request.TransferRequest;
import com.example.api.dto.request.WithdrawRequest;
import com.example.api.dto.response.TransactionResponse;
import com.example.api.entity.Transaction;
import com.example.api.entity.TransactionType;
import com.example.api.exception.AccountNotFoundException;
import com.example.api.exception.InsufficientBalanceException;
import com.example.api.repository.AccountRepository;
import com.example.api.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {
    private static final String EMAIL = "owner@example.com";


  @Mock
  private AccountRepository accountRepository;

  @Mock
  private TransactionRepository transactionRepository;

  @InjectMocks
  private TransactionService transactionService;

  @Test
  public void transfer_success() {

    // Arrange
    long fromId = 1L;
    long toId = 2L;
    BigDecimal amount = new BigDecimal("100");

    TransferRequest request = new TransferRequest(
        fromId,
        toId,
        amount,
        "test");

    when(accountRepository.existsById(fromId))
        .thenReturn(true);

    when(accountRepository.existsById(toId))
        .thenReturn(true);

    when(accountRepository.changeBalance(fromId, amount.negate()))
        .thenReturn(1);

    when(accountRepository.changeBalance(toId, amount))
        .thenReturn(1);

    Transaction saved = new Transaction();
    saved.setId(10L);
    saved.setAmount(amount);
    saved.setType(TransactionType.TRANSFER);
    saved.setFromAccountId(fromId);
    saved.setToAccountId(toId);
    saved.setDescription("test");
    saved.setCreatedAt(LocalDateTime.now());

    when(transactionRepository.save(any(Transaction.class)))
        .thenReturn(saved);

    // Act
    TransactionResponse response = transactionService.transfer(request);

    // Assert
    assertEquals(10L, response.id());
    assertEquals(amount, response.amount());
    assertEquals(fromId, response.fromAccountId());
    assertEquals(toId, response.toAccountId());
    assertEquals(TransactionType.TRANSFER, response.type());

    verify(accountRepository).changeBalance(fromId, amount.negate());
    verify(accountRepository).changeBalance(toId, amount);
    verify(transactionRepository).save(any(Transaction.class));

    ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

    verify(transactionRepository).save(captor.capture());

    Transaction actual = captor.getValue();

    assertEquals(TransactionType.TRANSFER, actual.getType());

  }

  @Test
  public void withdraw_success() {
    long fromId = 1L;
    BigDecimal amount = new BigDecimal("100");

    WithdrawRequest request = new WithdrawRequest(
        fromId,
        amount,
        "test withdraw");

    when(accountRepository.changeBalanceForOwner(fromId, EMAIL, amount.negate()))
        .thenReturn(1);

    Transaction saved = new Transaction();
    saved.setId(10L);
    saved.setAmount(amount);
    saved.setType(TransactionType.WITHDRAW);
    saved.setFromAccountId(fromId);
    saved.setToAccountId(null);
    saved.setDescription("test withdraw");
    saved.setCreatedAt(LocalDateTime.now());

    when(transactionRepository.save(any(Transaction.class)))
        .thenReturn(saved);

    TransactionResponse response = transactionService.withdraw(request, EMAIL);

    // Assert
    assertEquals(10L, response.id());
    assertEquals(amount, response.amount());
    assertEquals(fromId, response.fromAccountId());
    assertEquals(TransactionType.WITHDRAW, response.type());

    verify(accountRepository).changeBalanceForOwner(fromId, EMAIL, amount.negate());
    verify(transactionRepository).save(any(Transaction.class));

  }

  @Test
  public void deposit_success() {

    long toId = 1L;
    BigDecimal amount = new BigDecimal("100");

    DepositRequest request = new DepositRequest(
        toId,
        amount,
        "test deposit");

    when(accountRepository.changeBalanceForOwner(toId, EMAIL, amount))
        .thenReturn(1);

    Transaction saved = new Transaction();
    saved.setId(10L);
    saved.setAmount(amount);
    saved.setType(TransactionType.DEPOSIT);
    saved.setFromAccountId(null);
    saved.setToAccountId(toId);
    saved.setDescription("test deposit");
    saved.setCreatedAt(LocalDateTime.now());

    when(transactionRepository.save(any(Transaction.class)))
        .thenReturn(saved);

    TransactionResponse response = transactionService.deposit(request, EMAIL);

    // Assert
    assertEquals(10L, response.id());
    assertEquals(amount, response.amount());
    assertEquals(toId, response.toAccountId());
    assertEquals(TransactionType.DEPOSIT, response.type());

    verify(accountRepository).changeBalanceForOwner(toId, EMAIL, amount);
    verify(transactionRepository).save(any(Transaction.class));
  }

  // INSUFFICIENT BALANCE

  @Test
  public void withdraw_insufficientBalance() {
    long fromId = 1L;
    BigDecimal amount = new BigDecimal("100");

    WithdrawRequest request = new WithdrawRequest(
        fromId,
        amount,
        "test withdraw");

    when(accountRepository.existsByIdAndUserEmail(fromId, EMAIL))
        .thenReturn(true);

    when(accountRepository.changeBalanceForOwner(fromId, EMAIL, amount.negate()))
        .thenReturn(0);

    assertThrows(InsufficientBalanceException.class,
        () -> transactionService.withdraw(request, EMAIL));

    verify(transactionRepository, never()).save(any());
  }

  @Test
  public void transfer_insufficientBalance() {
    long fromId = 1L;
    long toId = 2L;
    BigDecimal amount = new BigDecimal("100");

    TransferRequest request = new TransferRequest(
        fromId,
        toId,
        amount,
        "test");

    when(accountRepository.existsById(fromId))
        .thenReturn(true);

    when(accountRepository.existsById(toId))
        .thenReturn(true);

    when(accountRepository.changeBalance(fromId, amount.negate()))
        .thenReturn(0);

    assertThrows(InsufficientBalanceException.class,
        () -> transactionService.transfer(request));
    verify(transactionRepository, never()).save(any());
  }

  // ACCOUNT NOT FOUND

  @Test
  public void deposit_accountNotFound() {
    long toId = 1L;
    DepositRequest request = new DepositRequest(
        toId, BigDecimal.TEN, "test");

    assertThrows(AccountNotFoundException.class,
        () -> transactionService.deposit(request, EMAIL));

    verify(accountRepository).changeBalanceForOwner(toId, EMAIL, BigDecimal.TEN);
    verify(transactionRepository, never()).save(any());
  }

  @Test
  public void withdraw_accountNotFound() {
    long fromId = 1L;
    WithdrawRequest request = new WithdrawRequest(
        fromId, BigDecimal.TEN, "test");

    when(accountRepository.existsByIdAndUserEmail(fromId, EMAIL))
        .thenReturn(false);

    assertThrows(AccountNotFoundException.class,
        () -> transactionService.withdraw(request, EMAIL));

    verify(accountRepository).changeBalanceForOwner(fromId, EMAIL, BigDecimal.TEN.negate());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  public void transfer_toAccountNotFound() {
    long fromId = 1L;
    long toId = 2L;
    TransferRequest request = new TransferRequest(
        fromId, toId, BigDecimal.TEN, "test");

    when(accountRepository.existsById(fromId))
        .thenReturn(true);

    when(accountRepository.existsById(toId))
        .thenReturn(false);

    assertThrows(AccountNotFoundException.class,
        () -> transactionService.transfer(request));

    verify(accountRepository, never()).changeBalance(anyLong(), any());
    verify(transactionRepository, never()).save(any());
  }

  @Test
  public void transfer_fromAccountNotFound() {
    long fromId = 1L;
    long toId = 2L;
    TransferRequest request = new TransferRequest(
        fromId, toId, BigDecimal.TEN, "test");

    when(accountRepository.existsById(fromId))
        .thenReturn(false);

    assertThrows(AccountNotFoundException.class,
        () -> transactionService.transfer(request));

    verify(accountRepository, never()).changeBalance(anyLong(), any());
    verify(transactionRepository, never()).save(any());
  }

}
