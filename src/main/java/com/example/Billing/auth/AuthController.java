package com.example.Billing.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    // =====================================================
    // REGISTER
    // =====================================================

    @PostMapping("/register")
    public ResponseEntity<AuthResponse_Dto> register(
            @Valid
            @RequestBody
            RegisterRequest_Dto request
    ) {

        AuthResponse_Dto response =
                authService.register(
                        request
                );

        ResponseCookie sessionCookie = ResponseCookie.from("__session", response.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60) // 24 hours (1 day)
                .sameSite("Lax")
                .build();

        ResponseCookie jwtCookie = ResponseCookie.from("jwt", response.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60) // 24 hours (1 day)
                .sameSite("Lax")
                .build();
                
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(response);
    }


    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid
            @RequestBody
            LoginRequest_Dto request
    ) {
        try {
            AuthResponse_Dto response =
                    authService.login(
                            request
                    );

            ResponseCookie sessionCookie = ResponseCookie.from("__session", response.getToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(24 * 60 * 60) // 24 hours (1 day)
                    .sameSite("Lax")
                    .build();

            ResponseCookie jwtCookie = ResponseCookie.from("jwt", response.getToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(24 * 60 * 60) // 24 hours (1 day)
                    .sameSite("Lax")
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(response);
        } catch (RuntimeException e) {
            System.err.println("Login failed for " + request.getEmail() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("message", e.getMessage() != null ? e.getMessage() : "Invalid email or password"));
        } catch (Exception e) {
            System.err.println("Unexpected login error for " + request.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message", "An unexpected error occurred during login"));
        }
    }

    
    // =====================================================
    // LOGOUT
    // =====================================================
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie sessionCookie = ResponseCookie.from("__session", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0) // Expire immediately
                .sameSite("Lax")
                .build();

        ResponseCookie jwtCookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0) // Expire immediately
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body("Logged out successfully");
    }

    // =====================================================
    // RESET PASSWORD
    // =====================================================
    @PostMapping("/forgot-password")
    public ResponseEntity<java.util.Map<String, String>> forgotPassword(
            @RequestBody java.util.Map<String, String> request
    ) {
        String email = request.get("email");
        authService.forgotPassword(email);
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "OTP sent to email");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<java.util.Map<String, String>> resetPassword(
            @Valid
            @RequestBody
            ResetPasswordRequest_Dto request
    ) {
        authService.resetPassword(request);
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "Password reset successfully");
        return ResponseEntity.ok(response);
    }
}