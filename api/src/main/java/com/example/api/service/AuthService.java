package com.example.api.service;

import java.sql.SQLException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.api.dto.request.RegisterRequest;
import com.example.api.dto.response.RegisterResponse;
import com.example.api.entity.User;
import com.example.api.exception.EmailAlreadyExistsException;
import com.example.api.repository.UserRepository;

@Service
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
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