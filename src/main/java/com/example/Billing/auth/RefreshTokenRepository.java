package com.example.Billing.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken_entity, Long> {
    Optional<RefreshToken_entity> findByTokenHash(String tokenHash);
    List<RefreshToken_entity> findByUserIdAndRevokedAtIsNull(Long userId);
}
