package com.wfotracker.common.init;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.default.admin.username:admin}")
    private String defaultAdminUsername;

    @Value("${app.default.admin.password:admin@123}")
    private String defaultAdminPassword;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Find if any admin user exists
        Optional<User> adminOpt = userRepository.findByUsername(defaultAdminUsername);
        if (adminOpt.isEmpty()) {
            log.info("No system administrator found. Initializing default admin account...");

            String encodedPassword = passwordEncoder.encode(defaultAdminPassword);

            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
                Role role = new Role();
                role.setName("ROLE_ADMIN");
                return roleRepository.save(role);
            });

            User admin = new User();
            admin.setFullName("System Admin");
            admin.setUsername(defaultAdminUsername);
            admin.setPassword(encodedPassword);
            admin.setPasswordChanged(false); // Admin MUST change password on first login
            admin.setActive(true);
            admin.getRoles().add(adminRole);

            userRepository.save(admin);
            log.info("Default system administrator account 'admin' initialized successfully.");
        } else {
            log.info("System administrator account 'admin' already exists. Skipping initialization.");
        }
    }
}
