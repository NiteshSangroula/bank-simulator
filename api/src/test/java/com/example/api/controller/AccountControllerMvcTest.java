package com.example.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.api.dto.request.CreateAccountRequest;
import com.example.api.dto.response.AccountResponse;
import com.example.api.exception.AccountNotFoundException;
import com.example.api.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AccountControllerMvcTest {
  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AccountService accountService;

  @Autowired
  private ObjectMapper objectMapper;

  // Create Account
  @Test
  void createAccount_validRequest_returns201() throws Exception {

    CreateAccountRequest request = new CreateAccountRequest(
        "Nitesh", new BigDecimal("1000"));

    AccountResponse response = new AccountResponse(
        1L, "Nitesh", new BigDecimal("1000"), LocalDateTime.now());

    when(accountService.createAccount(any(CreateAccountRequest.class)))
        .thenReturn(response);

    mockMvc.perform(post("/api/accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Nitesh"))
        .andExpect(jsonPath("$.balance").value(1000));
  }

  @Test
  void createAccount_blankName_returns400() throws Exception {
    CreateAccountRequest request = new CreateAccountRequest(
        "", new BigDecimal("1000"));

    mockMvc.perform(post("/api/accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("NotBlank"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(accountService, never()).createAccount(any());
  }

  @Test
  void createAccount_negativeBalance_returns400() throws Exception {
    CreateAccountRequest request = new CreateAccountRequest(
        "Nitesh", new BigDecimal("-1000"));

    mockMvc.perform(post("/api/accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("initialAmount"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("Positive"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(accountService, never()).createAccount(any());
  }

  @Test
  void createAccount_nullBalance_returns400() throws Exception {
    CreateAccountRequest request = new CreateAccountRequest(
        "Nitesh", null);

    mockMvc.perform(post("/api/accounts")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("initialAmount"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("NotNull"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(accountService, never()).createAccount(any());
  }

  // get all accounts
  @Test
  void getAllAccounts_returns200AndList() throws Exception {
    List<AccountResponse> accounts = List.of(
        new AccountResponse(
            1L, "Nitesh", new BigDecimal("100"), LocalDateTime.now()));

    when(accountService.getAllAccounts())
        .thenReturn(accounts);

    mockMvc.perform(get("/api/accounts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].name").value("Nitesh"))
        .andExpect(jsonPath("$[0].balance").value(100));

    verify(accountService).getAllAccounts();
  }

  // get account by id
  @Test
  void getAccountById_validId_returns200() throws Exception {
    AccountResponse response = new AccountResponse(
        1L, "Nitesh", new BigDecimal("100"), LocalDateTime.now());

    when(accountService.getAccountById(anyLong()))
        .thenReturn(response);

    mockMvc.perform(get("/api/accounts/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Nitesh"))
        .andExpect(jsonPath("$.balance").value(100));

    verify(accountService).getAccountById(1L);
  }

  @Test
  void getAccountById_notFound_returns404() throws Exception {

    when(accountService.getAccountById(1L))
        .thenThrow(new AccountNotFoundException(1L));

    mockMvc.perform(get("/api/accounts/1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Account not found with id: 1"))
        .andExpect(jsonPath("$.fieldErrors").doesNotExist());

    verify(accountService).getAccountById(1L);
  }

  // delete account
  @Test
  void deleteAccount_validId_returns204() throws Exception {

    mockMvc.perform(delete("/api/accounts/1"))
        .andExpect(status().isNoContent());

    verify(accountService).deleteAccount(1L);
  }

  @Test
  void deleteAccount_notFound_returns404() throws Exception {
    doThrow(new AccountNotFoundException(1L))
        .when(accountService).deleteAccount(1L);

    mockMvc.perform(delete("/api/accounts/1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Account not found with id: 1"))
        .andExpect(jsonPath("$.fieldErrors").doesNotExist());

    verify(accountService).deleteAccount(1L);
  }

}
