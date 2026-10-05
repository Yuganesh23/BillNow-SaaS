package com.example.Billing.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        return ResponseEntity
                .status(HttpStatus.CREATED)
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

        return ResponseEntity.ok(
                response
        );
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