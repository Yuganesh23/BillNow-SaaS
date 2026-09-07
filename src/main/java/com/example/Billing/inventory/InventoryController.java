package com.example.Billing.inventory;

import com.example.Billing.auth.User_entity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;


    // =====================================================
    // ADJUST STOCK
    // =====================================================
    @PreAuthorize("hasRole('SHOP_OWNER')")
    @PostMapping("/adjust")
    public ResponseEntity<InventoryResponse_Dto>
    adjustStock(

            Authentication authentication,

            @Valid
            @RequestBody
            StockAdjustmentRequest_Dto request

    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        InventoryResponse_Dto response =
                inventoryService.adjustStock(
                        user,
                        request
                );


        return ResponseEntity.ok(response);
    }


    // =====================================================
    // GET ALL INVENTORY
    // =====================================================

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @GetMapping
    public ResponseEntity<List<InventoryResponse_Dto>>
    getInventory(
            Authentication authentication
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        return ResponseEntity.ok(
                inventoryService.getInventory(user)
        );
    }


    // =====================================================
    // GET PRODUCT INVENTORY
    // =====================================================

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponse_Dto>
    getProductInventory(

            Authentication authentication,

            @PathVariable
            Long productId

    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        return ResponseEntity.ok(
                inventoryService.getProductInventory(
                        user,
                        productId
                )
        );
    }


    // =====================================================
    // GET LOW STOCK
    // =====================================================

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse_Dto>>
    getLowStockProducts(
            Authentication authentication
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        return ResponseEntity.ok(
                inventoryService.getLowStockProducts(
                        user
                )
        );
    }


    // =====================================================
    // GET ALL STOCK MOVEMENTS
    // =====================================================

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @GetMapping("/movements")
    public ResponseEntity<List<StockMovementResponse_Dto>>
    getStockMovements(
            Authentication authentication
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        return ResponseEntity.ok(
                inventoryService.getStockMovements(
                        user
                )
        );
    }


    // =====================================================
    // GET PRODUCT STOCK MOVEMENTS
    // =====================================================

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @GetMapping("/product/{productId}/movements")
    public ResponseEntity<List<StockMovementResponse_Dto>>
    getProductStockMovements(

            Authentication authentication,

            @PathVariable
            Long productId

    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        return ResponseEntity.ok(
                inventoryService.getProductStockMovements(
                        user,
                        productId
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