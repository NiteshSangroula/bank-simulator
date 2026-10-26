package com.example.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.example.api.dto.request.LoginRequest;
import com.example.api.dto.request.RegisterRequest;
import com.example.api.dto.response.LoginResponse;
import com.example.api.dto.response.RegisterResponse;
import com.example.api.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication APIs", description = "User registration")
public class AuthController {
  private final AuthService authService;
  private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
  private final SecurityContextRepository securityContextRepository;

  public AuthController(
      AuthService authService,
      SessionAuthenticationStrategy sessionAuthenticationStrategy,
      SecurityContextRepository securityContextRepository) {
    this.authService = authService;
    this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    this.securityContextRepository = securityContextRepository;
  }

  @Operation(summary = "Register a user")
  @PostMapping("/register")
  public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
  }

  @Operation(summary = "Log in")
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) {
    Authentication authentication = authService.authenticate(request);
    sessionAuthenticationStrategy.onAuthentication(authentication, servletRequest, servletResponse);

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, servletRequest, servletResponse);

    return ResponseEntity.ok(new LoginResponse(authentication.getName()));
  }
}