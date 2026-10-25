package com.example.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.api.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Query("""
      SELECT transaction FROM Transaction transaction
      WHERE transaction.fromAccountId = :accountId
      OR transaction.toAccountId = :accountId
      ORDER BY transaction.createdAt DESC
      """)
  Page<Transaction> findByAccountIdPaged(@Param("accountId") Long accountId, Pageable pageable);

}
