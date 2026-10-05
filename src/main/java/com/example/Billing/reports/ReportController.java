package com.example.Billing.reports;

import com.example.Billing.auth.User_entity;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;


    // =====================================================
    // SALES REPORT
    // =====================================================

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @GetMapping("/sales")
    public ResponseEntity<SalesReportResponse_Dto> getSalesReport(

            Authentication authentication,

            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to

    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        SalesReportResponse_Dto response =
                reportService.getSalesReport(
                        user,
                        from,
                        to
                );


        return ResponseEntity.ok(response);
    }


    // =====================================================
    // PRODUCT SALES
    // =====================================================
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @GetMapping("/products")
    public ResponseEntity<List<ProductSalesResponse_Dto>>
    getProductSales(

            Authentication authentication,

            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to

    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        return ResponseEntity.ok(
                reportService.getProductSales(
                        user,
                        from,
                        to
                )
        );
    }


    // =====================================================
    // CUSTOMER SALES

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @GetMapping("/customers/new")
    public ResponseEntity<List<CustomerSalesResponse_Dto>> getNewCustomers(
            org.springframework.security.core.Authentication authentication,
            @org.springframework.web.bind.annotation.RequestParam @jakarta.validation.constraints.NotNull @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @org.springframework.web.bind.annotation.RequestParam @jakarta.validation.constraints.NotNull @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate to
    ) {
        com.example.Billing.auth.User_entity user = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(reportService.getNewCustomers(user, from, to));
    }

    // =====================================================


    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @GetMapping("/customers")
    public ResponseEntity<List<CustomerSalesResponse_Dto>>
    getCustomerSales(

            Authentication authentication,

            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to

    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        return ResponseEntity.ok(
                reportService.getCustomerSales(
                        user,
                        from,
                        to
                )
        );
    }


    // =====================================================
    // PAYMENT REPORT
    // =====================================================


    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @GetMapping("/payments")
    public ResponseEntity<PaymentReportResponse_Dto>
    getPaymentReport(

            Authentication authentication,

            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @NotNull
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to

    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        return ResponseEntity.ok(
                reportService.getPaymentReport(
                        user,
                        from,
                        to
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
