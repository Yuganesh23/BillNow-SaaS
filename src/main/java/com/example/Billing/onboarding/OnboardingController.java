package com.example.Billing.onboarding;

import com.example.Billing.auth.User_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {
    private final OnboardingService onboardingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'MANAGER', 'BILLER')")
    public ResponseEntity<OnboardingResponse> status(Authentication authentication) {
        return ResponseEntity.ok(onboardingService.status((User_entity) authentication.getPrincipal()));
    }
}
