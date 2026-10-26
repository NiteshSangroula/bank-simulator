package com.example.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.api.dto.request.LoginRequest;
import com.example.api.entity.User;
import com.example.api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigurationIntegrationTest {

  private static final String EMAIL = "security@example.com";
  private static final String PASSWORD = "correct-horse";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserDetailsService userDetailsService;

  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void clearUsers() {
    userRepository.deleteAll();
  }

  @Test
  void unauthenticatedRequestsToBankingAndTransactionRoutesReturnUnauthorized() throws Exception {
    mockMvc.perform(get("/api/accounts"))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/transactions"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void userDetailsServiceLoadsEmailAndStoredPasswordHash() {
    User user = new User();
    user.setDisplayName("Security User");
    user.setEmail(EMAIL);
    user.setPasswordHash(passwordEncoder.encode(PASSWORD));
    String storedHash = userRepository.saveAndFlush(user).getPasswordHash();

    UserDetails userDetails = userDetailsService.loadUserByUsername(EMAIL);

    assertEquals(EMAIL, userDetails.getUsername());
    assertEquals(storedHash, userDetails.getPassword());
    assertTrue(userDetails.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
  }

  @Test
  void authenticationManagerAuthenticatesEmailAndPassword() {
    User user = new User();
    user.setDisplayName("Security User");
    user.setEmail(EMAIL);
    user.setPasswordHash(passwordEncoder.encode(PASSWORD));
    userRepository.saveAndFlush(user);

    var authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(EMAIL, PASSWORD));

    assertTrue(authentication.isAuthenticated());
    assertEquals(EMAIL, authentication.getName());
  }

  @Test
  void validLoginRotatesSessionAndPersistsAuthenticationWithoutSensitiveResponseFields() throws Exception {
    saveUser();
    LoginRequest loginRequest = new LoginRequest(EMAIL, PASSWORD);
    MockHttpSession anonymousSession = new MockHttpSession();
    String anonymousSessionId = anonymousSession.getId();

    MvcResult result = mockMvc.perform(post("/api/auth/login")
        .session(anonymousSession)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(loginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(EMAIL))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist())
        .andReturn();

    MockHttpSession authenticatedSession = (MockHttpSession) result.getRequest().getSession(false);
    assertNotNull(authenticatedSession);
    assertNotEquals(anonymousSessionId, authenticatedSession.getId());

    mockMvc.perform(get("/api/accounts").session(authenticatedSession))
        .andExpect(status().isOk());
  }

  @Test
  void invalidCredentialsReturnSameGenericFailureForUnknownEmailAndWrongPassword() throws Exception {
    saveUser();

    mockMvc.perform(post("/api/auth/login")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "wrong-password"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid email or password"));

    mockMvc.perform(post("/api/auth/login")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequest("unknown@example.com", PASSWORD))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid email or password"));
  }

  @Test
  void logoutRequiresCsrfInvalidatesSessionAndRejectsSubsequentProtectedRequests() throws Exception {
    saveUser();
    MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
        .andExpect(status().isOk())
        .andReturn();
    MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
    assertNotNull(session);

    mockMvc.perform(post("/api/auth/logout").session(session))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/accounts").session(session))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
        .andExpect(status().isNoContent());

    assertTrue(session.isInvalid());
    mockMvc.perform(get("/api/accounts"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginRequiresCsrf() throws Exception {
    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
        .andExpect(status().isForbidden());
  }

  private void saveUser() {
    User user = new User();
    user.setDisplayName("Security User");
    user.setEmail(EMAIL);
    user.setPasswordHash(passwordEncoder.encode(PASSWORD));
    userRepository.saveAndFlush(user);
  }
}