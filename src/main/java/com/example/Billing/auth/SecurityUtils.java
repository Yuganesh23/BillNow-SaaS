package com.example.Billing.auth;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    public User_entity getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        // =============================================
        // CHECK AUTHENTICATION
        // =============================================

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "User is not authenticated"
            );
        }

        // =============================================
        // GET PRINCIPAL
        // =============================================

        Object principal =
                authentication.getPrincipal();

        // =============================================
        // YOUR JwtAuthFilter STORES User_entity
        // AS THE PRINCIPAL
        // =============================================

        if (principal instanceof User_entity user) {

            return user;
        }

        // =============================================
        // INVALID PRINCIPAL
        // =============================================

        throw new RuntimeException(
                "Authenticated principal is not a valid user"
        );
    }
}
