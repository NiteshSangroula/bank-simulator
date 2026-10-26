package com.example.api.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.AuthenticationManager;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.api.dto.request.RegisterRequest;
import com.example.api.entity.User;
import com.example.api.exception.EmailAlreadyExistsException;
import com.example.api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private AuthenticationManager authenticationManager;

  @Test
  void registerUniqueConstraintViolationReturnsEmailConflict() {
    RegisterRequest request = new RegisterRequest("Alex Morgan", "alex@example.com", "correct-horse");
    AuthService authService = new AuthService(userRepository, passwordEncoder, authenticationManager);

    when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
    when(userRepository.saveAndFlush(any(User.class))).thenThrow(
        new DataIntegrityViolationException("unique email", new SQLException("duplicate", "23505")));

    assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
  }
}