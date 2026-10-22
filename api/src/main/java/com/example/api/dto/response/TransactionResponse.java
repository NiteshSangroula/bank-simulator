package com.example.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.api.entity.TransactionType;

public record TransactionResponse(
    long id,
    BigDecimal amount,
    TransactionType type,
    Long fromAccountId,
    Long toAccountId,
    LocalDateTime createdAt,
    String description) {
}
