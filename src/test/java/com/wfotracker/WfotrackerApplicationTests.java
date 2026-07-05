package com.wfotracker;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class WfotrackerApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {}

    @Test
    void testDefaultAdminCredentials() {
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        assertTrue(adminOpt.isPresent(), "Default admin user should be initialized");
        User admin = adminOpt.get();
        assertFalse(admin.isPasswordChanged(), "Admin password_changed should be false on first initialization");
        assertTrue(
                passwordEncoder.matches("admin@123", admin.getPassword()),
                "Admin password should match default 'admin@123'");
    }
}
