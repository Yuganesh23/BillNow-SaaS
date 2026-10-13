package com.example.Billing.auth;

import com.example.Billing.common.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthRateLimiterTest {
    @Test
    void loginReturns429WithRetryAfterWhenLimitIsExceeded() throws Exception {
        AuthRateLimiter limiter = new AuthRateLimiter(60, 1, 1, 10, 10, 10, 10, 10, Clock.systemUTC());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LimitedLoginController(limiter))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/test/login").param("email", "owner@example.test"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/test/login").param("email", "owner@example.test"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @RestController
    static class LimitedLoginController {
        private final AuthRateLimiter limiter;

        LimitedLoginController(AuthRateLimiter limiter) {
            this.limiter = limiter;
        }

        @PostMapping("/test/login")
        ResponseEntity<Void> login(@RequestParam String email, HttpServletRequest request) {
            limiter.checkLogin(request, email);
            return ResponseEntity.noContent().build();
        }
    }
}
