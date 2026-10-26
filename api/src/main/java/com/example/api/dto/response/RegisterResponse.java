package com.example.api.dto.response;

import java.time.LocalDateTime;

public record RegisterResponse(
    Long id,
    String displayName,
    String email,
    LocalDateTime createdAt) {
}