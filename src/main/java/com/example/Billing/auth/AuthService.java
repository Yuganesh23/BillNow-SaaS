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
}