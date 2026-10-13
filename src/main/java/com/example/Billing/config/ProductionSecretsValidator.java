package com.example.Billing.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Profile("prod")
public class ProductionSecretsValidator {
    private final Map<String, String> requiredSecrets = new LinkedHashMap<>();

    public ProductionSecretsValidator(
            @Value("${jwt.secret:}") String jwtSecret,
            @Value("${razorpay.key-secret:}") String razorpayKeySecret,
            @Value("${razorpay.webhook-secret:}") String razorpayWebhookSecret,
            @Value("${secrets.encryption-key:}") String integrationEncryptionKey) {
        requiredSecrets.put("JWT_SECRET", jwtSecret);
        requiredSecrets.put("RAZORPAY_KEY_SECRET", razorpayKeySecret);
        requiredSecrets.put("RAZORPAY_WEBHOOK_SECRET", razorpayWebhookSecret);
        requiredSecrets.put("WHATSAPP_TOKEN_ENCRYPTION_KEY", integrationEncryptionKey);
    }

    @PostConstruct
    void validate() {
        String missing = requiredSecrets.entrySet().stream()
                .filter(entry -> entry.getValue() == null || entry.getValue().isBlank())
                .map(Map.Entry::getKey)
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
        if (missing != null) {
            throw new IllegalStateException("Missing required production secrets: " + missing);
        }
    }
}
