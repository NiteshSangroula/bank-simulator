package com.example.api.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateAccountRequest(
    @NotBlank(message = "Name cannot be empty") String name,

    @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal initialAmount) {
}
