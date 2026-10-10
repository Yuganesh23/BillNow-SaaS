package com.example.Billing.auth;

import com.example.Billing.common.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final userRepository userRepository;

    @Value("${auth.refresh-token-days:7}")
    private long refreshTokenDays;

    @Transactional
    public String issue(Long userId) {
        User_entity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "User not found"));
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now();
        repository.save(RefreshToken_entity.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plus(Duration.ofDays(refreshTokenDays)))
                .build());
        return rawToken;
    }

    @Transactional
    public Rotation rotate(String rawToken) {
        RefreshToken_entity current = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> invalidToken());
        Instant now = Instant.now();
        if (current.getRevokedAt() != null || !current.getExpiresAt().isAfter(now) || !current.getUser().isActive()) {
            throw invalidToken();
        }
        current.setRevokedAt(now);
        repository.save(current);
        String next = issue(current.getUser().getId());
        return new Rotation(current.getUser(), next);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                repository.save(token);
            }
        });
    }

    private AppException invalidToken() {
        return new AppException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Session has expired. Please sign in again.");
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash refresh token", exception);
        }
    }

    public record Rotation(User_entity user, String refreshToken) {}
}
