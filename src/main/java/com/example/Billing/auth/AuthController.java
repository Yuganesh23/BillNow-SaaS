package com.example.Billing.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final AuthRateLimiter authRateLimiter;
    private final AuthCookies authCookies;




    // =====================================================
    // REGISTER
    // =====================================================

    @PostMapping("/register")
    public ResponseEntity<AuthResponse_Dto> register(
            @Valid
            @RequestBody
            RegisterRequest_Dto request,
            HttpServletRequest httpRequest
    ) {

        authRateLimiter.checkRegister(httpRequest);

        AuthResponse_Dto response =
                authService.register(
                        request
                );

        return authenticatedResponse(response, HttpStatus.CREATED);
    }


    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<AuthResponse_Dto> login(
            @Valid
            @RequestBody
            LoginRequest_Dto request,
            HttpServletRequest httpRequest
    ) {

        authRateLimiter.checkLogin(httpRequest, request.getEmail());

        AuthResponse_Dto response =
                authService.login(
                        request
                );

        return authenticatedResponse(response, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse_Dto> refresh(HttpServletRequest request) {
        String refreshToken = authCookies.refreshToken(request);
        if (refreshToken == null) {
            throw new com.example.Billing.common.AppException(
                    HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Session has expired. Please sign in again.");
        }
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        AuthResponse_Dto response = authService.responseFor(rotation.user());
        String accessToken = response.getToken();
        response.setToken(null);
        return ResponseEntity.ok()
                .headers(cookieHeaders(accessToken, rotation.refreshToken()))
                .body(response);
    }

    
    // =====================================================
    // LOGOUT
    // =====================================================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        refreshTokenService.revoke(authCookies.refreshToken(request));
        return ResponseEntity.ok().headers(authCookies.clear()).body(Map.of("message", "Logged out successfully"));
    }

    // =====================================================
    // RESET PASSWORD
    // =====================================================
    @PostMapping("/forgot-password")
    public ResponseEntity<java.util.Map<String, String>> forgotPassword(
            @RequestBody java.util.Map<String, String> request,
            HttpServletRequest httpRequest
    ) {
        String email = request.get("email");
        authRateLimiter.checkForgotPassword(httpRequest, email);
        authService.forgotPassword(email);
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "OTP sent to email");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<java.util.Map<String, String>> resetPassword(
            @Valid
            @RequestBody
            ResetPasswordRequest_Dto request,
            HttpServletRequest httpRequest
    ) {
        authRateLimiter.checkResetPassword(httpRequest, request.getEmail());
        authService.resetPassword(request);
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "Password reset successfully");
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<AuthResponse_Dto> authenticatedResponse(AuthResponse_Dto response, HttpStatus status) {
        String accessToken = response.getToken();
        String refreshToken = refreshTokenService.issue(response.getUserId());
        response.setToken(null);
        return ResponseEntity.status(status)
                .headers(cookieHeaders(accessToken, refreshToken))
                .body(response);
    }

    private HttpHeaders cookieHeaders(String accessToken, String refreshToken) {
        return authCookies.issue(
                accessToken, Duration.ofMinutes(jwtService.getAccessTokenMinutes()),
                refreshToken, Duration.ofDays(refreshTokenService.getRefreshTokenDays()));
    }

}
