package com.example.api.exception;

public class AccountDeletionException extends RuntimeException {
  public AccountDeletionException() {
    super("Account cannot be deleted while it has a nonzero balance or transaction history");
  }
}