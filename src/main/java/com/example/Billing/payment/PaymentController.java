package com.example.Billing.payment;

import com.example.Billing.auth.User_entity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;


    // =====================================================
    // CREATE PAYMENT
    // =====================================================

    @PostMapping
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


    // =====================================================
    // GET PAYMENT BY INVOICE
    // =====================================================

    @GetMapping("/invoice/{invoiceId}")
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