package com.example.Billing.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final userRepository userRepository;


    // =====================================================
    // SKIP JWT FILTER FOR LOGIN AND REGISTER
    // =====================================================

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        String path = request.getServletPath();

        return path.equals("/api/auth/register")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/refresh")
                || path.equals("/api/auth/logout")
                || path.equals("/api/auth/forgot-password")
                || path.equals("/api/auth/reset-password")
                || path.startsWith("/actuator/health");
    }


    // =====================================================
    // JWT FILTER
    // =====================================================

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        log.debug("JWT filter {} {}", request.getMethod(), request.getRequestURI());


        // =================================================
        // GET AUTHORIZATION HEADER
        // =================================================

        String token = null;

        // 1. Try from Cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 2. Fallback to Authorization Header
        if (token == null || token.isBlank()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }


        try {

            // =============================================
            // VALIDATE TOKEN
            // =============================================

            if (!jwtService.isTokenValid(token)) {

                filterChain.doFilter(
                        request,
                        response
                );

                return;
            }


            // =============================================
            // EXTRACT IMMUTABLE USER ID
            // =============================================

            Long userId = jwtService.extractUserId(token);


            if (userId == null) {

                filterChain.doFilter(
                        request,
                        response
                );

                return;
            }


            // =============================================
            // CHECK EXISTING AUTHENTICATION
            // =============================================

            if (SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {


                // =========================================
                // FIND USER
                // =========================================

                User_entity user =
                        userRepository
                                .findById(userId)
                                .orElse(null);


                if (user == null) {

                    filterChain.doFilter(
                            request,
                            response
                    );

                    return;
                }


                // =========================================
                // CHECK USER ACTIVE
                // =========================================

                if (!user.isActive()) {

                    filterChain.doFilter(
                            request,
                            response
                    );

                    return;
                }


                // =========================================
                // GET USER ROLE
                // =========================================

                String role =
                        user.getRole();


                // =========================================
                // CREATE SPRING SECURITY AUTHORITY
                // =========================================

                var authorities =
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role
                                )
                        );


                // =========================================
                // CREATE AUTHENTICATION
                // =========================================

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                authorities
                        );


                // =========================================
                // STORE AUTHENTICATION
                // =========================================

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                                authentication
                        );
            }

        } catch (Exception e) {

            log.debug("JWT authentication rejected: {}", e.getClass().getSimpleName());

            SecurityContextHolder
                    .clearContext();
        }


        // =================================================
        // CONTINUE REQUEST
        // =================================================

        filterChain.doFilter(
                request,
                response
        );
    }
}
