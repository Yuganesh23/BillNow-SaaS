package com.example.Billing.auth;

import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.common.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final userRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final com.example.Billing.notification.EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration OTP_LIFETIME = Duration.ofMinutes(5);
    private static final int MAX_OTP_ATTEMPTS = 5;


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

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(
                normalizedEmail
        )) {

            throw new AppException(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email already registered");
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
                                normalizedEmail
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

                        .email(
                                normalizedEmail
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

                        .trialEndsAt(
                                LocalDateTime.now().plusDays(7)
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

    public AuthResponse_Dto login(
            LoginRequest_Dto request
    ) {

        // =================================================
        // FIND USER
        // =================================================

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User_entity user =
                userRepository
                        .findByEmail(
                                normalizedEmail
                        )
                        .orElseThrow(() ->
                                new AppException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password")
                        );


        // =================================================
        // CHECK ACTIVE
        // =================================================

        if (!user.isActive()) {

            throw new AppException(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", "User account is inactive");
        }


        // =================================================
        // CHECK PASSWORD
        // =================================================

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {

            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
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

    
    @Transactional
    public void forgotPassword(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        String normalizedEmail = email.trim().toLowerCase();
        // Deliberately return the same response for unknown users to prevent account enumeration.
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            passwordResetTokenRepository.findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId())
                    .ifPresent(previous -> {
                        previous.setConsumedAt(Instant.now());
                        passwordResetTokenRepository.save(previous);
                    });
            String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
            Instant now = Instant.now();
            passwordResetTokenRepository.save(PasswordResetToken_entity.builder()
                    .user(user)
                    .codeHash(hashOtp(normalizedEmail, otp))
                    .createdAt(now)
                    .expiresAt(now.plus(OTP_LIFETIME))
                    .build());
            emailService.sendOtpEmail(normalizedEmail, otp);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest_Dto request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User_entity user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(this::invalidOtp);
        PasswordResetToken_entity resetToken = passwordResetTokenRepository
                .findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow(this::invalidOtp);
        Instant now = Instant.now();
        if (!resetToken.getExpiresAt().isAfter(now) || resetToken.getAttempts() >= MAX_OTP_ATTEMPTS) {
            resetToken.setConsumedAt(now);
            passwordResetTokenRepository.save(resetToken);
            throw invalidOtp();
        }
        if (!MessageDigest.isEqual(
                resetToken.getCodeHash().getBytes(StandardCharsets.UTF_8),
                hashOtp(normalizedEmail, request.getOtp()).getBytes(StandardCharsets.UTF_8))) {
            resetToken.setAttempts(resetToken.getAttempts() + 1);
            passwordResetTokenRepository.save(resetToken);
            throw invalidOtp();
        }

        resetToken.setConsumedAt(now);
        passwordResetTokenRepository.save(resetToken);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId()).stream()
                .forEach(token -> token.setRevokedAt(now));
    }

    public AuthResponse_Dto responseFor(User_entity user) {
        Long shopId = user.getShop() == null ? null : user.getShop().getId();
        return AuthResponse_Dto.builder()
                .token(jwtService.generateToken(user))
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .shopId(shopId)
                .build();
    }

    private AppException invalidOtp() {
        return new AppException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "Invalid or expired OTP");
    }

    private String hashOtp(String email, String otp) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((email + ":" + otp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash OTP", exception);
        }
    }

}
