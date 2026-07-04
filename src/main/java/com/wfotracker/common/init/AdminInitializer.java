package com.wfotracker.common.init;

import java.security.SecureRandom;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.wfotracker.domain.entity.Role;
import com.wfotracker.domain.entity.User;
import com.wfotracker.domain.repository.RoleRepository;
import com.wfotracker.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBER = "0123456789";
    private static final String OTHER_CHAR = "!@#$";
    private static final String PASSWORD_ALLOW_BASE = CHAR_LOWER + CHAR_UPPER + NUMBER + OTHER_CHAR;
    private static final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Find if any admin user exists
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        if (adminOpt.isEmpty()) {
            log.info("No system administrator found. Initializing default admin account...");

            String rawPassword = generateSecurePassword(10);
            String encodedPassword = passwordEncoder.encode(rawPassword);

            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
                Role role = new Role();
                role.setName("ROLE_ADMIN");
                return roleRepository.save(role);
            });

            User admin = new User();
            admin.setFullName("System Admin");
            admin.setUsername("admin");
            admin.setPassword(encodedPassword);
            admin.setPasswordChanged(true); // Admin does not require password change on first login
            admin.setActive(true);
            admin.getRoles().add(adminRole);

            userRepository.save(admin);

            // Log password with clear formatting
            log.info("=================================================");
            log.info("WFO-TRACKER ADMIN ACCOUNT CREATED SUCCESSFULLY!");
            log.info("Username: admin");
            log.info("Password: {}", rawPassword);
            log.info("=================================================");
        } else {
            log.info("System administrator account 'admin' already exists. Skipping initialization.");
        }
    }

    private String generateSecurePassword(int length) {
        if (length < 4) {
            throw new IllegalArgumentException("Password length must be at least 4 characters");
        }

        StringBuilder password = new StringBuilder(length);

        // Ensure we have at least one character from each group
        password.append(CHAR_LOWER.charAt(random.nextInt(CHAR_LOWER.length())));
        password.append(CHAR_UPPER.charAt(random.nextInt(CHAR_UPPER.length())));
        password.append(NUMBER.charAt(random.nextInt(NUMBER.length())));
        password.append(OTHER_CHAR.charAt(random.nextInt(OTHER_CHAR.length())));

        // Fill remaining characters
        for (int i = 4; i < length; i++) {
            password.append(PASSWORD_ALLOW_BASE.charAt(random.nextInt(PASSWORD_ALLOW_BASE.length())));
        }

        // Shuffle characters
        char[] passwordChars = password.toString().toCharArray();
        for (int i = passwordChars.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            char temp = passwordChars[index];
            passwordChars[index] = passwordChars[i];
            passwordChars[i] = temp;
        }

        return new String(passwordChars);
    }
}
