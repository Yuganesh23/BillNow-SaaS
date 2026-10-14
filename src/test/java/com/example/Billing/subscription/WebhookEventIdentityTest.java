package com.example.Billing.subscription;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookEventIdentityTest {
    @Test
    void providerEventIdWinsAndPayloadHashFallbackIsStable() throws Exception {
        assertThat(WebhookEventIdentity.resolve(new JSONObject("{\"id\":\"evt_123\"}"), "payload"))
                .isEqualTo("evt_123");
        String first = WebhookEventIdentity.resolve(new JSONObject("{}"), "same payload");
        String replay = WebhookEventIdentity.resolve(new JSONObject("{}"), "same payload");
        assertThat(first).isEqualTo(replay).startsWith("sha256:").hasSize(71);
        assertThat(WebhookEventIdentity.resolve(new JSONObject("{}"), "different payload")).isNotEqualTo(first);
    }
}
