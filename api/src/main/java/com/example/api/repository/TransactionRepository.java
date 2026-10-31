package com.example.api.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.api.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  boolean existsByFromAccountIdOrToAccountId(Long fromAccountId, Long toAccountId);

    @Query("""
      SELECT transaction FROM Transaction transaction
      WHERE EXISTS (
        SELECT account.id FROM Account account
        WHERE account.id = transaction.fromAccountId
        AND account.user.email = :email
      )
      OR EXISTS (
        SELECT account.id FROM Account account
        WHERE account.id = transaction.toAccountId
        AND account.user.email = :email
      )
      ORDER BY transaction.createdAt DESC
      """)
    Page<Transaction> findAllForOwner(@Param("email") String email, Pageable pageable);

  @Query("""
      SELECT transaction FROM Transaction transaction
      WHERE (transaction.fromAccountId = :accountId
      OR transaction.toAccountId = :accountId)
      AND EXISTS (
        SELECT account.id FROM Account account
        WHERE account.id = :accountId
        AND account.user.email = :email
      )
      ORDER BY transaction.createdAt DESC
      """)
    Page<Transaction> findByAccountIdForOwner(
      @Param("accountId") Long accountId,
      @Param("email") String email,
      Pageable pageable);

    @Query("""
      SELECT transaction FROM Transaction transaction
      WHERE transaction.id = :transactionId
      AND (
        EXISTS (
          SELECT account.id FROM Account account
          WHERE account.id = transaction.fromAccountId
          AND account.user.email = :email
        )
        OR EXISTS (
          SELECT account.id FROM Account account
          WHERE account.id = transaction.toAccountId
          AND account.user.email = :email
        )
      )
      """)
    Optional<Transaction> findByIdForOwner(
      @Param("transactionId") long transactionId,
      @Param("email") String email);

}
