package com.example.api.repository;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import com.example.api.entity.Transaction;

public interface TransactionRepository extends CrudRepository<Transaction, Long> {
  @Query("""
      SELECT * FROM transactions
      ORDER BY created_at DESC
      LIMIT :size OFFSET :offset
      """)
  List<Transaction> findAllPaged(int size, int offset);

  @Query("""
      SELECT * FROM transactions
      WHERE from_account_id = :accountId
      OR to_account_id = :accountId
      ORDER BY created_at DESC
      LIMIT :size OFFSET :offset
      """)
  List<Transaction> findByAccountIdPaged(Long accountId, int size, int offset);

  @Query("""
          SELECT COUNT(*) FROM transactions
      """)
  Long countAll();

  @Query("""
          SELECT COUNT(*) FROM transactions
          WHERE from_account_id = :accountId
             OR to_account_id = :accountId
      """)
  Long countByAccountId(Long accountId);

}
