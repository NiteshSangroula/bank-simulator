package com.example.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.api.dto.request.DepositRequest;
import com.example.api.dto.request.TransferRequest;
import com.example.api.dto.request.WithdrawRequest;
import com.example.api.dto.response.PageResponse;
import com.example.api.dto.response.TransactionResponse;
import com.example.api.entity.TransactionType;
import com.example.api.exception.TransactionNotFoundException;
import com.example.api.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
public class TransactionControllerMvcTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private TransactionService service;

  // -----------------------TRANSFER
  @Test
  void createTransferTransaction_validRequest_returns201() throws Exception {
    TransferRequest request = new TransferRequest(
        1L, 2L, new BigDecimal("100"), "test");

    TransactionResponse response = new TransactionResponse(
        1L, new BigDecimal("100"), TransactionType.TRANSFER, 1L, 2L, LocalDateTime.now(), "test");

    when(service.transfer(any(TransferRequest.class)))
        .thenReturn(response);

    mockMvc.perform(post("/api/transactions/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.amount").value(100))
        .andExpect(jsonPath("$.type").value("TRANSFER"))
        .andExpect(jsonPath("$.fromAccountId").value(1L))
        .andExpect(jsonPath("$.toAccountId").value(2L))
        .andExpect(jsonPath("$.description").value("test"));
  }

  @Test
  void createTransferTransaction_nullFromAccountId_returns400() throws Exception {
    TransferRequest request = new TransferRequest(
        null, 2L, new BigDecimal("100"), "test");

    mockMvc.perform(post("/api/transactions/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("fromAccountId"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("NotNull"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(service, never()).transfer(any());
  }

  @Test
  void createTransferTransaction_nullToAccountId_returns400() throws Exception {
    TransferRequest request = new TransferRequest(
        1L, null, new BigDecimal("100"), "test");

    mockMvc.perform(post("/api/transactions/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("toAccountId"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("NotNull"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(service, never()).transfer(any());
  }

  @Test
  void createTransferTransaction_nullBalance_returns400() throws Exception {
    TransferRequest request = new TransferRequest(
        1L, 2L, null, "test");

    mockMvc.perform(post("/api/transactions/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("NotNull"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(service, never()).transfer(any());
  }

  @Test
  void createTransferTransaction_negativeBalance_returns400() throws Exception {
    TransferRequest request = new TransferRequest(
        1L, 2L, new BigDecimal("-100"), "test");

    mockMvc.perform(post("/api/transactions/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("Positive"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(service, never()).transfer(any());
  }

  @Test
  void createTransferTransaction_blankDescription_returns400() throws Exception {
    TransferRequest request = new TransferRequest(
        1L, 2L, new BigDecimal("100"), "");

    mockMvc.perform(post("/api/transactions/transfer")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("description"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("NotBlank"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(service, never()).transfer(any());
  }

  // -----------------------WITHDRAW

  @Test
  void createWithdrawTransaction_validRequest_returns201() throws Exception {
    WithdrawRequest request = new WithdrawRequest(
        1L, new BigDecimal("100"), "test");

    TransactionResponse response = new TransactionResponse(
        1L, new BigDecimal("100"), TransactionType.WITHDRAW,
        1L, null, LocalDateTime.now(), "test");

    when(service.withdraw(any(WithdrawRequest.class)))
        .thenReturn(response);

    mockMvc.perform(post("/api/transactions/withdraw")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.amount").value(100))
        .andExpect(jsonPath("$.type").value("WITHDRAW"))
        .andExpect(jsonPath("$.fromAccountId").value(1L))
        .andExpect(jsonPath("$.toAccountId").value(Matchers.nullValue()))
        .andExpect(jsonPath("$.description").value("test"));
  }

  @Test
  void createWithdrawTransaction_negativeBalance_returns400() throws Exception {
    WithdrawRequest request = new WithdrawRequest(
        1L, new BigDecimal("-100"), "test");

    mockMvc.perform(post("/api/transactions/withdraw")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("Positive"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(service, never()).withdraw(any());
  }

  @Test
  void createWithdrawTransaction_nullBalance_returns400() throws Exception {
    WithdrawRequest request = new WithdrawRequest(
        1L, null, "test");

    mockMvc.perform(post("/api/transactions/withdraw")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("NotNull"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(service, never()).withdraw(any());
  }

  @Test
  void createWithdrawTransaction_nullFromAccountId_returns400() throws Exception {
    WithdrawRequest request = new WithdrawRequest(
        null, new BigDecimal("100"), "test");

    mockMvc.perform(post("/api/transactions/withdraw")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("fromAccountId"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("NotNull"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(service, never()).withdraw(any());
  }

  @Test
  void createWithdrawTransaction_blankDescription_returns400() throws Exception {
    WithdrawRequest request = new WithdrawRequest(
        1L, new BigDecimal("100"), "");

    mockMvc.perform(post("/api/transactions/withdraw")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("description"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("NotBlank"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(service, never()).withdraw(any());
  }

  // -----------------------DEPOSIT

  @Test
  void createDepositTransaction_validRequest_returns201() throws Exception {
    DepositRequest request = new DepositRequest(
        1L, new BigDecimal("100"), "test");

    TransactionResponse response = new TransactionResponse(
        1L, new BigDecimal("100"), TransactionType.DEPOSIT,
        null, 1L, LocalDateTime.now(), "test");

    when(service.deposit(any(DepositRequest.class)))
        .thenReturn(response);

    mockMvc.perform(post("/api/transactions/deposit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1L))
        .andExpect(jsonPath("$.amount").value(100))
        .andExpect(jsonPath("$.type").value("DEPOSIT"))
        .andExpect(jsonPath("$.fromAccountId").value(Matchers.nullValue()))
        .andExpect(jsonPath("$.toAccountId").value(1L))
        .andExpect(jsonPath("$.description").value("test"));
  }

  @Test
  void createDepositTransaction_negativeBalance_returns400() throws Exception {
    DepositRequest request = new DepositRequest(
        1L, new BigDecimal("-100"), "test");

    mockMvc.perform(post("/api/transactions/deposit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("Positive"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(service, never()).deposit(any());
  }

  @Test
  void createDepositTransaction_nullBalance_returns400() throws Exception {
    DepositRequest request = new DepositRequest(
        1L, null, "test");

    mockMvc.perform(post("/api/transactions/deposit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("NotNull"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(service, never()).deposit(any());
  }

  @Test
  void createDepositTransaction_nullToAccountId_returns400() throws Exception {
    DepositRequest request = new DepositRequest(
        null, new BigDecimal("100"), "test");

    mockMvc.perform(post("/api/transactions/deposit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("toAccountId"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("NotNull"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(service, never()).deposit(any());
  }

  @Test
  void createDepositTransaction_blankDescription_returns400() throws Exception {
    DepositRequest request = new DepositRequest(
        1L, new BigDecimal("100"), "");

    mockMvc.perform(post("/api/transactions/deposit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("description"))
        .andExpect(jsonPath("$.fieldErrors[0].code").value("NotBlank"))
        .andExpect(jsonPath("$.message").doesNotExist());

    verify(service, never()).deposit(any());
  }

  // -----------GET ALL TRANSACTIONS

  @Test
  void getAllTransactions_defaultParams_returns200() throws Exception {

    List<TransactionResponse> content = List.of(
        new TransactionResponse(
            1L,
            new BigDecimal("100"),
            TransactionType.DEPOSIT,
            null,
            1L,
            LocalDateTime.now(),
            "test"));

    PageResponse<TransactionResponse> pageResponse = new PageResponse<>(content, 0, 10, 1L, 1);

    when(service.getTransactions(0, 10, null))
        .thenReturn(pageResponse);

    mockMvc.perform(get("/api/transactions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.content[0].amount").value(100))
        .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
        .andExpect(jsonPath("$.content[0].toAccountId").value(1))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));

    verify(service).getTransactions(0, 10, null);
  }

  @Test
  void getAllTransactions_withParams_returns200() throws Exception {

    List<TransactionResponse> content = List.of(
        new TransactionResponse(
            2L,
            new BigDecimal("250"),
            TransactionType.TRANSFER,
            1L,
            2L,
            LocalDateTime.now(),
            "transfer test"));

    PageResponse<TransactionResponse> pageResponse = new PageResponse<>(content, 1, 5, 1L, 1);

    when(service.getTransactions(1, 5, 2L))
        .thenReturn(pageResponse);

    mockMvc.perform(get("/api/transactions")
        .param("page", "1")
        .param("size", "5")
        .param("accountId", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(2))
        .andExpect(jsonPath("$.content[0].amount").value(250))
        .andExpect(jsonPath("$.content[0].type").value("TRANSFER"))
        .andExpect(jsonPath("$.content[0].fromAccountId").value(1))
        .andExpect(jsonPath("$.content[0].toAccountId").value(2))
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(5))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));

    verify(service).getTransactions(1, 5, 2L);
  }

  // -----------GET TRANSACTION BY ID

  @Test
  void getTransactionById_validId_returns200() throws Exception {
    TransactionResponse response = new TransactionResponse(
        1L, new BigDecimal("100"), TransactionType.TRANSFER, 1L, 2L,
        LocalDateTime.now(), "test");

    when(service.getTransactionById(1L))
        .thenReturn(response);

    mockMvc.perform(get("/api/transactions/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.amount").value(100))
        .andExpect(jsonPath("$.type").value("TRANSFER"))
        .andExpect(jsonPath("$.fromAccountId").value(1))
        .andExpect(jsonPath("$.toAccountId").value(2))
        .andExpect(jsonPath("$.description").value("test"));

    verify(service).getTransactionById(1L);
  }

  @Test
  void getTransactionById_notFound_returns404() throws Exception {

    when(service.getTransactionById(1L))
        .thenThrow(new TransactionNotFoundException(1L));

    mockMvc.perform(get("/api/transactions/1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Transaction not found with id: 1"))
        .andExpect(jsonPath("$.fieldErrors").doesNotExist());

    verify(service).getTransactionById(1L);
  }
}
