package com.wfotracker.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wfotracker.auth.dto.ChangePasswordRequest;
import com.wfotracker.common.security.CustomUserDetails;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changePassword(CustomUserDetails userDetails, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = userRepository
                .findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChanged(true);
        userRepository.save(user);
    }
}
