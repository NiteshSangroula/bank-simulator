package com.example.api.service;

import java.sql.SQLException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.api.dto.request.LoginRequest;
import com.example.api.dto.request.RegisterRequest;
import com.example.api.dto.response.RegisterResponse;
import com.example.api.entity.User;
import com.example.api.exception.EmailAlreadyExistsException;
import com.example.api.repository.UserRepository;

@Service
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthenticationManager authenticationManager) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
  }

  public Authentication authenticate(LoginRequest request) {
    return authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
  }

  @Transactional
  public RegisterResponse register(RegisterRequest request) {
    if (userRepository.findByEmail(request.email()).isPresent()) {
      throw new EmailAlreadyExistsException();
    }

    User user = new User();
    user.setDisplayName(request.displayName());
    user.setEmail(request.email());
    user.setPasswordHash(passwordEncoder.encode(request.password()));

    try {
      return toResponse(userRepository.saveAndFlush(user));
    } catch (DataIntegrityViolationException exception) {
      if (isUniqueConstraintViolation(exception)) {
        throw new EmailAlreadyExistsException();
      }
      throw exception;
    }
  }

  private boolean isUniqueConstraintViolation(DataIntegrityViolationException exception) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  private RegisterResponse toResponse(User user) {
    return new RegisterResponse(
        user.getId(),
        user.getDisplayName(),
        user.getEmail(),
        user.getCreatedAt());
  }
}