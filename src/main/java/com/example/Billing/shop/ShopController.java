package com.example.Billing.shop;

import com.example.Billing.auth.User_entity;
import com.example.Billing.auth.userRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopRepository shopRepository;
    private final userRepository userRepository;

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @GetMapping
    public ResponseEntity<List<Shop_entity>> getMyShops(Authentication authentication) {
        User_entity userDetails = (User_entity) authentication.getPrincipal();
        return ResponseEntity.ok(shopRepository.findByOwnerId(userDetails.getId()));
    }

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @PostMapping
    @Transactional
    public ResponseEntity<Shop_entity> createShop(
            Authentication authentication,
            @RequestBody ShopCreateRequest request
    ) {
        User_entity userDetails = (User_entity) authentication.getPrincipal();
        User_entity owner = userRepository.findById(userDetails.getId()).orElseThrow();

        Shop_entity newShop = Shop_entity.builder()
                .name(request.getName())
                .invoiceName(request.getInvoiceName())
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .gstin(request.getGstin())
                .legalName(request.getLegalName())
                .state(request.getState())
                .address(request.getAddress())
                .owner(owner)
                .active(true)
                .build();

        return ResponseEntity.ok(shopRepository.save(newShop));
    }

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Shop_entity> updateShop(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody ShopUpdateRequest request
    ) {
        User_entity userDetails = (User_entity) authentication.getPrincipal();
        Shop_entity shop = shopRepository.findById(id).orElseThrow(() -> new RuntimeException("Shop not found"));
        
        if (!shop.getOwner().getId().equals(userDetails.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (request.getName() != null) shop.setName(request.getName());
        if (request.getInvoiceName() != null) shop.setInvoiceName(request.getInvoiceName());
        if (request.getEmail() != null) shop.setEmail(request.getEmail());
        if (request.getMobileNumber() != null) shop.setMobileNumber(request.getMobileNumber());
        if (request.getGstin() != null) shop.setGstin(request.getGstin());
        if (request.getLegalName() != null) shop.setLegalName(request.getLegalName());
        if (request.getState() != null) shop.setState(request.getState());
        if (request.getAddress() != null) shop.setAddress(request.getAddress());
        if (request.getLogoBase64() != null) shop.setLogoBase64(request.getLogoBase64());

        return ResponseEntity.ok(shopRepository.save(shop));
    }

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteShop(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User_entity userDetails = (User_entity) authentication.getPrincipal();
        Shop_entity shop = shopRepository.findById(id).orElseThrow(() -> new RuntimeException("Shop not found"));
        
        if (!shop.getOwner().getId().equals(userDetails.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        try {
            shopRepository.delete(shop);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot delete branch. It contains existing data (invoices, products, etc)."));
        }
    }

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @PostMapping("/{id}/upgrade")
    @org.springframework.transaction.annotation.Transactional
    public org.springframework.http.ResponseEntity<Shop_entity> upgradeSubscription(
            org.springframework.security.core.Authentication authentication,
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> body
    ) {
        com.example.Billing.auth.User_entity userDetails = (com.example.Billing.auth.User_entity) authentication.getPrincipal();
        Shop_entity shop = shopRepository.findById(id).orElseThrow(() -> new RuntimeException("Shop not found"));
        if (!shop.getOwner().getId().equals(userDetails.getId())) throw new RuntimeException("Unauthorized");

        String tierStr = body.get("tier");
        if (tierStr == null) throw new RuntimeException("Tier is required");
        
        SubscriptionTier newTier = SubscriptionTier.valueOf(tierStr.toUpperCase());
        
        shop.setSubscriptionTier(newTier);
        shop.setSubscriptionEndsAt(java.time.LocalDateTime.now().plusDays(30));
        
        return org.springframework.http.ResponseEntity.ok(shopRepository.save(shop));
    }
}

class ErrorResponse {
    private String message;
    public ErrorResponse(String message) { this.message = message; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    
}


@lombok.Data
class ShopUpdateRequest {
    private String name;
    private String invoiceName;
    private String email;
    private String mobileNumber;
    private String gstin;
    private String legalName;
    private String state;
    private String address;
    private String logoBase64;
}

@lombok.Data
class ShopCreateRequest {
    private String name;
    private String invoiceName;
    private String email;
    private String mobileNumber;
    private String gstin;
    private String legalName;
    private String state;
    private String address;
}
