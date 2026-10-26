package com.example.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Display name cannot be empty") @Size(max = 100) String displayName,
    @NotBlank(message = "Email cannot be empty") @Email(message = "Email must be valid") @Size(max = 254) String email,
    @NotBlank(message = "Password cannot be empty") @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters") String password) {
}