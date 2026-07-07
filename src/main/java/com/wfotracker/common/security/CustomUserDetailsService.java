package com.wfotracker.common.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final HttpServletRequest request;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String selectedRole = request.getParameter("role");
        if (selectedRole == null || selectedRole.trim().isEmpty()) {
            throw new UsernameNotFoundException("Role selection is mandatory");
        }

        User user = userRepository
                .findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found or inactive: " + username));

        boolean hasRole =
                user.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase(selectedRole.trim()));

        if (!hasRole) {
            throw new UsernameNotFoundException("User does not possess the selected role");
        }

        return new CustomUserDetails(user);
    }
}
