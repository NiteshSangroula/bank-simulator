package com.example.api.dto.other;

public record FieldValidationError(
    String field,
    String code,
    String message) {
}
