package com.example.Billing.gst;

import com.example.Billing.product.TaxType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaxCalculationServiceTest {
    private final TaxCalculationService service = new TaxCalculationService();

    @ParameterizedTest
    @CsvSource({
            "33,33,90.00,20.00,90.00,9.00,9.00,0.00,108.00",
            "33,29,90.00,20.00,90.00,0.00,0.00,18.00,108.00",
            "33,33,118.00,18.00,100.00,9.00,9.00,0.00,118.00"
    })
    void calculatesStateAndPriceModes(String seller, String buyer, String price, String rate, String taxable,
                                      String cgst, String sgst, String igst, String total) {
        TaxType type = price.equals("118.00") ? TaxType.INCLUSIVE : TaxType.EXCLUSIVE;
        var result = calculate(seller, buyer, RegistrationType.REGULAR, "0.00",
                new TaxCalculationService.LineInput("line", bd("1"), bd(price), type, bd(rate), bd("0"), bd("0")));
        assertThat(result.taxableAmount()).isEqualByComparingTo(taxable);
        assertThat(result.cgst()).isEqualByComparingTo(cgst);
        assertThat(result.sgst()).isEqualByComparingTo(sgst);
        assertThat(result.igst()).isEqualByComparingTo(igst);
        assertThat(result.total()).isEqualByComparingTo(total);
    }

    @Test
    void roundsEachLineThenAllocatesInvoiceDiscountAndCess() {
        var result = service.calculate(new TaxCalculationService.Request("33", "33", RegistrationType.REGULAR,
                bd("10"), List.of(
                new TaxCalculationService.LineInput("a", bd("2"), bd("100"), TaxType.EXCLUSIVE, bd("5"), bd("1"), bd("5")),
                new TaxCalculationService.LineInput("b", bd("1"), bd("200"), TaxType.EXCLUSIVE, bd("18"), bd("0"), bd("0")))));
        assertThat(result.lines()).hasSize(2);
        assertThat(result.invoiceDiscount()).isEqualByComparingTo("10.00");
        assertThat(result.cess()).isEqualByComparingTo("1.90");
        assertThat(result.roundOff().scale()).isEqualTo(2);
        assertThat(result.total().scale()).isEqualTo(2);
    }

    @Test
    void compositionSellerProducesBillOfSupplyWithoutTax() {
        var result = calculate("33", "29", RegistrationType.COMPOSITION, "0",
                new TaxCalculationService.LineInput("line", bd("1"), bd("100"), TaxType.EXCLUSIVE, bd("18"), bd("0"), bd("0")));
        assertThat(result.billOfSupply()).isTrue();
        assertThat(result.cgst().add(result.sgst()).add(result.igst())).isEqualByComparingTo("0.00");
    }

    private TaxCalculationService.Calculation calculate(String seller, String buyer, RegistrationType type,
                                                        String discount, TaxCalculationService.LineInput line) {
        return service.calculate(new TaxCalculationService.Request(seller, buyer, type, bd(discount), List.of(line)));
    }

    private static BigDecimal bd(String value) { return new BigDecimal(value); }
}
