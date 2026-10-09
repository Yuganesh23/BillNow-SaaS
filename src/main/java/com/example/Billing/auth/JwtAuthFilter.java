package com.example.Billing.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
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
                || path.equals("/api/auth/login");
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

        System.out.println(
                "JWT Filter: "
                        + request.getMethod()
                        + " "
                        + request.getRequestURI()
        );


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
            System.out.println("JWT: No token found in cookies or header");
            filterChain.doFilter(request, response);
            return;
        }


        try {

            // =============================================
            // VALIDATE TOKEN
            // =============================================

            if (!jwtService.isTokenValid(token)) {

                System.out.println(
                        "JWT: Invalid token"
                );

                filterChain.doFilter(
                        request,
                        response
                );

                return;
            }


            // =============================================
            // EXTRACT EMAIL
            // =============================================

            String email =
                    jwtService.extractEmail(token);


            if (email == null ||
                    email.isBlank()) {

                System.out.println(
                        "JWT: Email missing"
                );

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
                                .findByEmail(email)
                                .orElse(null);


                if (user == null) {

                    System.out.println(
                            "JWT: User not found"
                    );

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

                    System.out.println(
                            "JWT: User inactive"
                    );

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


                System.out.println(
                        "JWT: Role = " + role
                );


                // =========================================
                // CREATE SPRING SECURITY AUTHORITY
                // =========================================

                var authorities =
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role
                                )
                        );


                System.out.println(
                        "JWT: Authority = ROLE_" + role
                );


                // =========================================
                // USER INFORMATION
                // =========================================

                System.out.println(
                        "JWT: Authenticated "
                                + user.getEmail()
                );


                // =========================================
                // SHOP INFORMATION
                // =========================================

                System.out.println(
                        "JWT: Shop ID = "
                                + (
                                user.getShop() != null
                                        ? user.getShop().getId()
                                        : null
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

            System.out.println(
                    "JWT ERROR: "
                            + e.getMessage()
            );

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
