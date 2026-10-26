package com.example.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.api.entity.User;
import com.example.api.repository.UserRepository;

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
}