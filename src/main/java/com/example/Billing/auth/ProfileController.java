package com.example.Billing.auth;

import com.example.Billing.shop.Shop_entity;
import com.example.Billing.shop.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    @org.springframework.web.bind.annotation.GetMapping
    public ResponseEntity<?> getProfile(Authentication authentication) {
        User_entity userDetails = (User_entity) authentication.getPrincipal();
        String currentEmail = userDetails.getEmail();
        User_entity user = userRepository.findByEmail(currentEmail).orElseThrow();
        return ResponseEntity.ok(user);
    }


    private final userRepository userRepository;
    private final ShopRepository shopRepository;

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @PutMapping
    @Transactional
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @RequestBody ProfileUpdateRequest request
    ) {
        User_entity userDetails = (User_entity) authentication.getPrincipal();
        String currentEmail = userDetails.getEmail();
        User_entity user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update User
        if (request.getUserName() != null) user.setName(request.getUserName());
        if (request.getUserEmail() != null && !request.getUserEmail().isEmpty()) user.setEmail(request.getUserEmail());
        if (request.getUserMobile() != null) user.setMobileNumber(request.getUserMobile());
        // We might not want to let them change email to avoid breaking JWT flow for now,
        // but if we do, they have to login again.
        
        // Update Shop
        Shop_entity shop = user.getShop();
        if (shop == null && user.getRole().equals("ROLE_SHOP_OWNER")) {
            // Find shop by owner
            shop = shopRepository.findByOwnerId(user.getId()).stream().findFirst().orElse(null);
        }
        
        if (shop != null && user.getRole().equals("ROLE_SHOP_OWNER")) {
            if (request.getShopName() != null) shop.setName(request.getShopName());
            if (request.getShopMobile() != null) shop.setMobileNumber(request.getShopMobile());
            if (request.getShopAddress() != null) shop.setAddress(request.getShopAddress());
            if (request.getShopEmail() != null) shop.setEmail(request.getShopEmail());
            shopRepository.save(shop);
        }
        
        userRepository.save(user);

        return ResponseEntity.ok(new ProfileUpdateResponse("Profile updated successfully"));
    }
}

@lombok.Data
class ProfileUpdateRequest {
    private String userName;
    private String userEmail;
    private String userMobile;
    private String shopName;
    private String shopEmail;
    private String shopMobile;
    private String shopAddress;
}

@lombok.Data
@lombok.AllArgsConstructor
class ProfileUpdateResponse {
    private String message;
}
