package com.example.Billing.payment;

import com.example.Billing.auth.User_entity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentReceiptPdfService receiptPdfService;


    // =====================================================
    // CREATE PAYMENT
    // =====================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('SHOP_OWNER','MANAGER','BILLER')")
    public ResponseEntity<PaymentResponse_Dto> createPayment(

            Authentication authentication,

            @Valid
            @RequestBody
            PaymentRequest_Dto request

    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        PaymentResponse_Dto response =
                paymentService.createPayment(
                        user,
                        request
                );


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{paymentId}/receipt")
    @PreAuthorize("hasAnyRole('SHOP_OWNER','MANAGER','BILLER','VIEWER')")
    public ResponseEntity<byte[]> receipt(Authentication authentication, @PathVariable Long paymentId) {
        byte[] pdf = receiptPdfService.generate(getAuthenticatedUser(authentication), paymentId);
        return ResponseEntity.ok().header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=payment-" + paymentId + ".pdf").contentType(org.springframework.http.MediaType.APPLICATION_PDF).body(pdf);
    }


    // =====================================================
    // GET PAYMENT BY INVOICE
    // =====================================================

    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize("hasAnyRole('SHOP_OWNER','MANAGER','BILLER','VIEWER')")
    public ResponseEntity<PaymentResponse_Dto>
    getPaymentByInvoice(

            Authentication authentication,

            @PathVariable
            Long invoiceId

    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        PaymentResponse_Dto response =
                paymentService.getPaymentByInvoice(
                        user,
                        invoiceId
                );


        return ResponseEntity.ok(response);
    }


    // =====================================================
    // GET AUTHENTICATED USER
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
