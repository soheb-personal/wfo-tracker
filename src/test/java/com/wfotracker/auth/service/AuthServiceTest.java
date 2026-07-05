package com.wfotracker.auth.service;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.wfotracker.auth.dto.ChangePasswordRequest;
import com.wfotracker.common.security.CustomUserDetails;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFullName("Test User");
        user.setUsername("testuser");

        userDetails = new CustomUserDetails(user);
    }

    @Test
    void testChangePassword_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("encodedNewPass");

        ChangePasswordRequest request = new ChangePasswordRequest("newpass", "newpass");
        authService.changePassword(userDetails, request);

        assertTrue(user.isPasswordChanged());
        verify(userRepository).save(user);
    }

    @Test
    void testChangePassword_PasswordsMismatch() {
        ChangePasswordRequest request = new ChangePasswordRequest("newpass", "otherpass");
        assertThrows(IllegalArgumentException.class, () -> authService.changePassword(userDetails, request));
    }

    @Test
    void testChangePassword_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ChangePasswordRequest request = new ChangePasswordRequest("newpass", "newpass");
        assertThrows(IllegalArgumentException.class, () -> authService.changePassword(userDetails, request));
    }
}
