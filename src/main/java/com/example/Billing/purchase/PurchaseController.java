package com.example.Billing.purchase;

import com.example.Billing.auth.User_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    public ResponseEntity<List<PurchaseResponse_Dto>> getAllPurchases(Authentication authentication) {
        return ResponseEntity.ok(purchaseService.getAllPurchases((User_entity) authentication.getPrincipal()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponse_Dto> getPurchaseById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(purchaseService.getPurchaseById((User_entity) authentication.getPrincipal(), id));
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse_Dto> createPurchase(@RequestBody PurchaseRequest_Dto request, Authentication authentication) {
        return ResponseEntity.ok(purchaseService.createPurchase((User_entity) authentication.getPrincipal(), request));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<PurchaseResponse_Dto> recordPayment(
            @PathVariable Long id, 
            @RequestParam java.math.BigDecimal amount,
            @RequestParam(required = false, defaultValue = "CASH") String paymentMethod,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        return ResponseEntity.ok(purchaseService.recordPayment((User_entity) authentication.getPrincipal(), id, amount, paymentMethod, notes));
    }
}
