package com.example.Billing.invoice;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentNumberServiceTest {
    @Test
    void financialYearResetsInApril() {
        assertThat(DocumentNumberService.financialYear(LocalDate.of(2026, 3, 31))).isEqualTo("2025-26");
        assertThat(DocumentNumberService.financialYear(LocalDate.of(2026, 4, 1))).isEqualTo("2026-27");
    }
}
