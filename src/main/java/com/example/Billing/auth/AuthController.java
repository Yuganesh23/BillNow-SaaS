package com.example.Billing.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @Value("${auth.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${auth.cookie.same-site:Lax}")
    private String sameSite;


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

        return authenticatedResponse(response, HttpStatus.CREATED);
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

        return authenticatedResponse(response, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse_Dto> refresh(HttpServletRequest request) {
        String refreshToken = cookieValue(request, "refresh_token");
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
        refreshTokenService.revoke(cookieValue(request, "refresh_token"));
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, expiredCookie("jwt").toString());
        headers.add(HttpHeaders.SET_COOKIE, expiredCookie("refresh_token").toString());
        return ResponseEntity.ok().headers(headers).body(Map.of("message", "Logged out successfully"));
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

    private ResponseEntity<AuthResponse_Dto> authenticatedResponse(AuthResponse_Dto response, HttpStatus status) {
        String accessToken = response.getToken();
        String refreshToken = refreshTokenService.issue(response.getUserId());
        response.setToken(null);
        return ResponseEntity.status(status)
                .headers(cookieHeaders(accessToken, refreshToken))
                .body(response);
    }

    private HttpHeaders cookieHeaders(String accessToken, String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, authCookie("jwt", accessToken, Duration.ofMinutes(jwtService.getAccessTokenMinutes())).toString());
        headers.add(HttpHeaders.SET_COOKIE, authCookie("refresh_token", refreshToken, Duration.ofDays(7)).toString());
        return headers;
    }

    private ResponseCookie authCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true).secure(secureCookie).path("/").maxAge(maxAge).sameSite(sameSite).build();
    }

    private ResponseCookie expiredCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true).secure(secureCookie).path("/").maxAge(Duration.ZERO).sameSite(sameSite).build();
    }

    private String cookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return List.of(request.getCookies()).stream()
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);
    }
}
