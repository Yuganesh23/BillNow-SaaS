package com.example.Billing.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface userRepository
        extends JpaRepository<User_entity, Long> {

    Optional<User_entity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User_entity> findByResetTokenHash(String resetTokenHash);

    List<User_entity> findByShopIdAndRole(
            Long shopId,
            String role
    );

    long countByShopIdAndRole(
            Long shopId,
            String role
    );
}
