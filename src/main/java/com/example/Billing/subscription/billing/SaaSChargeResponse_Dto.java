package com.example.Billing.subscription.billing;

import com.example.Billing.subscription.SaaSPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaaSChargeResponse_Dto(
        Long id,
        SaaSPlan plan,
        LocalDateTime paidAt,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        String currency,
        BigDecimal totalAmount,
        BigDecimal taxableAmount,
        BigDecimal taxAmount,
        String invoiceNumber,
        boolean invoiceAvailable
) {
    public static SaaSChargeResponse_Dto from(SaaSBillingCharge_entity charge, boolean sellerConfigured) {
        BigDecimal tax = charge.getTaxableAmount() == null ? null
                : charge.getTotalAmount().subtract(charge.getTaxableAmount());
        return new SaaSChargeResponse_Dto(charge.getId(), charge.getPlan(), charge.getPaidAt(),
                charge.getPeriodStart(), charge.getPeriodEnd(), charge.getCurrency(), charge.getTotalAmount(),
                charge.getTaxableAmount(), tax, charge.getInvoiceNumber(),
                charge.getInvoiceNumber() != null || sellerConfigured);
    }
}
