package com.example.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
    long id,
    String name,
    BigDecimal balance,
    LocalDateTime createdAt) {
}
