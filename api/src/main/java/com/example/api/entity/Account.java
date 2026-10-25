package com.example.api.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Account {
  @Id
  private long id;
  private String name;
  private BigDecimal balance;
  private LocalDateTime createdAt;

}
