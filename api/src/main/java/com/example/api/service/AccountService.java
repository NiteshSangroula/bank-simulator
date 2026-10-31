package com.example.api.service;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.api.dto.request.CreateAccountRequest;
import com.example.api.dto.response.AccountResponse;
import com.example.api.entity.Account;
import com.example.api.exception.AccountDeletionException;
import com.example.api.exception.AccountNotFoundException;
import com.example.api.repository.AccountRepository;
import com.example.api.repository.TransactionRepository;
import com.example.api.repository.UserRepository;

@Service
public class AccountService {
  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;
  private final UserRepository userRepository;

  public AccountService(
      AccountRepository accountRepository,
      TransactionRepository transactionRepository,
      UserRepository userRepository) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.userRepository = userRepository;
  }

  private AccountResponse mapToResponse(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getName(),
        account.getBalance(),
        account.getCreatedAt());
  }

  public List<AccountResponse> getAllAccounts(String email) {
    return accountRepository.findAllByUserEmail(email).stream()
        .map(this::mapToResponse)
        .toList();
  }

  public AccountResponse createAccount(CreateAccountRequest createAccountRequest, String email) {
    var owner = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));

    Account account = new Account();
    account.setUser(owner);
    account.setName(createAccountRequest.name());
    account.setBalance(createAccountRequest.initialAmount());
    account.setCreatedAt(LocalDateTime.now());
    Account saved = accountRepository.save(account);

    return mapToResponse(saved);
  }

  public AccountResponse getAccountById(long id, String email) {
    Account account = accountRepository.findByIdAndUserEmail(id, email)
        .orElseThrow(() -> new AccountNotFoundException(id));

    return mapToResponse(account);
  }

  public void deleteAccount(long id, String email) {
    Account account = accountRepository.findByIdAndUserEmail(id, email)
        .orElseThrow(() -> new AccountNotFoundException(id));

    if (account.getBalance().compareTo(BigDecimal.ZERO) != 0
        || transactionRepository.existsByFromAccountIdOrToAccountId(id, id)) {
      throw new AccountDeletionException();
    }

    accountRepository.delete(account);
  }

}
