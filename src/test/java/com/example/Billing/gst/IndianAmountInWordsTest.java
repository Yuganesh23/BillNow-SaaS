package com.example.Billing.gst;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class IndianAmountInWordsTest {
    @Test
    void usesIndianNumberingAndPaise() {
        assertThat(IndianAmountInWords.rupees(new BigDecimal("12345678.50")))
                .isEqualTo("One Crore Twenty Three Lakh Forty Five Thousand Six Hundred Seventy Eight Rupees and Fifty Paise Only");
    }
}
