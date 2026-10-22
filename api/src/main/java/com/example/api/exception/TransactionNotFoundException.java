package com.example.api.exception;

public class TransactionNotFoundException extends RuntimeException {
  public TransactionNotFoundException(long id) {
    super("Transaction not found with id: " + id);
  }
}
