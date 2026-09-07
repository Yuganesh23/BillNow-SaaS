package com.example.Billing.customer;

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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;


    // =========================================================
    // CREATE CUSTOMER
    // OWNER + BILLER
    // =========================================================

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @PostMapping
    public ResponseEntity<CustomerResponse_Dto> createCustomer(
            Authentication authentication,
            @Valid @RequestBody CustomerRequest_Dto request
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);

        CustomerResponse_Dto response =
                customerService.createCustomer(
                        user,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =========================================================
    // GET ALL CUSTOMERS
    // OWNER + BILLER
    // =========================================================

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @GetMapping
    public ResponseEntity<List<CustomerResponse_Dto>> getAllCustomers(
            Authentication authentication
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);

        List<CustomerResponse_Dto> customers =
                customerService.getAllCustomers(user);

        return ResponseEntity.ok(customers);
    }


    // =========================================================
    // GET CUSTOMER BY ID
    // OWNER + BILLER
    // =========================================================

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse_Dto> getCustomer(
            Authentication authentication,
            @PathVariable Long customerId
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);

        CustomerResponse_Dto customer =
                customerService.getCustomer(
                        user,
                        customerId
                );

        return ResponseEntity.ok(customer);
    }


    // =========================================================
    // UPDATE CUSTOMER
    // OWNER + BILLER
    // =========================================================

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponse_Dto> updateCustomer(
            Authentication authentication,
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerRequest_Dto request
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);

        CustomerResponse_Dto updatedCustomer =
                customerService.updateCustomer(
                        user,
                        customerId,
                        request
                );

        return ResponseEntity.ok(updatedCustomer);
    }


    // =========================================================
    // DELETE CUSTOMER
    // OWNER ONLY
    // =========================================================

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(
            Authentication authentication,
            @PathVariable Long customerId
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);

        customerService.deleteCustomer(
                user,
                customerId
        );

        return ResponseEntity
                .noContent()
                .build();
    }


    // =========================================================
    // GET AUTHENTICATED USER
    // =========================================================

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
