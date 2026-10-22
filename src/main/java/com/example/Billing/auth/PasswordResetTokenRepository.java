package com.example.Billing.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken_entity, Long> {
    Optional<PasswordResetToken_entity> findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(Long userId);
}
