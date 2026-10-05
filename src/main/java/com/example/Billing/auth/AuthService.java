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
                                request.getEmail()
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
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Generate 6 digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        otpStorage.put(email, otp);
        
        emailService.sendOtpEmail(email, otp);
    }

    public void resetPassword(ResetPasswordRequest_Dto request) {
        User_entity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with this email"));
                
        // In this upgraded version, we verify OTP instead of mobile number.
        // We will assume `request.getMobileNumber()` is now actually passing the OTP for simplicity
        String expectedOtp = otpStorage.get(request.getEmail());
        if (expectedOtp == null || !expectedOtp.equals(request.getMobileNumber())) {
            throw new RuntimeException("Invalid or expired OTP");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpStorage.remove(request.getEmail());
    }

}