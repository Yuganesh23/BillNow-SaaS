package com.example.Billing.compliance;

import java.time.LocalDateTime;
import java.util.Map;

public interface EInvoiceProvider {
    Result generate(String idempotencyKey, Map<String, Object> payload);
    Cancellation cancel(String irn, String reason);
    EWayBillResult generateEWayBill(String idempotencyKey, Map<String, Object> payload);
    EWayBillResult updateEWayBillVehicle(String eWayBillNumber, Map<String, Object> payload);
    Cancellation cancelEWayBill(String eWayBillNumber, String reason);

    record Result(String providerReference, String irn, String acknowledgementNumber,
                  LocalDateTime acknowledgementDate, String signedQrData, Map<String, Object> rawResponse) {}
    record Cancellation(LocalDateTime cancelledAt, Map<String, Object> rawResponse) {}
    record EWayBillResult(String eWayBillNumber, LocalDateTime generatedAt, LocalDateTime validUntil,
                          Map<String, Object> rawResponse) {}
}
