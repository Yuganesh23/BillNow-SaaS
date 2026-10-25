package com.example.Billing.auth;

import com.example.Billing.notification.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private userRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User_entity testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "frontendBaseUrl", "https://xenbill.web.app");

        testUser = User_entity.builder()
                .id(1L)
                .name("Test Owner")
                .email("owner@example.com")
                .password("$2a$10$oldHashedPassword123")
                .role("SHOP_OWNER")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("1. Valid reset request stores SHA-256 token hash and calls Resend with raw token link")
    void testValidPasswordResetRequest() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(testUser));
        when(emailService.sendPasswordResetEmail(eq("owner@example.com"), eq("Test Owner"), anyString())).thenReturn(true);

        authService.forgotPassword("owner@example.com");

        // Verify user was updated with a 64-character SHA-256 hash and future expiry
        verify(userRepository, times(1)).save(testUser);
        assertNotNull(testUser.getResetTokenHash());
        assertEquals(64, testUser.getResetTokenHash().length(), "SHA-256 hash must be 64 hex characters");
        assertNotNull(testUser.getResetTokenExpiry());
        assertTrue(testUser.getResetTokenExpiry().isAfter(LocalDateTime.now()));

        // Capture link sent in email
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("owner@example.com"), eq("Test Owner"), linkCaptor.capture());
        String capturedUrl = linkCaptor.getValue();

        assertTrue(capturedUrl.startsWith("https://xenbill.web.app/reset-password?token="));
        String rawToken = capturedUrl.substring(capturedUrl.indexOf("token=") + 6);
        assertEquals(64, rawToken.length(), "Raw token has 32 secure bytes (64 hex characters)");

        // Verify that the stored hash matches SHA-256 of the raw token sent
        assertEquals(authService.hashToken(rawToken), testUser.getResetTokenHash());
    }

    @Test
    @DisplayName("2. Unknown email address returns generic success without revealing user absence")
    void testUnknownEmailReturnsGenericSuccess() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Should return gracefully without throwing exception
        assertDoesNotThrow(() -> authService.forgotPassword("unknown@example.com"));

        // Email service and save should never be called
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Resend email dispatch failure throws informative exception")
    void testResendFailureThrowsException() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(testUser));
        when(emailService.sendPasswordResetEmail(anyString(), anyString(), anyString())).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            authService.forgotPassword("owner@example.com");
        });

        assertTrue(ex.getMessage().contains("Failed to dispatch password reset email"));
    }

    @Test
    @DisplayName("4. Successful password reset encodes new password with PasswordEncoder and clears token")
    void testSuccessfulPasswordReset() {
        String rawToken = "abcd1234efgh5678abcd1234efgh5678abcd1234efgh5678abcd1234efgh5678";
        String tokenHash = authService.hashToken(rawToken);

        testUser.setResetTokenHash(tokenHash);
        testUser.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));

        when(userRepository.findByResetTokenHash(tokenHash)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("brandNewSecurePassword123")).thenReturn("$2a$10$brandNewHashedPassword456");

        ResetPasswordRequest_Dto request = new ResetPasswordRequest_Dto();
        request.setToken(rawToken);
        request.setNewPassword("brandNewSecurePassword123");

        authService.resetPassword(request);

        // Verify password was hashed and saved
        verify(passwordEncoder).encode("brandNewSecurePassword123");
        assertEquals("$2a$10$brandNewHashedPassword456", testUser.getPassword());

        // Single-use guarantee: verify token and expiry were cleared
        assertNull(testUser.getResetTokenHash());
        assertNull(testUser.getResetTokenExpiry());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("5. Invalid reset token is rejected")
    void testInvalidTokenRejected() {
        String invalidRawToken = "invalidTokenThatDoesNotExist123456789012345678901234567890123456";
        String tokenHash = authService.hashToken(invalidRawToken);

        when(userRepository.findByResetTokenHash(tokenHash)).thenReturn(Optional.empty());

        ResetPasswordRequest_Dto request = new ResetPasswordRequest_Dto();
        request.setToken(invalidRawToken);
        request.setNewPassword("newPassword123");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            authService.resetPassword(request);
        });

        assertTrue(ex.getMessage().contains("Invalid or expired password reset link"));
    }

    @Test
    @DisplayName("6. Expired reset token is rejected")
    void testExpiredTokenRejected() {
        String rawToken = "expiredToken1234567890123456789012345678901234567890123456789012";
        String tokenHash = authService.hashToken(rawToken);

        testUser.setResetTokenHash(tokenHash);
        testUser.setResetTokenExpiry(LocalDateTime.now().minusMinutes(5)); // Expired 5 mins ago

        when(userRepository.findByResetTokenHash(tokenHash)).thenReturn(Optional.of(testUser));

        ResetPasswordRequest_Dto request = new ResetPasswordRequest_Dto();
        request.setToken(rawToken);
        request.setNewPassword("newPassword123");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            authService.resetPassword(request);
        });

        assertTrue(ex.getMessage().contains("expired"));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("7. Already consumed token cannot be reused")
    void testConsumedTokenCannotBeReused() {
        String rawToken = "alreadyConsumedToken123456789012345678901234567890123456789012345";
        String tokenHash = authService.hashToken(rawToken);

        // When looked up after consumption, user has null token hash or not found
        when(userRepository.findByResetTokenHash(tokenHash)).thenReturn(Optional.empty());

        ResetPasswordRequest_Dto request = new ResetPasswordRequest_Dto();
        request.setToken(rawToken);
        request.setNewPassword("newPassword123");

        assertThrows(RuntimeException.class, () -> authService.resetPassword(request));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("8. Password shorter than 6 characters is rejected")
    void testShortPasswordRejected() {
        ResetPasswordRequest_Dto request = new ResetPasswordRequest_Dto();
        request.setToken("anyToken12345678901234567890123456789012345678901234567890123456");
        request.setNewPassword("12345"); // 5 chars

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            authService.resetPassword(request);
        });

        assertTrue(ex.getMessage().contains("at least 6 characters"));
    }
}
