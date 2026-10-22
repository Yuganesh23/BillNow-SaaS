package com.example.Billing.gst;

import com.example.Billing.common.AppException;
import com.example.Billing.product.TaxType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaxCalculationService {
    public static final int MONEY_SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public Calculation calculate(Request request) {
        if (request.lines() == null || request.lines().isEmpty()) {
            throw invalid("DOCUMENT_ITEMS_REQUIRED", "Document must contain at least one item");
        }
        BigDecimal invoiceDiscount = money(nonNegative(request.invoiceDiscount(), "Invoice discount"));
        boolean billOfSupply = request.registrationType() == RegistrationType.COMPOSITION
                || request.registrationType() == RegistrationType.UNREGISTERED;
        String destination = hasText(request.destinationStateCode())
                ? request.destinationStateCode() : request.sellerStateCode();
        validateState(request.sellerStateCode());
        validateState(destination);
        boolean interState = !request.sellerStateCode().equals(destination);

        List<UnallocatedLine> prepared = request.lines().stream().map(this::prepare).toList();
        BigDecimal grossTaxable = prepared.stream().map(UnallocatedLine::taxableBeforeInvoiceDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (invoiceDiscount.compareTo(grossTaxable) > 0) {
            throw invalid("INVALID_DISCOUNT", "Invoice discount cannot exceed taxable value");
        }

        List<LineResult> results = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < prepared.size(); index++) {
            UnallocatedLine line = prepared.get(index);
            BigDecimal share;
            if (index == prepared.size() - 1) {
                share = invoiceDiscount.subtract(allocated);
            } else if (grossTaxable.signum() == 0) {
                share = BigDecimal.ZERO.setScale(MONEY_SCALE);
            } else {
                share = money(invoiceDiscount.multiply(line.taxableBeforeInvoiceDiscount()).divide(grossTaxable, 8, ROUNDING));
                allocated = allocated.add(share);
            }
            BigDecimal taxable = money(line.taxableBeforeInvoiceDiscount().subtract(share));
            BigDecimal gst = billOfSupply ? zero() : money(taxable.multiply(line.input().gstRate()).divide(HUNDRED, 8, ROUNDING));
            BigDecimal cess = billOfSupply ? zero() : money(taxable.multiply(line.input().cessRate()).divide(HUNDRED, 8, ROUNDING));
            BigDecimal cgst = interState ? zero() : money(gst.divide(new BigDecimal("2"), 8, ROUNDING));
            BigDecimal sgst = interState ? zero() : gst.subtract(cgst).setScale(MONEY_SCALE, ROUNDING);
            BigDecimal igst = interState ? gst : zero();
            BigDecimal total = money(taxable.add(cgst).add(sgst).add(igst).add(cess));
            results.add(new LineResult(line.input().reference(), taxable, share, cgst, sgst, igst, cess, total));
        }

        BigDecimal taxable = sum(results, LineResult::taxableAmount);
        BigDecimal cgst = sum(results, LineResult::cgst);
        BigDecimal sgst = sum(results, LineResult::sgst);
        BigDecimal igst = sum(results, LineResult::igst);
        BigDecimal cess = sum(results, LineResult::cess);
        BigDecimal unrounded = money(taxable.add(cgst).add(sgst).add(igst).add(cess));
        BigDecimal rounded = unrounded.setScale(0, ROUNDING).setScale(MONEY_SCALE, ROUNDING);
        BigDecimal roundOff = rounded.subtract(unrounded).setScale(MONEY_SCALE, ROUNDING);
        return new Calculation(results, taxable, invoiceDiscount, cgst, sgst, igst, cess,
                roundOff, rounded, interState, destination, billOfSupply);
    }

    private UnallocatedLine prepare(LineInput input) {
        if (input == null || input.quantity() == null || input.quantity().signum() <= 0) {
            throw invalid("INVALID_QUANTITY", "Quantity must be greater than zero");
        }
        BigDecimal price = nonNegative(input.unitPrice(), "Unit price");
        BigDecimal lineDiscount = money(nonNegative(input.lineDiscount(), "Line discount"));
        BigDecimal gstRate = nonNegative(input.gstRate(), "GST rate");
        BigDecimal cessRate = nonNegative(input.cessRate(), "Cess rate");
        BigDecimal entered = money(price.multiply(input.quantity()));
        if (lineDiscount.compareTo(entered) > 0) throw invalid("INVALID_DISCOUNT", "Line discount exceeds line value");
        BigDecimal afterDiscount = entered.subtract(lineDiscount);
        BigDecimal taxable = afterDiscount;
        if (input.taxType() == TaxType.INCLUSIVE && gstRate.add(cessRate).signum() > 0) {
            taxable = money(afterDiscount.multiply(HUNDRED)
                    .divide(HUNDRED.add(gstRate).add(cessRate), 8, ROUNDING));
        }
        return new UnallocatedLine(input, taxable);
    }

    private static BigDecimal sum(List<LineResult> lines, java.util.function.Function<LineResult, BigDecimal> getter) {
        return money(lines.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal nonNegative(BigDecimal value, String label) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value;
        if (normalized.signum() < 0) throw invalid("INVALID_AMOUNT", label + " cannot be negative");
        return normalized;
    }

    private static void validateState(String code) {
        if (IndianState.fromCode(code).isEmpty()) throw invalid("INVALID_STATE_CODE", "Invalid Indian state or UT code");
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
    private static BigDecimal money(BigDecimal value) { return value.setScale(MONEY_SCALE, ROUNDING); }
    private static BigDecimal zero() { return BigDecimal.ZERO.setScale(MONEY_SCALE); }
    private static AppException invalid(String code, String message) { return new AppException(HttpStatus.BAD_REQUEST, code, message); }

    private record UnallocatedLine(LineInput input, BigDecimal taxableBeforeInvoiceDiscount) {}

    public record LineInput(String reference, BigDecimal quantity, BigDecimal unitPrice, TaxType taxType,
                            BigDecimal gstRate, BigDecimal cessRate, BigDecimal lineDiscount) {}
    public record Request(String sellerStateCode, String destinationStateCode, RegistrationType registrationType,
                          BigDecimal invoiceDiscount, List<LineInput> lines) {}
    public record LineResult(String reference, BigDecimal taxableAmount, BigDecimal discountAllocated,
                             BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal cess, BigDecimal total) {}
    public record Calculation(List<LineResult> lines, BigDecimal taxableAmount, BigDecimal invoiceDiscount,
                              BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal cess,
                              BigDecimal roundOff, BigDecimal total, boolean interState,
                              String placeOfSupplyCode, boolean billOfSupply) {}
}
