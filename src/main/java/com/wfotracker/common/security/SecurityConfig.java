package com.wfotracker.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final PasswordChangeFilter passwordChangeFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        try {
            http.authorizeHttpRequests(auth -> auth.requestMatchers(
                                    "/css/**",
                                    "/js/**",
                                    "/images/**",
                                    "/webjars/**",
                                    "/favicon.ico",
                                    "/error",
                                    "/error/**")
                            .permitAll()
                            .requestMatchers("/login")
                            .permitAll()
                            .requestMatchers("/change-password")
                            .authenticated()
                            .requestMatchers("/admin/**")
                            .hasRole("ADMIN")
                            .requestMatchers("/manager/**")
                            .hasRole("MANAGER")
                            .requestMatchers("/employee/**")
                            .hasRole("EMPLOYEE")
                            .anyRequest()
                            .authenticated())
                    .formLogin(form -> form.loginPage("/login")
                            .successHandler(customAuthenticationSuccessHandler)
                            .permitAll())
                    .exceptionHandling(exception -> exception.accessDeniedPage("/error/403"))
                    .logout(logout -> logout.logoutUrl("/logout")
                            .logoutSuccessUrl("/login?logout")
                            .permitAll())
                    .addFilterAfter(passwordChangeFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build security filter chain", e);
        }
    }
}
