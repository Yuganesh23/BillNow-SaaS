package com.example.Billing.shop;

import com.example.Billing.auth.User_entity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/whatsapp")
@RequiredArgsConstructor
public class ShopWhatsAppConfigController {

    private final ShopWhatsAppConfigService configService;


    // =====================================================
    // SAVE / UPDATE WHATSAPP CONFIG
    // =====================================================

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @PostMapping
    public ResponseEntity<ShopWhatsAppConfigResponse_Dto> saveConfig(
            Authentication authentication,
            @Valid @RequestBody ShopWhatsAppConfigRequest_Dto request
    ) {

        User_entity owner =
                getAuthenticatedUser(authentication);


        return ResponseEntity.ok(
                configService.saveConfig(
                        owner,
                        request
                )
        );
    }


    // =====================================================
    // GET WHATSAPP CONFIG
    // =====================================================

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @GetMapping
    public ResponseEntity<ShopWhatsAppConfigResponse_Dto> getConfig(
            Authentication authentication
    ) {

        User_entity owner =
                getAuthenticatedUser(authentication);


        return ResponseEntity.ok(
                configService.getConfig(owner)
        );
    }


    // =====================================================
    // ENABLE / DISABLE
    // =====================================================

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @PatchMapping("/status")
    public ResponseEntity<ShopWhatsAppConfigResponse_Dto> setEnabled(
            Authentication authentication,
            @RequestParam boolean enabled
    ) {

        User_entity owner =
                getAuthenticatedUser(authentication);


        return ResponseEntity.ok(
                configService.setEnabled(
                        owner,
                        enabled
                )
        );
    }


    // =====================================================
    // AUTHENTICATED USER
    // =====================================================

    private User_entity getAuthenticatedUser(
            Authentication authentication
    ) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "User is not authenticated"
            );
        }


        Object principal =
                authentication.getPrincipal();


        if (!(principal instanceof User_entity)) {

            throw new RuntimeException(
                    "Invalid authenticated user"
            );
        }


        return (User_entity) principal;
    }
}