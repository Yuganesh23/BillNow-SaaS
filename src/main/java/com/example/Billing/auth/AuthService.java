package com.example.Billing.auth;

import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Value("${frontend.base-url:${FRONTEND_BASE_URL:https://xenbill.web.app}}")
    private String frontendBaseUrl;

    private final userRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final com.example.Billing.notification.EmailService emailService;
    private final java.util.Map<String, String> otpStorage = new java.util.concurrent.ConcurrentHashMap<>();


    // =====================================================
    // REGISTER
    // =====================================================

    @Transactional
    public AuthResponse_Dto register(
            RegisterRequest_Dto request
    ) {

        // =================================================
        // CHECK EMAIL
        // =================================================

        if (userRepository.existsByEmail(
                request.getEmail()
        )) {

            throw new RuntimeException(
                    "Email already registered"
            );
        }


        // =================================================
        // CREATE OWNER
        // =================================================

        User_entity user =
                User_entity.builder()
                        .name(
                                request.getOwnerName()
                        )
                        .email(
                                request.getEmail()
                        )
                        .mobileNumber(
                                request.getMobileNumber()
                        )
                        .password(
                                passwordEncoder.encode(
                                        request.getPassword()
                                )
                        )
                        .role(
                                "SHOP_OWNER"
                        )
                        .active(
                                true
                        )
                        .build();

        // =================================================
        // SAVE OWNER
        // =================================================

        user =
                userRepository.save(
                        user
                );

        // =================================================
        // CREATE SHOP
        // =================================================

        Shop_entity shop =
                Shop_entity.builder()
                        .name(
                                request.getShopName()
                        )
                        .invoiceName(
                                request.getShopName()
                        )
                        .email(
                                request.getEmail()
                        )
                        .mobileNumber(
                                request.getMobileNumber()
                        )
                        .address(
                                request.getAddress()
                        )
                        .logoBase64(
                                request.getLogoBase64()
                        )
                        .active(
                                true
                        )
                        .owner(
                                user
                        )
                        .build();


        // =================================================
        // SAVE SHOP
        // =================================================

        shop =
                shopRepository.save(
                        shop
                );


        // =================================================
        // CONNECT OWNER TO SHOP
        // =================================================

        user.setShop(
                shop
        );

        user =
                userRepository.save(
                        user
                );


        // =================================================
        // GENERATE JWT
        // =================================================

        String token =
                jwtService.generateToken(
                        user
                );


        // =================================================
        // SEND WELCOME EMAIL (RESEND)
        // =================================================
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getName(), shop.getName());
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        // =================================================
        // RETURN RESPONSE
        // =================================================

        return AuthResponse_Dto.builder()

                .token(
                        token
                )

                .userId(
                        user.getId()
                )

                .name(
                        user.getName()
                )

                .email(
                        user.getEmail()
                )

                .role(
                        user.getRole()
                )

                .shopId(
                        shop.getId()
                )

                .build();
    }


    // =====================================================
    // LOGIN
    // =====================================================

    @Transactional
    public AuthResponse_Dto login(
            LoginRequest_Dto request
    ) {

        // =================================================
        // FIND USER
        // =================================================

        User_entity user =
                userRepository
                        .findByEmail(
                                request.getEmail()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid email or password"
                                )
                        );


        // =================================================
        // CHECK ACTIVE
        // =================================================

        if (!user.isActive()) {

            throw new RuntimeException(
                    "User account is inactive"
            );
        }


        // =================================================
        // CHECK PASSWORD
        // =================================================

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {

            throw new RuntimeException(
                    "Invalid email or password"
            );
        }


        // =================================================
        // GENERATE JWT
        // =================================================

        String token =
                jwtService.generateToken(
                        user
                );


        // =================================================
        // GET SHOP ID
        // =================================================

        Long shopId = null;

        if (user.getShop() != null) {

            shopId =
                    user.getShop()
                            .getId();
        }


        // =================================================
        // RETURN RESPONSE
        // =================================================

        return AuthResponse_Dto.builder()

                .token(
                        token
                )

                .userId(
                        user.getId()
                )

                .name(
                        user.getName()
                )

                .email(
                        user.getEmail()
                )

                .role(
                        user.getRole()
                )

                .shopId(
                        shopId
                )

                .build();
    }

    
    // =========================================================================
    // PASSWORD RESET WITH CRYPTOGRAPHIC TOKEN HASHING
    // =========================================================================

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    public String hashToken(String rawToken) {
        if (rawToken == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional
    public void forgotPassword(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        Optional<User_entity> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            // Avoid account enumeration: log and return generic success
            log.info("Password reset requested for non-existent email: {}", email.trim());
            return;
        }

        User_entity user = userOpt.get();

        // 1. Generate 32-byte cryptographically secure token (64 hex chars)
        String rawToken = generateSecureToken();

        // 2. Compute SHA-256 hash to store in DB
        String tokenHash = hashToken(rawToken);

        // 3. Invalidate previous token and set 15-minute expiry
        user.setResetTokenHash(tokenHash);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        // 4. Construct reset link using configured frontend URL
        String baseUrl = (frontendBaseUrl != null && !frontendBaseUrl.isBlank())
                ? frontendBaseUrl.replaceAll("/+$", "")
                : "https://xenbill.web.app";
        String resetUrl = baseUrl + "/reset-password?token=" + rawToken;

        // 5. Deliver email via Resend
        boolean sent = emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetUrl);
        if (!sent) {
            log.error("Failed to send password reset email to user id: {}", user.getId());
            throw new RuntimeException("Failed to dispatch password reset email. Please try again later.");
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest_Dto request) {
        if (request == null) {
            throw new RuntimeException("Invalid password reset request");
        }

        String rawToken = request.getToken();
        if (rawToken == null || rawToken.isBlank()) {
            throw new RuntimeException("Password reset token is missing. Please click the reset link sent to your email.");
        }

        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters long.");
        }

        // Hash incoming token to match stored SHA-256 hash
        String tokenHash = hashToken(rawToken.trim());
        User_entity user = userRepository.findByResetTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid or expired password reset link. Please request a new one."));

        if (user.getResetTokenExpiry() == null || LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            throw new RuntimeException("Password reset link has expired. Please request a new reset link.");
        }

        // Hash new password using BCrypt PasswordEncoder
        user.setPassword(passwordEncoder.encode(newPassword));

        // Single-use: immediately invalidate token in DB
        user.setResetTokenHash(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        log.info("Password successfully updated for user id: {}", user.getId());
    }

}