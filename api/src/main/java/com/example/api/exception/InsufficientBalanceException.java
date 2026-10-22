package com.example.api.exception;

public class InsufficientBalanceException extends RuntimeException {
  public InsufficientBalanceException(long id) {
    super("Insufficient Balance in account with id: " + id);
  }
}
