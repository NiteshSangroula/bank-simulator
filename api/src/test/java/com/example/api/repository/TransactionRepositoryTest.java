package com.example.api.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.example.api.entity.Account;
import com.example.api.entity.Transaction;
import com.example.api.entity.TransactionType;
import com.example.api.entity.User;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

  @Autowired
  private TransactionRepository transactionRepository;

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private UserRepository userRepository;

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
    User owner = saveUser("owner@example.com");
    User other = saveUser("other@example.com");
    Account ownedAccount = saveAccount(owner, "Owner account");
    Account otherAccount = saveAccount(other, "Other account");
    transactionRepository.saveAndFlush(
      newTransaction(TransactionType.DEPOSIT, null, ownedAccount.getId(), 1));
    transactionRepository.saveAndFlush(
      newTransaction(TransactionType.WITHDRAW, ownedAccount.getId(), null, 2));
    transactionRepository.saveAndFlush(
      newTransaction(TransactionType.TRANSFER, ownedAccount.getId(), otherAccount.getId(), 3));
    transactionRepository.saveAndFlush(
      newTransaction(TransactionType.DEPOSIT, null, otherAccount.getId(), 4));

    Page<Transaction> page = transactionRepository.findAllForOwner(owner.getEmail(), PageRequest.of(0, 2));
    Page<Transaction> secondPage = transactionRepository.findAllForOwner(owner.getEmail(), PageRequest.of(1, 2));

    assertEquals(3, page.getTotalElements());
    assertEquals(2, page.getTotalPages());
    assertEquals(2, page.getContent().size());
    assertEquals(TransactionType.TRANSFER, page.getContent().get(0).getType());
    assertEquals(TransactionType.WITHDRAW, page.getContent().get(1).getType());
    assertEquals(1, secondPage.getContent().size());
    assertEquals(TransactionType.DEPOSIT, secondPage.getContent().get(0).getType());
  }

  @Test
  void pagesTransactionsForEitherAccountDirection() {
    User owner = saveUser("owner@example.com");
    User other = saveUser("other@example.com");
    Account ownedAccount = saveAccount(owner, "Owner account");
    Account otherAccount = saveAccount(other, "Other account");
    transactionRepository.saveAndFlush(
      newTransaction(TransactionType.DEPOSIT, null, ownedAccount.getId(), 1));
    transactionRepository.saveAndFlush(
      newTransaction(TransactionType.WITHDRAW, ownedAccount.getId(), null, 2));
    transactionRepository.saveAndFlush(
      newTransaction(TransactionType.TRANSFER, otherAccount.getId(), ownedAccount.getId(), 3));
    transactionRepository.saveAndFlush(
      newTransaction(TransactionType.DEPOSIT, null, otherAccount.getId(), 4));

    Page<Transaction> page = transactionRepository.findByAccountIdForOwner(
      ownedAccount.getId(), owner.getEmail(), PageRequest.of(0, 10));

    assertEquals(3, page.getTotalElements());
    assertEquals(3, page.getContent().size());
    assertEquals(TransactionType.TRANSFER, page.getContent().get(0).getType());
    assertEquals(TransactionType.WITHDRAW, page.getContent().get(1).getType());
    assertEquals(TransactionType.DEPOSIT, page.getContent().get(2).getType());
    assertTrue(transactionRepository.findByAccountIdForOwner(
      otherAccount.getId(), owner.getEmail(), PageRequest.of(0, 10)).isEmpty());
    }

    @Test
    void findsTransactionsByIdForEitherOwnerOfCrossUserTransfer() {
    User sender = saveUser("sender@example.com");
    User recipient = saveUser("recipient@example.com");
    Account senderAccount = saveAccount(sender, "Sender account");
    Account recipientAccount = saveAccount(recipient, "Recipient account");
    Transaction transfer = transactionRepository.saveAndFlush(
      newTransaction(TransactionType.TRANSFER, senderAccount.getId(), recipientAccount.getId(), 1));

    assertEquals(transfer.getId(), transactionRepository.findByIdForOwner(
      transfer.getId(), sender.getEmail()).orElseThrow().getId());
    assertEquals(transfer.getId(), transactionRepository.findByIdForOwner(
      transfer.getId(), recipient.getEmail()).orElseThrow().getId());
    assertTrue(transactionRepository.findByIdForOwner(
      transfer.getId(), "unrelated@example.com").isEmpty());
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

  private User saveUser(String email) {
    User user = new User();
    user.setDisplayName(email);
    user.setEmail(email);
    user.setPasswordHash("not-used");
    return userRepository.saveAndFlush(user);
  }

  private Account saveAccount(User owner, String name) {
    Account account = new Account();
    account.setUser(owner);
    account.setName(name);
    account.setBalance(new BigDecimal("100.00"));
    account.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
    return accountRepository.saveAndFlush(account);
  }
}