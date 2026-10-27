package com.example.api.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.api.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

  List<Account> findAllByUserEmail(String email);

  Optional<Account> findByIdAndUserEmail(long id, String email);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      UPDATE Account account
      SET account.balance = account.balance + :amount
      WHERE account.id = :accountId
      AND account.balance + :amount >= 0
      """)
  int changeBalance(@Param("accountId") long accountId, @Param("amount") BigDecimal amount);

}
