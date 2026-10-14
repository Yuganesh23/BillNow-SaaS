package com.example.Billing.reports;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.config.ShopContextResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdvancedReportService {
    private final DerivedReportRepository reports;
    private final ReportExportSettingsRepository settingsRepository;
    private final ShopContextResolver shopContext;

    @Value("${reports.gst.b2cl-threshold:250000.00}")
    private BigDecimal configuredB2clThreshold;

    public ReportDtos.Page<ReportDtos.RegisterRow> register(User_entity user, String type, LocalDate from, LocalDate to,
                                                             Long partyId, BigDecimal taxRate, String documentType,
                                                             int page, int size) {
        Range range = range(from, to);
        if (page < 0 || size < 1 || size > 200) bad("INVALID_PAGE", "Page must be non-negative and size must be between 1 and 200");
        if (!java.util.Set.of("sales", "purchases", "notes").contains(type)) bad("INVALID_REGISTER", "Unsupported register type");
        return reports.register(type, shopContext.resolveActiveShopId(user), range.from(), range.to(), partyId,
                taxRate, documentType, page, size);
    }

    public java.util.List<ReportDtos.RegisterRow> registerExport(User_entity user, String type, LocalDate from, LocalDate to,
                                                                  Long partyId, BigDecimal taxRate, String documentType) {
        java.util.ArrayList<ReportDtos.RegisterRow> all = new java.util.ArrayList<>();
        int page = 0;
        ReportDtos.Page<ReportDtos.RegisterRow> result;
        do {
            result = register(user, type, from, to, partyId, taxRate, documentType, page++, 200);
            all.addAll(result.content());
        } while (page < result.totalPages());
        return all;
    }

    public ReportDtos.Page<ReportDtos.PaymentRow> payments(User_entity user, LocalDate from, LocalDate to,
                                                            int page, int size) {
        Range range = range(from, to);
        validatePage(page, size);
        return reports.payments(shopContext.resolveActiveShopId(user), range.from(), range.to(), page, size);
    }

    public ReportDtos.Page<ReportDtos.DayBookRow> dayBook(User_entity user, LocalDate from, LocalDate to,
                                                          int page, int size) {
        Range range = range(from, to);
        validatePage(page, size);
        return reports.dayBook(shopContext.resolveActiveShopId(user), range.from(), range.to(), page, size);
    }

    public java.util.List<ReportDtos.PaymentRow> paymentExport(User_entity user, LocalDate from, LocalDate to) {
        java.util.ArrayList<ReportDtos.PaymentRow> all = new java.util.ArrayList<>();
        int page = 0; ReportDtos.Page<ReportDtos.PaymentRow> result;
        do { result = payments(user, from, to, page++, 200); all.addAll(result.content()); }
        while (page < result.totalPages());
        return all;
    }

    public java.util.List<ReportDtos.DayBookRow> dayBookExport(User_entity user, LocalDate from, LocalDate to) {
        java.util.ArrayList<ReportDtos.DayBookRow> all = new java.util.ArrayList<>();
        int page = 0; ReportDtos.Page<ReportDtos.DayBookRow> result;
        do { result = dayBook(user, from, to, page++, 200); all.addAll(result.content()); }
        while (page < result.totalPages());
        return all;
    }

    public java.util.List<ReportDtos.PartyOption> partyOptions(User_entity user, String type) {
        if (!java.util.Set.of("sales", "purchases", "notes").contains(type))
            bad("INVALID_REGISTER", "Unsupported register type");
        return reports.partyOptions(type, shopContext.resolveActiveShopId(user));
    }

    public ReportDtos.Gstr1Summary gstr1(User_entity user, LocalDate from, LocalDate to, BigDecimal threshold) {
        Range range = range(from, to);
        BigDecimal applied = threshold == null ? configuredB2clThreshold : threshold;
        if (applied == null || applied.signum() < 0) bad("INVALID_B2CL_THRESHOLD", "B2C large threshold must be configured and non-negative");
        Long shopId = shopContext.resolveActiveShopId(user);
        return new ReportDtos.Gstr1Summary(reports.outward(shopId, range.from(), range.to(), applied),
                reports.notes(shopId, range.from(), range.to()), reports.hsn(shopId, range.from(), range.to()),
                reports.series(shopId, range.from(), range.to()), applied,
                "Review classifications and the configured B2C-large threshold with a CA before filing.");
    }

    public ReportDtos.Gstr3bSummary gstr3b(User_entity user, LocalDate from, LocalDate to) {
        Range range = range(from, to);
        return reports.gstr3b(shopContext.resolveActiveShopId(user), range.from(), range.to());
    }

    public java.util.List<ReportDtos.StockRow> stock(User_entity user, boolean lowOnly) {
        return reports.stock(shopContext.resolveActiveShopId(user), lowOnly);
    }

    public java.util.List<ReportDtos.StockMovementRow> movements(User_entity user, Long productId,
                                                                  LocalDate from, LocalDate to) {
        Range range = range(from, to);
        return reports.movements(shopContext.resolveActiveShopId(user), productId, range.from(), range.to());
    }

    public ReportDtos.BusinessSummary business(User_entity user, LocalDate from, LocalDate to) {
        Range range = range(from, to); Long shopId = shopContext.resolveActiveShopId(user);
        return new ReportDtos.BusinessSummary(reports.receivables(shopId, range.from(), range.to()),
                reports.payables(shopId, range.from(), range.to()), reports.profitAndLoss(shopId, range.from(), range.to()));
    }

    public ReportDtos.ExportSettings settings(User_entity user) {
        Long shopId = shopContext.resolveActiveShopId(user);
        return settingsRepository.findByShopId(shopId).map(this::map).orElse(defaultSettings());
    }

    @Transactional
    public ReportDtos.ExportSettings saveSettings(User_entity user, ReportDtos.ExportSettings request) {
        com.example.Billing.shop.Shop_entity shop = shopContext.resolveActiveShop(user);
        Long shopId = shop.getId();
        validateSettings(request);
        ReportExportSettings_entity entity = settingsRepository.findByShopId(shopId)
                .orElseGet(() -> ReportExportSettings_entity.builder().shop(shop).build());
        entity.setSalesLedger(request.salesLedger().trim()); entity.setPurchaseLedger(request.purchaseLedger().trim());
        entity.setCgstLedger(request.cgstLedger().trim()); entity.setSgstLedger(request.sgstLedger().trim());
        entity.setIgstLedger(request.igstLedger().trim()); entity.setCessLedger(request.cessLedger().trim());
        return map(settingsRepository.save(entity));
    }

    private void validateSettings(ReportDtos.ExportSettings request) {
        if (request == null || java.util.stream.Stream.of(request.salesLedger(), request.purchaseLedger(), request.cgstLedger(),
                request.sgstLedger(), request.igstLedger(), request.cessLedger()).anyMatch(v -> v == null || v.isBlank() || v.length() > 100)) {
            bad("INVALID_EXPORT_SETTINGS", "Every ledger name is required and must be at most 100 characters");
        }
    }
    private ReportDtos.ExportSettings map(ReportExportSettings_entity e) { return new ReportDtos.ExportSettings(e.getSalesLedger(),
            e.getPurchaseLedger(), e.getCgstLedger(), e.getSgstLedger(), e.getIgstLedger(), e.getCessLedger()); }
    private ReportDtos.ExportSettings defaultSettings() { return new ReportDtos.ExportSettings("Sales", "Purchases", "Output CGST", "Output SGST", "Output IGST", "Output Cess"); }
    private Range range(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) bad("INVALID_DATE_RANGE", "A valid from/to date range is required");
        if (from.plusYears(2).isBefore(to)) bad("DATE_RANGE_TOO_LARGE", "Report range cannot exceed two years");
        return new Range(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }
    private void bad(String code, String message) { throw new AppException(HttpStatus.BAD_REQUEST, code, message); }
    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 200)
            bad("INVALID_PAGE", "Page must be non-negative and size must be between 1 and 200");
    }
    private record Range(LocalDateTime from, LocalDateTime to) {}
}
