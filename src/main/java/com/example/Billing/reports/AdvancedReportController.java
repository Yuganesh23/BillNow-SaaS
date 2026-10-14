package com.example.Billing.reports;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports/v2")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SHOP_OWNER', 'MANAGER')")
public class AdvancedReportController {
    private final AdvancedReportService reports;
    private final ReportExportService exports;

    @GetMapping("/registers/{type}")
    public ReportDtos.Page<ReportDtos.RegisterRow> register(Authentication auth, @PathVariable String type,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) Long partyId, @RequestParam(required=false) BigDecimal taxRate,
            @RequestParam(required=false) String documentType, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="25") int size) {
        return reports.register(user(auth), type, from, to, partyId, taxRate, documentType, page, size);
    }

    @GetMapping("/payments")
    public java.util.List<ReportDtos.PaymentRow> payments(Authentication auth,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.payments(user(auth), from, to);
    }

    @GetMapping("/day-book")
    public java.util.List<ReportDtos.DayBookRow> dayBook(Authentication auth,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.dayBook(user(auth), from, to);
    }

    @GetMapping(value="/payments/export.csv", produces="text/csv")
    public ResponseEntity<byte[]> paymentsCsv(Authentication auth,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return download(exports.paymentsCsv(user(auth),from,to),"payment-register.csv",MediaType.parseMediaType("text/csv"));
    }

    @GetMapping(value="/day-book/export.csv", produces="text/csv")
    public ResponseEntity<byte[]> dayBookCsv(Authentication auth,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return download(exports.dayBookCsv(user(auth),from,to),"day-book.csv",MediaType.parseMediaType("text/csv"));
    }

    @GetMapping("/gst/gstr1")
    public ReportDtos.Gstr1Summary gstr1(Authentication auth,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) BigDecimal b2clThreshold) {
        return reports.gstr1(user(auth), from, to, b2clThreshold);
    }

    @GetMapping("/gst/gstr3b")
    public ReportDtos.Gstr3bSummary gstr3b(Authentication auth,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.gstr3b(user(auth), from, to);
    }

    @GetMapping("/inventory/stock")
    public java.util.List<ReportDtos.StockRow> stock(Authentication auth,
            @RequestParam(defaultValue="false") boolean lowOnly) { return reports.stock(user(auth), lowOnly); }

    @GetMapping("/inventory/movements")
    public java.util.List<ReportDtos.StockMovementRow> movements(Authentication auth,
            @RequestParam(required=false) Long productId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.movements(user(auth), productId, from, to);
    }

    @GetMapping("/business")
    public ReportDtos.BusinessSummary business(Authentication auth,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.business(user(auth), from, to);
    }

    @GetMapping("/export-settings")
    public ReportDtos.ExportSettings settings(Authentication auth) { return reports.settings(user(auth)); }

    @PutMapping("/export-settings")
    public ReportDtos.ExportSettings settings(Authentication auth, @RequestBody ReportDtos.ExportSettings request) {
        return reports.saveSettings(user(auth), request);
    }

    @GetMapping(value="/registers/{type}/export.csv", produces="text/csv")
    public ResponseEntity<byte[]> registerCsv(Authentication auth, @PathVariable String type,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) Long partyId, @RequestParam(required=false) BigDecimal taxRate,
            @RequestParam(required=false) String documentType) {
        return download(exports.registerCsv(user(auth), type, from, to, partyId, taxRate, documentType),
                type+"-register.csv", MediaType.parseMediaType("text/csv"));
    }

    @GetMapping(value="/registers/{type}/export.xlsx")
    public ResponseEntity<byte[]> registerExcel(Authentication auth, @PathVariable String type,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) Long partyId, @RequestParam(required=false) BigDecimal taxRate,
            @RequestParam(required=false) String documentType) {
        return download(exports.registerExcel(user(auth), type, from, to, partyId, taxRate, documentType),
                type+"-register.xlsx", MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/gst/export.xlsx")
    public ResponseEntity<byte[]> gstExcel(Authentication auth,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) BigDecimal b2clThreshold) {
        return download(exports.gstExcel(user(auth), from, to, b2clThreshold), "gst-summary.xlsx",
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping(value="/tally/{type}.xml", produces=MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> tally(Authentication auth, @PathVariable String type,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        if (!java.util.Set.of("sales","purchases").contains(type))
            throw new AppException(HttpStatus.BAD_REQUEST,"INVALID_TALLY_TYPE","Tally export supports sales or purchases");
        return download(exports.tallyXml(user(auth), type, from, to), "tally-"+type+".xml", MediaType.APPLICATION_XML);
    }

    private ResponseEntity<byte[]> download(byte[] bytes, String name, MediaType type) {
        return ResponseEntity.ok().contentType(type).header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(name).build().toString()).body(bytes);
    }
    private User_entity user(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User_entity user))
            throw new AppException(HttpStatus.UNAUTHORIZED,"AUTH_REQUIRED","Authentication is required");
        return user;
    }
}
