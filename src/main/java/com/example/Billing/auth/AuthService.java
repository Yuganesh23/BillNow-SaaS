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

    
    public void forgotPassword(String email) {
        User_entity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        // Generate 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        otpStorage.put(email, otp);
        
        String resetUrl = "https://xenbill.web.app/forgot-password?email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8) + "&code=" + otp;
        emailService.sendPasswordResetEmail(email, user.getName(), resetUrl, otp);
    }

    public void resetPassword(ResetPasswordRequest_Dto request) {
        User_entity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with this email"));
                
        String expectedOtp = otpStorage.get(request.getEmail());
        String code = request.getToken();
        if (code == null || code.isBlank()) {
            code = request.getMobileNumber();
        }

        boolean valid = false;
        if (expectedOtp != null && expectedOtp.equals(code)) {
            valid = true;
        } else if (user.getMobileNumber() != null && code != null) {
            String dbDigits = user.getMobileNumber().replaceAll("\\D", "");
            String inputDigits = code.replaceAll("\\D", "");
            if (!inputDigits.isEmpty()) {
                String dbLast10 = dbDigits.length() >= 10 ? dbDigits.substring(dbDigits.length() - 10) : dbDigits;
                String inputLast10 = inputDigits.length() >= 10 ? inputDigits.substring(inputDigits.length() - 10) : inputDigits;
                if (!dbLast10.isEmpty() && dbLast10.equals(inputLast10)) {
                    valid = true;
                }
            }
        }

        if (!valid) {
            throw new RuntimeException("Invalid verification code or mobile number. Please check your details or request a new reset code.");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpStorage.remove(request.getEmail());
    }

}