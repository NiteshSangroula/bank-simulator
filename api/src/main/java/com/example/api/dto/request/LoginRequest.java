package com.example.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Email cannot be empty") @Email(message = "Email must be valid") @Size(max = 254) String email,
    @NotBlank(message = "Password cannot be empty") @Size(max = 128) String password) {
}