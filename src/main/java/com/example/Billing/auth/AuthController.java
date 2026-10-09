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

        ResponseCookie cookie = ResponseCookie.from("jwt", response.getToken())
                .httpOnly(true)
                .secure(false) // Set true in production (HTTPS)
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7 days
                .sameSite("Lax")
                .build();
                
        // Return without token in body to be safe, or leave it for now.
        // We'll let frontend ignore it and just use the cookie.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }


    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<AuthResponse_Dto> login(
            @Valid
            @RequestBody
            LoginRequest_Dto request
    ) {

        AuthResponse_Dto response =
                authService.login(
                        request
                );

        ResponseCookie cookie = ResponseCookie.from("jwt", response.getToken())
                .httpOnly(true)
                .secure(false) // Set true in prod
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    
    // =====================================================
    // LOGOUT
    // =====================================================
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0) // Expire immediately
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
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