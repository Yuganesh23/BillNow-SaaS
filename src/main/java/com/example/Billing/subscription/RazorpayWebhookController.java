package com.example.Billing.subscription;

import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/razorpay")
@RequiredArgsConstructor
@Slf4j
public class RazorpayWebhookController {

    private final SaaSSubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(
                    value = "X-Razorpay-Signature",
                    required = false
            )
            String signature,

            @RequestHeader(
                    value = "X-Razorpay-Event-Id",
                    required = false
            )
            String eventId,

            @RequestBody
            String payload
    ) {

        if (signature == null || signature.isBlank()) {
            log.warn("Rejected Razorpay webhook: missing X-Razorpay-Signature");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Missing Razorpay signature");
        }

        try {
            subscriptionService.processWebhook(
                    payload,
                    signature,
                    eventId
            );
            return ResponseEntity.ok("OK");
        } catch (RazorpayException e) {
            log.error("Invalid Razorpay webhook signature: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid Razorpay webhook signature");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Razorpay webhook validation error: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Webhook processing failed");
        }
    }
}