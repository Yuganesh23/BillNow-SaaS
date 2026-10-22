package com.example.Billing.auth;

import com.example.Billing.notification.EmailService;
import com.example.Billing.shop.ShopRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

class AuthServiceOtpHashTest {
    @Test
    void otpHashUsesServerSecretHmacInsteadOfBareDigest() throws Exception {
        AuthService service = new AuthService(
                mock(userRepository.class), mock(ShopRepository.class), mock(org.springframework.security.crypto.password.PasswordEncoder.class),
                mock(JwtService.class), mock(EmailService.class), mock(PasswordResetTokenRepository.class),
                mock(RefreshTokenRepository.class));
        String secret = "server-side-test-secret";
        ReflectionTestUtils.setField(service, "otpHmacSecret", secret);

        String emailAndOtp = "owner@example.test:123456";
        String actual = ReflectionTestUtils.invokeMethod(service, "hashOtp", "owner@example.test", "123456");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = HexFormat.of().formatHex(mac.doFinal(emailAndOtp.getBytes(StandardCharsets.UTF_8)));
        String bareDigest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(emailAndOtp.getBytes(StandardCharsets.UTF_8)));

        assertEquals(expected, actual);
        assertNotEquals(bareDigest, actual);
    }
}
