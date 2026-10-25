package com.example.api.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Getter;
import lombok.Setter;

@Table("TRANSACTIONS")
@Getter
@Setter
public class Transaction {
  @Id
  private long id;
  private BigDecimal amount;

  private TransactionType type;

  private Long fromAccountId;
  private Long toAccountId;

  private LocalDateTime createdAt;
  private String description;

}
