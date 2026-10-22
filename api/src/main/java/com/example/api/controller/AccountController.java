package com.example.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.dto.request.CreateAccountRequest;
import com.example.api.dto.response.AccountResponse;
import com.example.api.service.AccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Account APIs", description = "Operations related to bank accounts")
public class AccountController {
  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @Operation(summary = "Get all accounts")
  @GetMapping
  public ResponseEntity<List<AccountResponse>> getAllAccount() {

    return ResponseEntity.ok(accountService.getAllAccounts());
  }

  @Operation(summary = "Get account by ID")
  @GetMapping("/{id}")
  public ResponseEntity<AccountResponse> getAccountById(
      @PathVariable long id) {

    return ResponseEntity.ok(accountService.getAccountById(id));
  }

  @Operation(summary = "Create new account")
  @PostMapping
  public ResponseEntity<AccountResponse> createAccount(
      @Valid @RequestBody CreateAccountRequest createAccountRequest) {

    AccountResponse created = accountService.createAccount(createAccountRequest);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(created);
  }

  @Operation(summary = "Delete account by ID")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteAccount(@PathVariable long id) {

    accountService.deleteAccount(id);
    return ResponseEntity.noContent().build();
  }

}
