package com.example.Billing.auth;

import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

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

    
    @Transactional
    public void forgotPassword(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email address is required");
        }

        User_entity user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("No account registered with email: " + email));

        // Generate cryptographically secure token and save in database
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        user.setResetToken(token);
        user.setResetTokenExpiry(java.time.LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        String resetUrl = "https://xenbill.web.app/reset-password?token=" + token;
        boolean sent = emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetUrl);
        if (!sent) {
            throw new RuntimeException("Failed to dispatch email via Resend. Please ensure RESEND_API_KEY is configured on Cloud Run.");
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest_Dto request) {
        String token = request.getToken();
        if (token == null || token.isBlank()) {
            // Also support token passed in mobileNumber field from legacy forms
            token = request.getMobileNumber();
        }

        if (token == null || token.isBlank()) {
            throw new RuntimeException("Password reset token is missing. Please click the reset link sent to your email.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters long");
        }

        User_entity user = userRepository.findByResetToken(token.trim())
                .orElseThrow(() -> new RuntimeException("Invalid or expired password reset link. Please request a new one."));

        if (user.getResetTokenExpiry() == null || java.time.LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            throw new RuntimeException("Password reset link has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

}