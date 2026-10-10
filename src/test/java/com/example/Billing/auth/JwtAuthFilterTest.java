package com.example.Billing.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JwtAuthFilterTest {

    private final JwtAuthFilter filter = new JwtAuthFilter(
            mock(JwtService.class),
            mock(userRepository.class)
    );

    @Test
    void skipsEveryPublicAuthenticationEndpoint() {
        assertTrue(shouldSkip("/api/auth/register"));
        assertTrue(shouldSkip("/api/auth/login"));
        assertTrue(shouldSkip("/api/auth/logout"));
        assertTrue(shouldSkip("/api/auth/forgot-password"));
        assertTrue(shouldSkip("/api/auth/reset-password"));
    }

    @Test
    void skipsHealthChecksButProtectsApplicationEndpoints() {
        assertTrue(shouldSkip("/actuator/health"));
        assertTrue(shouldSkip("/actuator/health/liveness"));
        assertFalse(shouldSkip("/api/shops"));
    }

    private boolean shouldSkip(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(path);
        return filter.shouldNotFilter(request);
    }
}
