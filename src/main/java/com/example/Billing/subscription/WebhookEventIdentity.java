package com.example.Billing.subscription;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class WebhookEventIdentity {
    private WebhookEventIdentity() {}

    static String resolve(JSONObject event, String payload) {
        String providerId = event.optString("id", "").trim();
        return providerId.isBlank() ? "sha256:" + hash(payload) : providerId;
    }

    static String hash(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
