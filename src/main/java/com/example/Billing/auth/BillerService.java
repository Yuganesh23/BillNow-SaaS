package com.example.Billing.auth;

import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.invoice.InvoiceRepository;
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BillerService {

    private final userRepository userRepository;
    private final ShopContextResolver shopContextResolver;
    private final PasswordEncoder passwordEncoder;
    private final InvoiceRepository invoiceRepository;


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
        Shop_entity shop = shopContextResolver.resolveActiveShop(owner);

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

        // 4. Check password
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        if (request.getPassword().length() < 6) {
            throw new RuntimeException("Password must contain at least 6 characters");
        }

        // 5. Create biller
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

    @Transactional
    public BillerResponse_Dto updateBiller(
            User_entity owner,
            Long billerId,
            BillerRequest_Dto request
    ) {

        // 1. Verify owner
        if (!"SHOP_OWNER".equals(owner.getRole())) {
            throw new RuntimeException("Only shop owner can update billers");
        }

        // 2. Get owner's shop
        Shop_entity shop = shopContextResolver.resolveActiveShop(owner);
        if (shop == null) {
            throw new RuntimeException("Owner is not associated with a shop");
        }

        // 3. Find biller
        User_entity biller = userRepository.findById(billerId)
                .orElseThrow(() -> new RuntimeException("Biller not found"));

        if (biller.getShop() == null || !biller.getShop().getId().equals(shop.getId())) {
            throw new RuntimeException("Biller does not belong to your shop");
        }

        if (!"BILLER".equals(biller.getRole())) {
            throw new RuntimeException("User is not a biller");
        }

        // 4. Check email uniqueness if changed
        String newEmail = request.getEmail().trim().toLowerCase();
        if (!biller.getEmail().equals(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("Email already registered");
        }

        // 5. Update
        biller.setName(request.getName().trim());
        biller.setEmail(newEmail);
        
        // Only update password if provided and not empty
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            if (request.getPassword().length() < 6) {
                throw new RuntimeException("Password must contain at least 6 characters");
            }
            biller.setPassword(passwordEncoder.encode(request.getPassword()));
        }

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
        Shop_entity shop = shopContextResolver.resolveActiveShop(owner);

        if (shop == null) {
            throw new RuntimeException(
                    "Owner is not associated with a shop"
            );
        }

        // 3. Get billers
        java.util.List<BillerResponse_Dto> responses = new java.util.ArrayList<>();
        
        // Add owner to top of the list
        responses.add(mapToResponse(shop.getOwner(), shop));
        
        // Add all regular billers
        userRepository.findByShopIdAndRole(shop.getId(), "BILLER").stream()
                .map(biller -> mapToResponse(biller, shop))
                .forEach(responses::add);
                
        return responses;
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
        Shop_entity shop = shopContextResolver.resolveActiveShop(owner);

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
        Shop_entity shop = shopContextResolver.resolveActiveShop(owner);

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
        long totalInvoices = 0;
        BigDecimal totalSales = BigDecimal.ZERO;
        
        try {
            if (invoiceRepository != null) {
                totalInvoices = invoiceRepository.countByBillerIdAndShopId(biller.getId(), shop.getId());
                totalSales = invoiceRepository.sumTotalAmountByBillerIdAndShopId(biller.getId(), shop.getId());
                if (totalSales == null) totalSales = BigDecimal.ZERO;
            }
        } catch (Exception e) {}

        return BillerResponse_Dto.builder()
                .id(biller.getId())
                .shopId(shop.getId())
                .name(biller.getName())
                .email(biller.getEmail())
                .mobileNumber(biller.getMobileNumber())
                .role(biller.getRole())
                .active(biller.isActive())
                .totalInvoices(totalInvoices)
                .totalSales(totalSales)
                .build();
    }
}
