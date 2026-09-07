package com.example.Billing.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billers")
@RequiredArgsConstructor
public class BillerController {

    private final BillerService billerService;

    // =========================
    // CREATE BILLER
    // =========================

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @PostMapping
    public ResponseEntity<BillerResponse_Dto> createBiller(
            @Valid @RequestBody BillerRequest_Dto request,
            Authentication authentication
    ) {

        User_entity owner =
                (User_entity) authentication.getPrincipal();

        BillerResponse_Dto response =
                billerService.createBiller(owner, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================
    // GET BILLERS
    // =========================

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @GetMapping
    public ResponseEntity<List<BillerResponse_Dto>> getBillers(
            Authentication authentication
    ) {

        User_entity owner =
                (User_entity) authentication.getPrincipal();

        return ResponseEntity.ok(
                billerService.getBillers(owner)
        );
    }

    // =========================
    // DEACTIVATE BILLER
    // =========================
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<BillerResponse_Dto> deactivateBiller(
            @PathVariable Long id,
            Authentication authentication
    ) {

        User_entity owner =
                (User_entity) authentication.getPrincipal();

        return ResponseEntity.ok(
                billerService.deactivateBiller(
                        owner,
                        id
                )
        );
    }
}