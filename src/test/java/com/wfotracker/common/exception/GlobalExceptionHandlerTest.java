package com.wfotracker.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @RestController
    static class TestController {
        @GetMapping("/test-illegal-argument")
        public void testIllegalArgument() {
            throw new IllegalArgumentException("Invalid argument provided");
        }

        @GetMapping("/test-exception")
        public void testException() throws Exception {
            throw new Exception("General unexpected error");
        }

        @GetMapping("/test-access-denied")
        public void testAccessDenied() {
            throw new org.springframework.security.access.AccessDeniedException("Access denied test");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(globalExceptionHandler)
                .build();
    }

    @Test
    void handleIllegalArgumentExceptionTest() throws Exception {
        mockMvc.perform(get("/test-illegal-argument"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/400"))
                .andExpect(model().attribute("error", "Invalid argument provided"));
    }

    @Test
    void handleGeneralExceptionTest() throws Exception {
        mockMvc.perform(get("/test-exception"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/500"))
                .andExpect(model().attribute("error", "An unexpected error occurred. Please contact support."));
    }

    @Test
    void handleAccessDeniedExceptionTest() throws Exception {
        try {
            mockMvc.perform(get("/test-access-denied"));
            org.junit.jupiter.api.Assertions.fail("Should have thrown AccessDeniedException");
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.assertTrue(
                    e.getCause() instanceof org.springframework.security.access.AccessDeniedException);
        }
    }
}
