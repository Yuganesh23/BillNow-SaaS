package com.example.Billing.subscription;

import com.example.Billing.auth.SecurityUtils;
import com.example.Billing.subscription.billing.SaaSBillingCharge_entity;
import com.example.Billing.subscription.billing.SaaSBillingService;
import com.example.Billing.subscription.billing.SaaSChargeResponse_Dto;
import com.example.Billing.subscription.billing.SaaSInvoicePdfService;
import com.example.Billing.subscription.dto.ChangePlanRequest_Dto;
import com.example.Billing.subscription.dto.CreateSubscriptionRequest_Dto;
import com.example.Billing.subscription.dto.SubscriptionResponse_Dto;
import com.example.Billing.subscription.dto.VerifySubscriptionRequest_Dto;
import com.example.Billing.auth.User_entity;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SaaSSubscriptionController {

    private final SaaSSubscriptionService subscriptionService;

    private final SecurityUtils securityUtils;
    private final com.example.Billing.config.ShopContextResolver shopContextResolver;
    private final PlanLimitsService planLimitsService;
    private final SaaSBillingService billingService;
    private final SaaSInvoicePdfService invoicePdfService;

    @GetMapping("/usage")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<PlanLimitsService.Usage> usage() {
        User_entity user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(planLimitsService.usage(shopContextResolver.resolveActiveShop(user)));
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


    // =========================================================
    // CHANGE PLAN
    // =========================================================

    @PostMapping("/change-plan")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<SubscriptionResponse_Dto>
    changePlan(
            @Valid
            @RequestBody
            ChangePlanRequest_Dto request
    ) {

        User_entity user =
                securityUtils.getCurrentUser();

        return ResponseEntity.ok(
                subscriptionService.changePlan(
                        user,
                        request
                )
        );
    }

    @PostMapping("/change-plan/cancel")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<SubscriptionResponse_Dto>
    cancelPlanChange() {

        User_entity user =
                securityUtils.getCurrentUser();

        return ResponseEntity.ok(
                subscriptionService
                        .cancelPlanChange(user)
        );
    }

    // =========================================================
    // BILLING HISTORY & TAX INVOICES
    // =========================================================

    @GetMapping("/charges")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<java.util.List<SaaSChargeResponse_Dto>> charges() {
        Long shopId = shopContextResolver.resolveActiveShopId(securityUtils.getCurrentUser());
        boolean sellerConfigured = billingService.isSellerConfigured();
        return ResponseEntity.ok(billingService.history(shopId).stream()
                .map(charge -> SaaSChargeResponse_Dto.from(charge, sellerConfigured))
                .toList());
    }

    @GetMapping("/charges/{chargeId}/invoice")
    @PreAuthorize("hasRole('SHOP_OWNER')")
    public ResponseEntity<byte[]> chargeInvoice(@PathVariable Long chargeId) {
        Long shopId = shopContextResolver.resolveActiveShopId(securityUtils.getCurrentUser());
        SaaSBillingCharge_entity charge = billingService.invoicedCharge(shopId, chargeId);
        byte[] pdf = invoicePdfService.render(charge);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + charge.getInvoiceNumber().replace('/', '-') + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
