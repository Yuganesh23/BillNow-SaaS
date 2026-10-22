package com.example.Billing.subscription.billing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SaaSChargeTaxTest {
    private static final BigDecimal RATE = new BigDecimal("18.00");

    @Test
    void intraStateChargeSplitsExtractedTaxIntoEqualHalves() {
        SaaSChargeTax tax = SaaSChargeTax.fromInclusive(new BigDecimal("4999.00"), RATE, true);
        assertThat(tax.taxable()).isEqualByComparingTo("4236.44");
        assertThat(tax.cgst()).isEqualByComparingTo("381.28");
        assertThat(tax.sgst()).isEqualByComparingTo("381.28");
        assertThat(tax.igst()).isEqualByComparingTo("0.00");
    }

    @Test
    void oddPaiseGoToSgstSoTheInvoiceAddsUpExactly() {
        SaaSChargeTax tax = SaaSChargeTax.fromInclusive(new BigDecimal("9999.00"), RATE, true);
        assertThat(tax.taxable()).isEqualByComparingTo("8473.73");
        assertThat(tax.cgst()).isEqualByComparingTo("762.64");
        assertThat(tax.sgst()).isEqualByComparingTo("762.63");
        assertThat(tax.taxable().add(tax.cgst()).add(tax.sgst())).isEqualByComparingTo("9999.00");
    }

    @Test
    void interStateChargeUsesIgst() {
        SaaSChargeTax tax = SaaSChargeTax.fromInclusive(new BigDecimal("24999.00"), RATE, false);
        assertThat(tax.taxable()).isEqualByComparingTo("21185.59");
        assertThat(tax.igst()).isEqualByComparingTo("3813.41");
        assertThat(tax.cgst()).isEqualByComparingTo("0.00");
        assertThat(tax.taxable().add(tax.igst())).isEqualByComparingTo("24999.00");
    }
}
