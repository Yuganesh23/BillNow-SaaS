package com.example.Billing.subscription.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Splits a GST-inclusive subscription charge. The amount Razorpay collected is
 * treated as the gross value; the taxable value is extracted from it.
 */
public record SaaSChargeTax(BigDecimal taxable, BigDecimal cgst, BigDecimal sgst, BigDecimal igst) {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public static SaaSChargeTax fromInclusive(BigDecimal gross, BigDecimal ratePercent, boolean intraState) {
        BigDecimal total = gross.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxable = total.multiply(HUNDRED).divide(HUNDRED.add(ratePercent), 2, RoundingMode.HALF_UP);
        BigDecimal tax = total.subtract(taxable);
        if (!intraState) return new SaaSChargeTax(taxable, zero(), zero(), tax);
        BigDecimal cgst = tax.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        return new SaaSChargeTax(taxable, cgst, tax.subtract(cgst), zero());
    }

    private static BigDecimal zero() { return BigDecimal.ZERO.setScale(2); }
}
