package com.example.api.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.example.api.entity.Transaction;
import com.example.api.entity.TransactionType;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

  @Autowired
  private TransactionRepository transactionRepository;

  @Test
  void savesTransactionWithGeneratedIdAndStringEnum() {
    Transaction transaction = transactionRepository.saveAndFlush(
        newTransaction(TransactionType.DEPOSIT, null, 10L, 3));

    assertNotNull(transaction.getId());
    assertEquals(TransactionType.DEPOSIT,
        transactionRepository.findById(transaction.getId()).orElseThrow().getType());
  }

  @Test
  void pagesTransactionsNewestFirst() {
    transactionRepository.saveAndFlush(newTransaction(TransactionType.DEPOSIT, null, 10L, 1));
    transactionRepository.saveAndFlush(newTransaction(TransactionType.WITHDRAW, 10L, null, 2));
    transactionRepository.saveAndFlush(newTransaction(TransactionType.TRANSFER, 10L, 20L, 3));

    Page<Transaction> page = transactionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 2));

    assertEquals(3, page.getTotalElements());
    assertEquals(2, page.getContent().size());
    assertEquals(TransactionType.TRANSFER, page.getContent().get(0).getType());
    assertEquals(TransactionType.WITHDRAW, page.getContent().get(1).getType());
  }

  @Test
  void pagesTransactionsForEitherAccountDirection() {
    transactionRepository.saveAndFlush(newTransaction(TransactionType.DEPOSIT, null, 10L, 1));
    transactionRepository.saveAndFlush(newTransaction(TransactionType.WITHDRAW, 10L, null, 2));
    transactionRepository.saveAndFlush(newTransaction(TransactionType.TRANSFER, 20L, 30L, 3));

    Page<Transaction> page = transactionRepository.findByAccountIdPaged(10L, PageRequest.of(0, 10));

    assertEquals(2, page.getTotalElements());
    assertEquals(2, page.getContent().size());
    assertEquals(TransactionType.WITHDRAW, page.getContent().get(0).getType());
    assertEquals(TransactionType.DEPOSIT, page.getContent().get(1).getType());
  }

  private Transaction newTransaction(
      TransactionType type,
      Long fromAccountId,
      Long toAccountId,
      int day) {
    Transaction transaction = new Transaction();
    transaction.setAmount(new BigDecimal("25.00"));
    transaction.setType(type);
    transaction.setFromAccountId(fromAccountId);
    transaction.setToAccountId(toAccountId);
    transaction.setCreatedAt(LocalDateTime.of(2024, 1, day, 12, 0));
    transaction.setDescription(type.name());
    return transaction;
  }
}