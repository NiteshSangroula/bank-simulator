package com.example.api.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.example.api.entity.User;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Test
  void savesUserAndFindsItByEmail() {
    User user = newUser("alex@example.com");

    User savedUser = userRepository.saveAndFlush(user);

    User foundUser = userRepository.findByEmail("alex@example.com").orElseThrow();
    assertNotNull(savedUser.getId());
    assertNotNull(savedUser.getCreatedAt());
    assertEquals(savedUser.getId(), foundUser.getId());
    assertEquals("Alex Morgan", foundUser.getDisplayName());
  }

  @Test
  void rejectsDuplicateEmail() {
    userRepository.saveAndFlush(newUser("alex@example.com"));

    assertThrows(DataIntegrityViolationException.class,
        () -> userRepository.saveAndFlush(newUser("alex@example.com")));
  }

  @Test
  void rejectsInvalidEmail() {
    User user = newUser("not-an-email");

    assertThrows(ConstraintViolationException.class,
        () -> userRepository.saveAndFlush(user));
  }

  private User newUser(String email) {
    User user = new User();
    user.setDisplayName("Alex Morgan");
    user.setEmail(email);
    user.setPasswordHash("encoded-password-hash");
    return user;
  }
}