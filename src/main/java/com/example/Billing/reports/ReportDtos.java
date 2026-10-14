package com.example.Billing.reports;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ReportDtos {
    private ReportDtos() {}

    public record Page<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    public record RegisterRow(Long id, String number, LocalDateTime date, String partyName,
                              String partyGstin, String documentType, String status,
                              BigDecimal taxableAmount, BigDecimal discountAmount,
                              BigDecimal cgst, BigDecimal sgst, BigDecimal igst,
                              BigDecimal cess, BigDecimal roundOff, BigDecimal totalAmount) {}

    public record PaymentRow(Long id, String invoiceNumber, LocalDateTime date, String partyName,
                             String method, BigDecimal amount, String reference) {}

    public record DayBookRow(LocalDateTime date, String kind, String reference,
                             String partyName, BigDecimal debit, BigDecimal credit) {}

    public record GstSection(String category, long documentCount, BigDecimal taxableValue,
                             BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal cess) {}

    public record HsnSummary(String hsn, String description, String uqc, BigDecimal quantity,
                             BigDecimal taxableValue, BigDecimal cgst, BigDecimal sgst,
                             BigDecimal igst, BigDecimal cess) {}

    public record DocumentSeries(String documentType, String financialYear, long issued,
                                 long cancelled, Integer firstNumber, Integer lastNumber) {}

    public record Gstr1Summary(List<GstSection> outwardSupplies, List<GstSection> notes,
                               List<HsnSummary> hsnSummary, List<DocumentSeries> documentSeries,
                               BigDecimal b2clThreshold, String filingWarning) {}

    public record Gstr3bSummary(BigDecimal outwardTaxable, BigDecimal outwardCgst,
                                BigDecimal outwardSgst, BigDecimal outwardIgst, BigDecimal outwardCess,
                                BigDecimal inwardTaxable, BigDecimal eligibleItcCgst,
                                BigDecimal eligibleItcSgst, BigDecimal eligibleItcIgst,
                                BigDecimal eligibleItcCess, BigDecimal netTaxPayable) {}

    public record StockRow(Long productId, String productName, String sku, String hsn,
                           BigDecimal quantity, BigDecimal weightedAverageCost,
                           BigDecimal stockValue, BigDecimal lowStockThreshold, boolean lowStock) {}

    public record StockMovementRow(Long id, Long productId, String productName, LocalDateTime date,
                                   String movementType, BigDecimal quantity, BigDecimal stockBefore,
                                   BigDecimal stockAfter, String reason) {}

    public record PartyBalance(Long partyId, String partyName, BigDecimal invoiced,
                               BigDecimal paid, BigDecimal outstanding) {}

    public record ProfitAndLoss(BigDecimal sales, BigDecimal creditNotes, BigDecimal netSales,
                                BigDecimal purchases, BigDecimal grossProfit,
                                String expenseLimitation) {}

    public record BusinessSummary(List<PartyBalance> receivables, List<PartyBalance> payables,
                                  ProfitAndLoss profitAndLoss) {}

    public record ExportSettings(String salesLedger, String purchaseLedger, String cgstLedger,
                                 String sgstLedger, String igstLedger, String cessLedger) {}
}
