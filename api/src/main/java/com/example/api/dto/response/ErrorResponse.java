package com.example.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.example.api.dto.other.FieldValidationError;

public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    List<FieldValidationError> fieldErrors) {
}
