package com.example.Billing.subscription;

import com.example.Billing.auth.SecurityUtils;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.subscription.dto.CreateOrderRequest_Dto;
import com.example.Billing.subscription.dto.CreateSubscriptionRequest_Dto;
import com.example.Billing.subscription.dto.SubscriptionResponse_Dto;
import com.example.Billing.subscription.dto.VerifyOrderRequest_Dto;
import com.example.Billing.subscription.dto.VerifySubscriptionRequest_Dto;
import com.example.Billing.auth.User_entity;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SaaSSubscriptionController {

    private final SaaSSubscriptionService subscriptionService;

    private final SecurityUtils securityUtils;


    // =========================================================
    // CREATE RAZORPAY ORDER FOR PLAN UPGRADE CHECKOUT
    // =========================================================

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<Map<String, Object>> createUpgradeOrder(
            @Valid
            @RequestBody
            CreateOrderRequest_Dto request
    ) {
        User_entity user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(subscriptionService.createUpgradeOrder(user, request));
    }


    // =========================================================
    // VERIFY RAZORPAY PAYMENT & UPGRADE SHOP
    // =========================================================

    @PostMapping("/verify-order")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<Shop_entity> verifyUpgradeOrder(
            @Valid
            @RequestBody
            VerifyOrderRequest_Dto request
    ) {
        User_entity user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(subscriptionService.verifyUpgradeOrder(user, request));
    }


    // =========================================================
    // CREATE
    // =========================================================

    @PostMapping("/create")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<SubscriptionResponse_Dto>
    createSubscription(
            @Valid
            @RequestBody
            CreateSubscriptionRequest_Dto request
    ) {

        User_entity user =
                securityUtils.getCurrentUser();

        return ResponseEntity.ok(
                subscriptionService.createSubscription(
                        user,
                        request
                )
        );
    }


    // =========================================================
    // VERIFY
    // =========================================================

    @PostMapping("/verify")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<SubscriptionResponse_Dto>
    verifyPayment(
            @Valid
            @RequestBody
            VerifySubscriptionRequest_Dto request
    ) {

        User_entity user =
                securityUtils.getCurrentUser();

        return ResponseEntity.ok(
                subscriptionService.verifyPayment(
                        user,
                        request
                )
        );
    }


    // =========================================================
    // CURRENT
    // =========================================================

    @GetMapping
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<SubscriptionResponse_Dto>
    getCurrentSubscription() {

        User_entity user =
                securityUtils.getCurrentUser();

        return ResponseEntity.ok(
                subscriptionService
                        .getCurrentSubscription(user)
        );
    }


    // =========================================================
    // CANCEL
    // =========================================================

    @PostMapping("/cancel")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<SubscriptionResponse_Dto>
    cancelSubscription() {

        User_entity user =
                securityUtils.getCurrentUser();

        return ResponseEntity.ok(
                subscriptionService
                        .cancelSubscription(user)
        );
    }
}