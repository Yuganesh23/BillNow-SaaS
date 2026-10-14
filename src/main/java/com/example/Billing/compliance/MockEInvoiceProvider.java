package com.example.Billing.compliance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
@ConditionalOnProperty(name = "compliance.provider", havingValue = "mock", matchIfMissing = true)
public class MockEInvoiceProvider implements EInvoiceProvider {
    private final AtomicReference<String> nextEInvoiceFailure = new AtomicReference<>();
    private final AtomicReference<String> nextEWayBillFailure = new AtomicReference<>();

    @Override public Result generate(String idempotencyKey, Map<String, Object> payload) {
        failIfRequested(nextEInvoiceFailure);
        String irn = sha256(idempotencyKey);
        LocalDateTime now = LocalDateTime.now();
        return new Result("MOCK-" + irn.substring(0, 12), irn, "MOCK" + irn.substring(0, 16), now,
                "MOCK-SIGNED-QR:" + irn, Map.of("mode", "MOCK", "irn", irn, "acknowledgedAt", now.toString()));
    }

    @Override public Cancellation cancel(String irn, String reason) {
        return new Cancellation(LocalDateTime.now(), Map.of("mode", "MOCK", "irn", irn, "cancelled", true));
    }

    @Override public EWayBillResult generateEWayBill(String idempotencyKey, Map<String, Object> payload) {
        failIfRequested(nextEWayBillFailure);
        String hash = sha256(idempotencyKey);
        LocalDateTime now = LocalDateTime.now();
        String number = String.format("9%011d", new BigInteger(hash, 16).mod(BigInteger.valueOf(100_000_000_000L)));
        return new EWayBillResult(number, now, now.plusDays(1),
                Map.of("mode", "MOCK", "ewayBillNo", number, "generatedAt", now.toString()));
    }

    @Override public EWayBillResult updateEWayBillVehicle(String eWayBillNumber, Map<String, Object> payload) {
        LocalDateTime now = LocalDateTime.now();
        return new EWayBillResult(eWayBillNumber, now, now.plusDays(1),
                Map.of("mode", "MOCK", "ewayBillNo", eWayBillNumber, "vehicleUpdated", true));
    }

    @Override public Cancellation cancelEWayBill(String eWayBillNumber, String reason) {
        return new Cancellation(LocalDateTime.now(),
                Map.of("mode", "MOCK", "ewayBillNo", eWayBillNumber, "cancelled", true));
    }

    private String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))).toUpperCase(); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }

    void failNextEInvoice(String message) { nextEInvoiceFailure.set(message); }
    void failNextEWayBill(String message) { nextEWayBillFailure.set(message); }
    private void failIfRequested(AtomicReference<String> requestedFailure) {
        String message = requestedFailure.getAndSet(null);
        if (message != null) throw new IllegalStateException(message);
    }
}
