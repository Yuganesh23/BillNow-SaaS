package com.example.Billing.workflow;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class UpiPaymentLinkServiceTest {
    @Test void createsEncodedInrPaymentUri() {
        String link = new UpiPaymentLinkService().create("shop@upi", "ACME Stores", new BigDecimal("1250.50"), "INV/26/1");
        assertThat(link).isEqualTo("upi://pay?pa=shop%40upi&pn=ACME%20Stores&am=1250.50&cu=INR&tn=Invoice%20INV%2F26%2F1");
    }
}
