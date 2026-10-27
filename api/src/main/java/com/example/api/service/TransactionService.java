package com.example.api.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.api.dto.request.DepositRequest;
import com.example.api.dto.request.TransferRequest;
import com.example.api.dto.request.WithdrawRequest;
import com.example.api.dto.response.PageResponse;
import com.example.api.dto.response.TransactionResponse;
import com.example.api.entity.Transaction;
import com.example.api.entity.TransactionType;
import com.example.api.exception.AccountNotFoundException;
import com.example.api.exception.InsufficientBalanceException;
import com.example.api.exception.TransactionNotFoundException;
import com.example.api.repository.AccountRepository;
import com.example.api.repository.TransactionRepository;

@Service
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final AccountRepository accountRepository;

  public TransactionService(
      TransactionRepository transactionRepository,
      AccountRepository accountRepository) {
    this.transactionRepository = transactionRepository;
    this.accountRepository = accountRepository;
  }

  private TransactionResponse mapToTransactionResponse(Transaction transaction) {
    return new TransactionResponse(
        transaction.getId(),
        transaction.getAmount(),
        transaction.getType(),
        transaction.getFromAccountId(),
        transaction.getToAccountId(),
        transaction.getCreatedAt(),
        transaction.getDescription());
  }

  private Transaction createAndSaveTransaction(
      TransactionType type,
      BigDecimal amount,
      Long fromAccountId,
      Long toAccountId,
      String description) {

    Transaction transaction = new Transaction();
    transaction.setAmount(amount);
    transaction.setType(type);
    transaction.setFromAccountId(fromAccountId);
    transaction.setToAccountId(toAccountId);
    transaction.setDescription(description);
    transaction.setCreatedAt(LocalDateTime.now());

    return transactionRepository.save(transaction);

  }

  // 1. TRANSFER
  @Transactional
  public TransactionResponse transfer(TransferRequest request, String email) {

    BigDecimal amount = request.amount();

    if (!accountRepository.existsByIdAndUserEmail(request.fromAccountId(), email)) {
      throw new AccountNotFoundException(request.fromAccountId());
    }

    if (!accountRepository.existsById(request.toAccountId())) {
      throw new AccountNotFoundException(request.toAccountId());
    }

    // 1. Debit sender first
    int updated = accountRepository.changeBalanceForOwner(
        request.fromAccountId(),
        email,
        amount.negate());

    if (updated <= 0) {
      if (!accountRepository.existsByIdAndUserEmail(request.fromAccountId(), email)) {
        throw new AccountNotFoundException(request.fromAccountId());
      }
      throw new InsufficientBalanceException(request.fromAccountId());
    }

    // 2. Credit receiver
    int credited = accountRepository.changeBalance(
        request.toAccountId(),
        amount);

    if (credited <= 0) {
      throw new AccountNotFoundException(request.toAccountId());
    }

    // 3. Record transaction
    Transaction saved = createAndSaveTransaction(
        TransactionType.TRANSFER,
        amount,
        request.fromAccountId(),
        request.toAccountId(),
        request.description());

    return mapToTransactionResponse(saved);
  }

  // 2. WITHDRAW
  @Transactional
  public TransactionResponse withdraw(WithdrawRequest request, String email) {
    BigDecimal amount = request.amount();

    int updated = accountRepository.changeBalanceForOwner(
        request.fromAccountId(),
        email,
        amount.negate());

    if (updated <= 0) {
      if (!accountRepository.existsByIdAndUserEmail(request.fromAccountId(), email)) {
        throw new AccountNotFoundException(request.fromAccountId());
      }
      throw new InsufficientBalanceException(request.fromAccountId());
    }

    Transaction saved = createAndSaveTransaction(
        TransactionType.WITHDRAW,
        amount,
        request.fromAccountId(),
        null,
        request.description());

    return mapToTransactionResponse(saved);
  }

  // 3. DEPOSIT
  @Transactional
  public TransactionResponse deposit(DepositRequest request, String email) {
    BigDecimal amount = request.amount();

    int updated = accountRepository.changeBalanceForOwner(
        request.toAccountId(),
        email,
        amount);

    if (updated <= 0) {
      throw new AccountNotFoundException(request.toAccountId());
    }

    Transaction saved = createAndSaveTransaction(
        TransactionType.DEPOSIT,
        amount,
        null,
        request.toAccountId(),
        request.description());

    return mapToTransactionResponse(saved);
  }

  // Get All Transaction
  public PageResponse<TransactionResponse> getTransactions(
      int page,
      int size,
      Long accountId) {
    Page<Transaction> result;

    if (accountId != null) {
      result = transactionRepository.findByAccountIdPaged(accountId, PageRequest.of(page, size));
    } else {
      result = transactionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    List<TransactionResponse> content = result.getContent().stream()
        .map(this::mapToTransactionResponse)
        .toList();

    return new PageResponse<>(content, page, size, result.getTotalElements(), result.getTotalPages());
  }

  // Get Transaction by Id
  public TransactionResponse getTransactionById(long id) {
    Transaction transaction = transactionRepository.findById(id)
        .orElseThrow(() -> new TransactionNotFoundException(id));

    return mapToTransactionResponse(transaction);
  }

}
