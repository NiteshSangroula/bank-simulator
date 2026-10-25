package com.example.api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.api.dto.request.CreateAccountRequest;
import com.example.api.dto.response.AccountResponse;
import com.example.api.entity.Account;
import com.example.api.exception.AccountNotFoundException;
import com.example.api.repository.AccountRepository;

@Service
public class AccountService {
  private final AccountRepository accountRepository;

  public AccountService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  private AccountResponse mapToResponse(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getName(),
        account.getBalance(),
        account.getCreatedAt());
  }

  public List<AccountResponse> getAllAccounts() {
    return accountRepository.findAll().stream()
        .map(this::mapToResponse)
        .toList();
  }

  public AccountResponse createAccount(CreateAccountRequest createAccountRequest) {
    Account account = new Account();
    account.setName(createAccountRequest.name());
    account.setBalance(createAccountRequest.initialAmount());
    account.setCreatedAt(LocalDateTime.now());
    Account saved = accountRepository.save(account);

    return mapToResponse(saved);
  }

  public AccountResponse getAccountById(long id) {
    Account account = accountRepository.findById(id)
        .orElseThrow(() -> new AccountNotFoundException(id));

    return mapToResponse(account);
  }

  public void deleteAccount(long id) {
    if (!accountRepository.existsById(id)) {
      throw new AccountNotFoundException(id);
    }
    accountRepository.deleteById(id);
  }

}
