package com.example.Billing.product;

import com.example.Billing.auth.User_entity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;


    // =====================================================
    // CREATE PRODUCT
    // POST /api/products
    // =====================================================
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @PostMapping
    public ResponseEntity<ProductResponse_Dto> createProduct(
            Authentication authentication,
            @Valid @RequestBody ProductRequest_Dto request
    ) {

        User_entity user = getAuthenticatedUser(authentication);

        ProductResponse_Dto response =
                productService.createProduct(user, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =====================================================
    // GET ALL PRODUCTS
    // GET /api/products
    // =====================================================
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @GetMapping
    public ResponseEntity<List<ProductResponse_Dto>> getAllProducts(
            Authentication authentication
    ) {

        System.out.println("========== GET ALL PRODUCTS ==========");

        User_entity user =
                getAuthenticatedUser(authentication);

        System.out.println("Authenticated user: " + user.getEmail());

        if (user.getShop() == null) {
            throw new RuntimeException(
                    "User is not assigned to a shop"
            );
        }

        System.out.println(
                "Shop ID: " + user.getShop().getId()
        );

        List<ProductResponse_Dto> products =
                productService.getAllProducts(user);

        System.out.println(
                "Products found: " + products.size()
        );

        return ResponseEntity.ok(products);
    }


    // =====================================================
    // GET PRODUCT BY ID
    // GET /api/products/{productId}
    // =====================================================
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse_Dto> getProduct(
            Authentication authentication,
            @PathVariable Long productId
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);

        ProductResponse_Dto product =
                productService.getProduct(
                        user,
                        productId
                );

        return ResponseEntity.ok(product);
    }


    // =====================================================
    // UPDATE PRODUCT
    // PUT /api/products/{productId}
    // =====================================================
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse_Dto> updateProduct(
            Authentication authentication,
            @PathVariable Long productId,
            @Valid @RequestBody ProductRequest_Dto request
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);

        ProductResponse_Dto updatedProduct =
                productService.updateProduct(
                        user,
                        productId,
                        request
                );

        return ResponseEntity.ok(updatedProduct);
    }


    // =====================================================
    // DELETE PRODUCT
    // DELETE /api/products/{productId}
    // =====================================================
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            Authentication authentication,
            @PathVariable Long productId
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);

        productService.deleteProduct(
                user,
                productId
        );

        return ResponseEntity.noContent().build();
    }


    // =====================================================
    // RESTORE PRODUCT
    // PUT /api/products/{productId}/restore
    // =====================================================
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @PutMapping("/{productId}/restore")
    public ResponseEntity<ProductResponse_Dto> restoreProduct(
            Authentication authentication,
            @PathVariable Long productId
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);

        ProductResponse_Dto product =
                productService.restoreProduct(
                        user,
                        productId
                );

        return ResponseEntity.ok(product);
    }


    // =====================================================
    // GET AUTHENTICATED USER
    // =====================================================
    private User_entity getAuthenticatedUser(
            Authentication authentication
    ) {

        if (authentication == null) {

            throw new RuntimeException(
                    "Authentication is null"
            );
        }

        if (!authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "User is not authenticated"
            );
        }

        Object principal =
                authentication.getPrincipal();

        if (!(principal instanceof User_entity)) {

            throw new RuntimeException(
                    "Invalid authenticated user: "
                            + principal.getClass().getName()
            );
        }

        return (User_entity) principal;
    }
}
