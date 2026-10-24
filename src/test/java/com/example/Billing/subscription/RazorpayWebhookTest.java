package com.example.Billing.subscription;

import com.razorpay.RazorpayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RazorpayWebhookTest {

    @Mock
    private SaaSSubscriptionService subscriptionService;

    @InjectMocks
    private RazorpayWebhookController webhookController;

    private String validPayload;

    @BeforeEach
    void setUp() {
        validPayload = "{\"event\":\"subscription.activated\",\"event_id\":\"evt_12345\"}";
    }

    @Test
    void testMissingSignatureReturnsBadRequest() {
        ResponseEntity<String> response = webhookController.handleWebhook(null, "evt_12345", validPayload);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Missing Razorpay signature", response.getBody());
    }

    @Test
    void testBlankSignatureReturnsBadRequest() {
        ResponseEntity<String> response = webhookController.handleWebhook("   ", "evt_12345", validPayload);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Missing Razorpay signature", response.getBody());
    }

    @Test
    void testInvalidSignatureReturnsBadRequest() throws RazorpayException {
        doThrow(new RazorpayException("Signature verification failed"))
                .when(subscriptionService).processWebhook(validPayload, "bad_sig", "evt_12345");

        ResponseEntity<String> response = webhookController.handleWebhook("bad_sig", "evt_12345", validPayload);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid Razorpay webhook signature", response.getBody());
    }

    @Test
    void testValidWebhookReturnsOk() throws RazorpayException {
        doNothing().when(subscriptionService).processWebhook(validPayload, "valid_sig", "evt_12345");

        ResponseEntity<String> response = webhookController.handleWebhook("valid_sig", "evt_12345", validPayload);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody());

        verify(subscriptionService, times(1)).processWebhook(validPayload, "valid_sig", "evt_12345");
    }
}
