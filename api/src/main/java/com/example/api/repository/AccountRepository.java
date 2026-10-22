package com.example.api.repository;

import java.math.BigDecimal;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import com.example.api.entity.Account;

public interface AccountRepository extends CrudRepository<Account, Long> {

  @Modifying
  @Query("""
      UPDATE account
      SET balance = balance + :amount
      WHERE id = :accountId
      AND balance + :amount >= 0
      """)
  int changeBalance(long accountId, BigDecimal amount);

}
