package com.example.Billing.portal;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers/{customerId}/portal-access")
@RequiredArgsConstructor
public class CustomerPortalAdminController {
    private final CustomerPortalService portalService;

    @PostMapping
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<CustomerPortalService.PortalLink> create(Authentication authentication,
                                                                   @PathVariable Long customerId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.createLink(user(authentication), customerId));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<Void> revoke(Authentication authentication, @PathVariable Long customerId) {
        portalService.revoke(user(authentication), customerId);
        return ResponseEntity.noContent().build();
    }

    private User_entity user(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User_entity user)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Authentication is required");
        }
        return user;
    }
}
