package com.example.Billing.auth;

import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BillerService {

    private final userRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    // =========================================================
    // CREATE BILLER
    // =========================================================

    @Transactional
    public BillerResponse_Dto createBiller(
            User_entity owner,
            BillerRequest_Dto request
    ) {

        // 1. Verify owner role
        if (!"SHOP_OWNER".equals(owner.getRole())) {
            throw new RuntimeException(
                    "Only shop owner can create billers"
            );
        }

        // 2. Verify owner has shop
        Shop_entity shop = owner.getShop();

        if (shop == null) {
            throw new RuntimeException(
                    "Owner is not associated with a shop"
            );
        }

        // 3. Check email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException(
                    "Email already registered"
            );
        }

        // 4. Create biller
        User_entity biller = User_entity.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("BILLER")
                .active(true)
                .shop(shop)
                .build();

        // 5. Save
        User_entity savedBiller = userRepository.save(biller);

        // 6. Response
        return mapToResponse(savedBiller, shop);
    }


    // =========================================================
    // GET ALL BILLERS
    // =========================================================

    @Transactional(readOnly = true)
    public List<BillerResponse_Dto> getBillers(
            User_entity owner
    ) {

        // 1. Verify owner
        if (!"SHOP_OWNER".equals(owner.getRole())) {
            throw new RuntimeException(
                    "Only shop owner can view billers"
            );
        }

        // 2. Get shop
        Shop_entity shop = owner.getShop();

        if (shop == null) {
            throw new RuntimeException(
                    "Owner is not associated with a shop"
            );
        }

        // 3. Get billers
        return userRepository
                .findByShopIdAndRole(
                        shop.getId(),
                        "BILLER"
                )
                .stream()
                .map(biller -> mapToResponse(biller, shop))
                .toList();
    }


    // =========================================================
    // DEACTIVATE BILLER
    // =========================================================

    @Transactional
    public BillerResponse_Dto deactivateBiller(
            User_entity owner,
            Long billerId
    ) {

        // 1. Verify owner
        if (!"SHOP_OWNER".equals(owner.getRole())) {
            throw new RuntimeException(
                    "Only shop owner can deactivate billers"
            );
        }

        // 2. Get owner's shop
        Shop_entity shop = owner.getShop();

        if (shop == null) {
            throw new RuntimeException(
                    "Owner is not associated with a shop"
            );
        }

        // 3. Find biller
        User_entity biller = userRepository
                .findById(billerId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Biller not found"
                        )
                );

        // 4. Make sure biller belongs to owner's shop
        if (biller.getShop() == null ||
                !biller.getShop()
                        .getId()
                        .equals(shop.getId())) {

            throw new RuntimeException(
                    "Biller does not belong to your shop"
            );
        }

        // 5. Verify role
        if (!"BILLER".equals(biller.getRole())) {
            throw new RuntimeException(
                    "User is not a biller"
            );
        }

        // 6. Deactivate
        biller.setActive(false);

        User_entity savedBiller =
                userRepository.save(biller);

        // 7. Response
        return mapToResponse(savedBiller, shop);
    }


    // =========================================================
    // ACTIVATE BILLER
    // =========================================================

    @Transactional
    public BillerResponse_Dto activateBiller(
            User_entity owner,
            Long billerId
    ) {

        // 1. Verify owner
        if (!"SHOP_OWNER".equals(owner.getRole())) {
            throw new RuntimeException(
                    "Only shop owner can activate billers"
            );
        }

        // 2. Get shop
        Shop_entity shop = owner.getShop();

        if (shop == null) {
            throw new RuntimeException(
                    "Owner is not associated with a shop"
            );
        }

        // 3. Find biller
        User_entity biller = userRepository
                .findById(billerId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Biller not found"
                        )
                );

        // 4. Check shop ownership
        if (biller.getShop() == null ||
                !biller.getShop()
                        .getId()
                        .equals(shop.getId())) {

            throw new RuntimeException(
                    "Biller does not belong to your shop"
            );
        }

        // 5. Verify role
        if (!"BILLER".equals(biller.getRole())) {
            throw new RuntimeException(
                    "User is not a biller"
            );
        }

        // 6. Activate
        biller.setActive(true);

        User_entity savedBiller =
                userRepository.save(biller);

        return mapToResponse(savedBiller, shop);
    }


    // =========================================================
    // MAPPER
    // =========================================================

    private BillerResponse_Dto mapToResponse(
            User_entity biller,
            Shop_entity shop
    ) {

        return BillerResponse_Dto.builder()
                .id(biller.getId())
                .shopId(shop.getId())
                .name(biller.getName())
                .email(biller.getEmail())
                .role(biller.getRole())
                .active(biller.isActive())
                .build();
    }
}