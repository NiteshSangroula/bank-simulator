package com.example.api.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WithdrawRequest(
    @NotNull(message = "Account id is required") Long fromAccountId,

    @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount,

    @NotBlank(message = "Description cannot be empty") String description) {
}
