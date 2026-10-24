package com.example.Billing.subscription;

import com.example.Billing.auth.User_entity;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.subscription.dto.VerifyOrderRequest_Dto;
import com.razorpay.RazorpayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SaaSSubscriptionServiceTest {

    @Mock
    private SaaSSubscriptionRepository subscriptionRepository;

    @Mock
    private ProcessedWebhookEventRepository processedWebhookEventRepository;

    @InjectMocks
    private SaaSSubscriptionService subscriptionService;

    @Mock
    private ShopRepository shopRepository;

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

    @Test
    void testVerifyUpgradeOrder_InvalidSignatureThrowsException() {
        ReflectionTestUtils.setField(subscriptionService, "razorpayKeySecret", "test_secret");

        User_entity user = User_entity.builder().id(1L).email("owner@test.com").role("SHOP_OWNER").build();
        Shop_entity shop = Shop_entity.builder().id(10L).name("Test Shop").owner(user).build();

        when(shopRepository.findById(10L)).thenReturn(Optional.of(shop));

        VerifyOrderRequest_Dto request = new VerifyOrderRequest_Dto();
        request.setBranchId(10L);
        request.setTier("PRO");
        request.setRazorpayOrderId("order_12345");
        request.setRazorpayPaymentId("pay_12345");
        request.setRazorpaySignature("invalid_signature_hex");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            subscriptionService.verifyUpgradeOrder(user, request);
        });

        assertTrue(ex.getMessage().contains("Invalid payment signature"));
    }
}
