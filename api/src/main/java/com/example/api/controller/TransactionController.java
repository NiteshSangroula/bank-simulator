package com.example.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.dto.request.DepositRequest;
import com.example.api.dto.request.TransferRequest;
import com.example.api.dto.request.WithdrawRequest;
import com.example.api.dto.response.PageResponse;
import com.example.api.dto.response.TransactionResponse;
import com.example.api.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction APIs", description = "Operations related to transactions")
public class TransactionController {
  private final TransactionService transactionService;

  public TransactionController(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @Operation(summary = "Get all transactions")
  @GetMapping
  public ResponseEntity<PageResponse<TransactionResponse>> getAllTransactions(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) Long accountId) {

    PageResponse<TransactionResponse> result = transactionService.getTransactions(page, size, accountId);

    return ResponseEntity.ok(result);
  }

  @Operation(summary = "Get transaction by ID")
  @GetMapping("/{id}")
  public ResponseEntity<TransactionResponse> getTransactionById(
      @PathVariable long id) {
    return ResponseEntity.ok(transactionService.getTransactionById(id));
  }

  @Operation(summary = "Transfer amount from one account to another")
  @PostMapping("/transfer")
  public ResponseEntity<TransactionResponse> createTransferTransaction(
      @Valid @RequestBody TransferRequest request,
      Authentication authentication) {

    TransactionResponse created = transactionService.transfer(request, authentication.getName());

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(created);
  }

  @Operation(summary = "Withdraw amount from an account")
  @PostMapping("/withdraw")
  public ResponseEntity<TransactionResponse> createWithdrawTransaction(
      @Valid @RequestBody WithdrawRequest request,
      Authentication authentication) {

    TransactionResponse created = transactionService.withdraw(request, authentication.getName());

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(created);
  }

  @Operation(summary = "Deposit amount to an account")
  @PostMapping("/deposit")
  public ResponseEntity<TransactionResponse> createDepositTransaction(
      @Valid @RequestBody DepositRequest request,
      Authentication authentication) {

    TransactionResponse created = transactionService.deposit(request, authentication.getName());

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(created);
  }

}
