package com.example.Billing.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionSecretsValidatorTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("spring.profiles.active=prod")
            .withUserConfiguration(ProductionSecretsValidator.class);

    @Test
    void productionContextFailsWhenRequiredSecretsAreMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "Missing required production secrets: JWT_SECRET, RAZORPAY_KEY_SECRET, RAZORPAY_WEBHOOK_SECRET, WHATSAPP_TOKEN_ENCRYPTION_KEY");
        });
    }

    @Test
    void productionSecretValidationPassesWhenAllSecretsExist() {
        contextRunner.withPropertyValues(
                "jwt.secret=a-test-jwt-secret-that-is-at-least-32-bytes",
                "razorpay.key-secret=test-key-secret",
                "razorpay.webhook-secret=test-webhook-secret",
                "secrets.encryption-key=test-encryption-key")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
