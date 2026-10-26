package com.example.api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.api.dto.request.RegisterRequest;
import com.example.api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRegistrationIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void clearUsers() {
    userRepository.deleteAll();
  }

  @Test
  void register_validRequestReturnsSafeResponseAndStoresHashedPassword() throws Exception {
    RegisterRequest request = new RegisterRequest("Alex Morgan", "alex@example.com", "correct-horse");

    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
      .content(objectMapper.writeValueAsString(request))
      .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.displayName").value("Alex Morgan"))
        .andExpect(jsonPath("$.email").value("alex@example.com"))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());

    var storedUser = userRepository.findByEmail("alex@example.com").orElseThrow();
    org.junit.jupiter.api.Assertions.assertNotEquals("correct-horse", storedUser.getPasswordHash());
    org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("correct-horse", storedUser.getPasswordHash()));
  }

  @Test
  void register_invalidInputReturnsBadRequest() throws Exception {
    RegisterRequest request = new RegisterRequest(" ", "alex@example.com", "correct-horse");

    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
      .content(objectMapper.writeValueAsString(request))
      .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("displayName"));
  }

  @Test
  void register_duplicateEmailReturnsConflict() throws Exception {
    RegisterRequest request = new RegisterRequest("Alex Morgan", "alex@example.com", "correct-horse");
    String body = objectMapper.writeValueAsString(request);

    mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body).with(csrf()))
        .andExpect(status().isCreated());
    mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body).with(csrf()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("An account with this email already exists"));
  }
}