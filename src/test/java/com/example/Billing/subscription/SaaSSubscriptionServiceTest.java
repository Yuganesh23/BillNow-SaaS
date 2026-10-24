package com.example.Billing.subscription;

import com.razorpay.RazorpayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SaaSSubscriptionServiceTest {

    @Mock
    private SaaSSubscriptionRepository subscriptionRepository;

    @Mock
    private ProcessedWebhookEventRepository processedWebhookEventRepository;

    @InjectMocks
    private SaaSSubscriptionService subscriptionService;

    @Test
    void testMissingWebhookSignatureThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            subscriptionService.processWebhook("{}", null, "evt_123");
        });
    }

    @Test
    void testMissingWebhookSecretThrowsIllegalStateException() {
        ReflectionTestUtils.setField(subscriptionService, "webhookSecret", "");
        assertThrows(IllegalStateException.class, () -> {
            subscriptionService.processWebhook("{}", "some_sig", "evt_123");
        });
    }
}
