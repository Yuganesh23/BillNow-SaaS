package com.example.Billing.subscription;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/razorpay")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final SaaSSubscriptionService subscriptionService;


    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(
                    value = "X-Razorpay-Signature",
                    required = false
            )
            String signature,

            @RequestBody
            String payload
    ) {

        if (signature == null ||
                signature.isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body("Missing Razorpay signature");
        }


        subscriptionService.processWebhook(
                payload,
                signature
        );


        return ResponseEntity.ok("OK");
    }
}