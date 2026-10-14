package com.example.Billing.reports;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DerivedReportRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public ReportDtos.Page<ReportDtos.RegisterRow> register(String type, Long shopId, LocalDateTime from,
                                                             LocalDateTime to, Long partyId, BigDecimal taxRate,
                                                             String documentType, int page, int size) {
        String source = switch (type) {
            case "sales" -> """
                SELECT i.id, i.invoice_number number, i.created_at, c.name party_name, c.gstin party_gstin,
                       i.document_type, i.status, i.taxable_amount, i.discount_amount,
                       i.cgst_total, i.sgst_total, i.igst_total, i.cess_total, i.round_off, i.total_amount
                FROM invoices i JOIN customers c ON c.id=i.customer_id
                WHERE i.shop_id=:shopId AND i.created_at>=:from AND i.created_at<:to
                  AND (:partyId IS NULL OR i.customer_id=:partyId)
                  AND (:documentType IS NULL OR i.document_type=:documentType)
                  AND (:taxRate IS NULL OR EXISTS (SELECT 1 FROM invoice_items ii WHERE ii.invoice_id=i.id AND ii.gst_rate=:taxRate))
                """;
            case "purchases" -> """
                SELECT p.id, p.purchase_number number, p.created_at, s.business_name party_name, s.gstin party_gstin,
                       'PURCHASE' document_type, p.status, p.taxable_amount, p.discount_amount,
                       p.cgst_total, p.sgst_total, p.igst_total, p.cess_total, 0 round_off, p.grand_total total_amount
                FROM purchases p JOIN suppliers s ON s.id=p.supplier_id
                WHERE p.shop_id=:shopId AND p.created_at>=:from AND p.created_at<:to
                  AND (:partyId IS NULL OR p.supplier_id=:partyId)
                  AND (:taxRate IS NULL OR EXISTS (SELECT 1 FROM purchase_items pi JOIN products pr ON pr.id=pi.product_id
                                                    WHERE pi.purchase_id=p.id AND pr.gst_rate=:taxRate))
                """;
            case "notes" -> """
                SELECT n.id, n.note_number number, n.created_at, c.name party_name, c.gstin party_gstin,
                       n.note_type document_type, 'ISSUED' status,
                       n.amount*i.taxable_amount/NULLIF(i.total_amount,0) taxable_amount, 0 discount_amount,
                       n.amount*i.cgst_total/NULLIF(i.total_amount,0) cgst_total,
                       n.amount*i.sgst_total/NULLIF(i.total_amount,0) sgst_total,
                       n.amount*i.igst_total/NULLIF(i.total_amount,0) igst_total,
                       n.amount*i.cess_total/NULLIF(i.total_amount,0) cess_total,
                       0 round_off, n.amount total_amount
                FROM document_notes n JOIN invoices i ON i.id=n.original_invoice_id JOIN customers c ON c.id=i.customer_id
                WHERE n.shop_id=:shopId AND n.created_at>=:from AND n.created_at<:to
                  AND (:partyId IS NULL OR i.customer_id=:partyId)
                  AND (:documentType IS NULL OR n.note_type=:documentType)
                  AND :taxRate IS NULL
                """;
            default -> throw new IllegalArgumentException("Unknown register type");
        };
        MapSqlParameterSource params = params(shopId, from, to).addValue("partyId", partyId)
                .addValue("taxRate", taxRate).addValue("documentType", blankToNull(documentType))
                .addValue("limit", size).addValue("offset", page * size);
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM (" + source + ") report_rows", params, Long.class);
        List<ReportDtos.RegisterRow> rows = jdbc.query(source + " ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset",
                params, (rs, row) -> registerRow(rs));
        return new ReportDtos.Page<>(rows, page, size, count, (int) Math.ceil((double) count / size));
    }

    public List<ReportDtos.PaymentRow> payments(Long shopId, LocalDateTime from, LocalDateTime to) {
        return jdbc.query("""
            SELECT p.id, i.invoice_number, p.paid_at, c.name party_name, p.payment_method, p.amount, p.reference
            FROM payments p JOIN invoices i ON i.id=p.invoice_id JOIN customers c ON c.id=i.customer_id
            WHERE i.shop_id=:shopId AND p.paid_at>=:from AND p.paid_at<:to
            ORDER BY p.paid_at DESC, p.id DESC
            """, params(shopId, from, to), (rs, n) -> new ReportDtos.PaymentRow(rs.getLong("id"),
                rs.getString("invoice_number"), timestamp(rs, "paid_at"), rs.getString("party_name"),
                rs.getString("payment_method"), money(rs, "amount"), rs.getString("reference")));
    }

    public List<ReportDtos.DayBookRow> dayBook(Long shopId, LocalDateTime from, LocalDateTime to) {
        return jdbc.query("""
            SELECT entry_date, kind, reference, party_name, debit, credit FROM (
              SELECT p.paid_at entry_date, 'CUSTOMER_PAYMENT' kind, i.invoice_number reference,
                     c.name party_name, 0 debit, p.amount credit
              FROM payments p JOIN invoices i ON i.id=p.invoice_id JOIN customers c ON c.id=i.customer_id
              WHERE i.shop_id=:shopId AND p.paid_at>=:from AND p.paid_at<:to
              UNION ALL
              SELECT sl.transaction_date entry_date, 'SUPPLIER_' || sl.type kind,
                     COALESCE(pu.purchase_number, '') reference, s.business_name party_name,
                     COALESCE(sl.debit_amount,0) debit, COALESCE(sl.credit_amount,0) credit
              FROM supplier_ledger sl JOIN suppliers s ON s.id=sl.supplier_id
              LEFT JOIN purchases pu ON pu.id=sl.purchase_id
              WHERE sl.shop_id=:shopId AND sl.transaction_date>=:from AND sl.transaction_date<:to
            ) entries ORDER BY entry_date DESC
            """, params(shopId, from, to), (rs, n) -> new ReportDtos.DayBookRow(timestamp(rs, "entry_date"),
                rs.getString("kind"), rs.getString("reference"), rs.getString("party_name"),
                money(rs, "debit"), money(rs, "credit")));
    }

    public List<ReportDtos.GstSection> outward(Long shopId, LocalDateTime from, LocalDateTime to,
                                                BigDecimal b2clThreshold) {
        MapSqlParameterSource p = params(shopId, from, to).addValue("threshold", b2clThreshold);
        return jdbc.query("""
            SELECT CASE WHEN c.gstin IS NOT NULL AND c.gstin<>'' THEN 'B2B'
                        WHEN i.is_inter_state=TRUE AND i.total_amount>=:threshold THEN 'B2C_LARGE'
                        ELSE 'B2C_SMALL' END category,
                   COUNT(*) document_count, COALESCE(SUM(i.taxable_amount),0) taxable_value,
                   COALESCE(SUM(i.cgst_total),0) cgst, COALESCE(SUM(i.sgst_total),0) sgst,
                   COALESCE(SUM(i.igst_total),0) igst, COALESCE(SUM(i.cess_total),0) cess
            FROM invoices i JOIN customers c ON c.id=i.customer_id
            WHERE i.shop_id=:shopId AND i.status<>'CANCELLED' AND i.created_at>=:from AND i.created_at<:to
            GROUP BY CASE WHEN c.gstin IS NOT NULL AND c.gstin<>'' THEN 'B2B'
                          WHEN i.is_inter_state=TRUE AND i.total_amount>=:threshold THEN 'B2C_LARGE'
                          ELSE 'B2C_SMALL' END
            ORDER BY category
            """, p, (rs, n) -> gstSection(rs));
    }

    public List<ReportDtos.GstSection> notes(Long shopId, LocalDateTime from, LocalDateTime to) {
        return jdbc.query("""
            SELECT n.note_type category, COUNT(*) document_count,
                   COALESCE(SUM(n.amount*i.taxable_amount/NULLIF(i.total_amount,0)),0) taxable_value,
                   COALESCE(SUM(n.amount*i.cgst_total/NULLIF(i.total_amount,0)),0) cgst,
                   COALESCE(SUM(n.amount*i.sgst_total/NULLIF(i.total_amount,0)),0) sgst,
                   COALESCE(SUM(n.amount*i.igst_total/NULLIF(i.total_amount,0)),0) igst,
                   COALESCE(SUM(n.amount*i.cess_total/NULLIF(i.total_amount,0)),0) cess
            FROM document_notes n JOIN invoices i ON i.id=n.original_invoice_id
            WHERE n.shop_id=:shopId AND n.created_at>=:from AND n.created_at<:to
            GROUP BY n.note_type ORDER BY n.note_type
            """, params(shopId, from, to), (rs, n) -> gstSection(rs));
    }

    public List<ReportDtos.HsnSummary> hsn(Long shopId, LocalDateTime from, LocalDateTime to) {
        return jdbc.query("""
            SELECT COALESCE(pr.hsn_code,'UNSPECIFIED') hsn, pr.name description, pr.uqc,
                   COALESCE(SUM(ii.quantity),0) quantity, COALESCE(SUM(ii.taxable_amount),0) taxable_value,
                   COALESCE(SUM(ii.cgst),0) cgst, COALESCE(SUM(ii.sgst),0) sgst,
                   COALESCE(SUM(ii.igst),0) igst, COALESCE(SUM(ii.cess),0) cess
            FROM invoice_items ii JOIN invoices i ON i.id=ii.invoice_id JOIN products pr ON pr.id=ii.product_id
            WHERE i.shop_id=:shopId AND i.status<>'CANCELLED' AND i.created_at>=:from AND i.created_at<:to
            GROUP BY pr.hsn_code, pr.name, pr.uqc ORDER BY pr.hsn_code, pr.name
            """, params(shopId, from, to), (rs, n) -> new ReportDtos.HsnSummary(rs.getString("hsn"),
                rs.getString("description"), rs.getString("uqc"), decimal(rs, "quantity"),
                money(rs, "taxable_value"), money(rs, "cgst"), money(rs, "sgst"), money(rs, "igst"), money(rs, "cess")));
    }

    public List<ReportDtos.DocumentSeries> series(Long shopId, LocalDateTime from, LocalDateTime to) {
        return jdbc.query("""
            SELECT document_type, COALESCE(financial_year,'UNKNOWN') financial_year, COUNT(*) issued,
                   SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END) cancelled,
                   MIN(series_number) first_number, MAX(series_number) last_number
            FROM invoices WHERE shop_id=:shopId AND created_at>=:from AND created_at<:to
            GROUP BY document_type, financial_year ORDER BY financial_year, document_type
            """, params(shopId, from, to), (rs, n) -> new ReportDtos.DocumentSeries(rs.getString("document_type"),
                rs.getString("financial_year"), rs.getLong("issued"), rs.getLong("cancelled"),
                nullableInt(rs, "first_number"), nullableInt(rs, "last_number")));
    }

    public ReportDtos.Gstr3bSummary gstr3b(Long shopId, LocalDateTime from, LocalDateTime to) {
        MapSqlParameterSource p = params(shopId, from, to);
        BigDecimal[] outward = jdbc.queryForObject("""
            SELECT COALESCE(SUM(taxable_amount),0) taxable, COALESCE(SUM(cgst_total),0) cgst,
                   COALESCE(SUM(sgst_total),0) sgst, COALESCE(SUM(igst_total),0) igst,
                   COALESCE(SUM(cess_total),0) cess
            FROM invoices WHERE shop_id=:shopId AND status<>'CANCELLED' AND created_at>=:from AND created_at<:to
            """, p, (rs, n) -> amounts(rs, "taxable", "cgst", "sgst", "igst", "cess"));
        BigDecimal[] credits = jdbc.queryForObject("""
            SELECT COALESCE(SUM(n.amount*i.taxable_amount/NULLIF(i.total_amount,0)),0) taxable,
                   COALESCE(SUM(n.amount*i.cgst_total/NULLIF(i.total_amount,0)),0) cgst,
                   COALESCE(SUM(n.amount*i.sgst_total/NULLIF(i.total_amount,0)),0) sgst,
                   COALESCE(SUM(n.amount*i.igst_total/NULLIF(i.total_amount,0)),0) igst,
                   COALESCE(SUM(n.amount*i.cess_total/NULLIF(i.total_amount,0)),0) cess
            FROM document_notes n JOIN invoices i ON i.id=n.original_invoice_id
            WHERE n.shop_id=:shopId AND n.note_type='CREDIT_NOTE' AND n.created_at>=:from AND n.created_at<:to
            """, p, (rs, n) -> amounts(rs, "taxable", "cgst", "sgst", "igst", "cess"));
        BigDecimal[] inward = jdbc.queryForObject("""
            SELECT COALESCE(SUM(taxable_amount),0) taxable, COALESCE(SUM(cgst_total),0) cgst,
                   COALESCE(SUM(sgst_total),0) sgst, COALESCE(SUM(igst_total),0) igst,
                   COALESCE(SUM(cess_total),0) cess
            FROM purchases WHERE shop_id=:shopId AND created_at>=:from AND created_at<:to
            """, p, (rs, n) -> amounts(rs, "taxable", "cgst", "sgst", "igst", "cess"));
        BigDecimal taxable = outward[0].subtract(credits[0]);
        BigDecimal cgst = outward[1].subtract(credits[1]);
        BigDecimal sgst = outward[2].subtract(credits[2]);
        BigDecimal igst = outward[3].subtract(credits[3]);
        BigDecimal cess = outward[4].subtract(credits[4]);
        BigDecimal net = cgst.add(sgst).add(igst).add(cess)
                .subtract(inward[1]).subtract(inward[2]).subtract(inward[3]).subtract(inward[4]);
        return new ReportDtos.Gstr3bSummary(scale(taxable), scale(cgst), scale(sgst), scale(igst), scale(cess),
                scale(inward[0]), scale(inward[1]), scale(inward[2]), scale(inward[3]), scale(inward[4]), scale(net));
    }

    public List<ReportDtos.StockRow> stock(Long shopId, boolean lowOnly) {
        return jdbc.query("""
            SELECT pr.id, pr.name, pr.sku, pr.hsn_code, pr.stock_quantity, pr.low_stock_threshold,
                   COALESCE(SUM(pi.quantity*pi.purchase_price)/NULLIF(SUM(pi.quantity),0), pr.purchase_price) average_cost
            FROM products pr LEFT JOIN purchase_items pi ON pi.product_id=pr.id
            WHERE pr.shop_id=:shopId AND pr.active=TRUE
              AND (:lowOnly=FALSE OR pr.stock_quantity<=pr.low_stock_threshold)
            GROUP BY pr.id, pr.name, pr.sku, pr.hsn_code, pr.stock_quantity, pr.low_stock_threshold, pr.purchase_price
            ORDER BY pr.name
            """, new MapSqlParameterSource().addValue("shopId", shopId).addValue("lowOnly", lowOnly), (rs, n) -> {
                BigDecimal qty = decimal(rs, "stock_quantity"); BigDecimal average = money(rs, "average_cost");
                BigDecimal threshold = decimal(rs, "low_stock_threshold");
                return new ReportDtos.StockRow(rs.getLong("id"), rs.getString("name"), rs.getString("sku"),
                        rs.getString("hsn_code"), qty, average, scale(qty.multiply(average)), threshold,
                        qty.compareTo(threshold) <= 0);
            });
    }

    public List<ReportDtos.StockMovementRow> movements(Long shopId, Long productId,
                                                        LocalDateTime from, LocalDateTime to) {
        MapSqlParameterSource p = params(shopId, from, to).addValue("productId", productId);
        return jdbc.query("""
            SELECT im.id, im.product_id, pr.name, im.created_at, im.movement_type, im.quantity,
                   im.stock_before, im.stock_after, im.reason
            FROM inventory_movements im JOIN products pr ON pr.id=im.product_id
            WHERE im.shop_id=:shopId AND im.created_at>=:from AND im.created_at<:to
              AND (:productId IS NULL OR im.product_id=:productId)
            ORDER BY im.created_at DESC, im.id DESC
            """, p, (rs, n) -> new ReportDtos.StockMovementRow(rs.getLong("id"), rs.getLong("product_id"),
                rs.getString("name"), timestamp(rs, "created_at"), rs.getString("movement_type"),
                decimal(rs, "quantity"), decimal(rs, "stock_before"), decimal(rs, "stock_after"), rs.getString("reason")));
    }

    public List<ReportDtos.PartyBalance> receivables(Long shopId, LocalDateTime from, LocalDateTime to) {
        return jdbc.query("""
            SELECT c.id, c.name, COALESCE(SUM(i.total_amount),0) invoiced,
                   COALESCE(SUM((SELECT COALESCE(SUM(p.amount),0) FROM payments p WHERE p.invoice_id=i.id)),0) paid
            FROM customers c JOIN invoices i ON i.customer_id=c.id AND i.shop_id=c.shop_id
            WHERE c.shop_id=:shopId AND i.status<>'CANCELLED' AND i.created_at>=:from AND i.created_at<:to
            GROUP BY c.id,c.name ORDER BY COALESCE(SUM(i.total_amount),0)-COALESCE(SUM((SELECT COALESCE(SUM(p.amount),0) FROM payments p WHERE p.invoice_id=i.id)),0) DESC
            """, params(shopId, from, to), (rs, n) -> balance(rs));
    }

    public List<ReportDtos.PartyBalance> payables(Long shopId, LocalDateTime from, LocalDateTime to) {
        return jdbc.query("""
            SELECT s.id, s.business_name name, COALESCE(SUM(p.grand_total),0) invoiced,
                   COALESCE(SUM(p.amount_paid),0) paid
            FROM suppliers s JOIN purchases p ON p.supplier_id=s.id AND p.shop_id=s.shop_id
            WHERE s.shop_id=:shopId AND p.created_at>=:from AND p.created_at<:to
            GROUP BY s.id,s.business_name ORDER BY COALESCE(SUM(p.grand_total),0)-COALESCE(SUM(p.amount_paid),0) DESC
            """, params(shopId, from, to), (rs, n) -> balance(rs));
    }

    public ReportDtos.ProfitAndLoss profitAndLoss(Long shopId, LocalDateTime from, LocalDateTime to) {
        MapSqlParameterSource p = params(shopId, from, to);
        BigDecimal sales = jdbc.queryForObject("SELECT COALESCE(SUM(total_amount),0) FROM invoices WHERE shop_id=:shopId AND status<>'CANCELLED' AND created_at>=:from AND created_at<:to", p, BigDecimal.class);
        BigDecimal credits = jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM document_notes WHERE shop_id=:shopId AND note_type='CREDIT_NOTE' AND created_at>=:from AND created_at<:to", p, BigDecimal.class);
        BigDecimal purchases = jdbc.queryForObject("SELECT COALESCE(SUM(grand_total),0) FROM purchases WHERE shop_id=:shopId AND created_at>=:from AND created_at<:to", p, BigDecimal.class);
        BigDecimal netSales = nz(sales).subtract(nz(credits));
        return new ReportDtos.ProfitAndLoss(scale(sales), scale(credits), scale(netSales), scale(purchases),
                scale(netSales.subtract(nz(purchases))), "Expenses are not yet modelled; this is sales less purchases, not statutory net profit.");
    }

    private ReportDtos.RegisterRow registerRow(ResultSet rs) throws SQLException {
        return new ReportDtos.RegisterRow(rs.getLong("id"), rs.getString("number"), timestamp(rs, "created_at"),
                rs.getString("party_name"), rs.getString("party_gstin"), rs.getString("document_type"),
                rs.getString("status"), money(rs, "taxable_amount"), money(rs, "discount_amount"),
                money(rs, "cgst_total"), money(rs, "sgst_total"), money(rs, "igst_total"),
                money(rs, "cess_total"), money(rs, "round_off"), money(rs, "total_amount"));
    }
    private ReportDtos.GstSection gstSection(ResultSet rs) throws SQLException {
        return new ReportDtos.GstSection(rs.getString("category"), rs.getLong("document_count"),
                money(rs, "taxable_value"), money(rs, "cgst"), money(rs, "sgst"), money(rs, "igst"), money(rs, "cess"));
    }
    private ReportDtos.PartyBalance balance(ResultSet rs) throws SQLException {
        BigDecimal invoiced = money(rs, "invoiced"), paid = money(rs, "paid");
        return new ReportDtos.PartyBalance(rs.getLong("id"), rs.getString("name"), invoiced, paid, scale(invoiced.subtract(paid)));
    }
    private MapSqlParameterSource params(Long shopId, LocalDateTime from, LocalDateTime to) {
        return new MapSqlParameterSource().addValue("shopId", shopId).addValue("from", from).addValue("to", to);
    }
    private BigDecimal[] amounts(ResultSet rs, String... names) throws SQLException {
        BigDecimal[] values = new BigDecimal[names.length];
        for (int i=0; i<names.length; i++) values[i] = money(rs, names[i]);
        return values;
    }
    private LocalDateTime timestamp(ResultSet rs, String name) throws SQLException {
        return rs.getTimestamp(name).toLocalDateTime();
    }
    private BigDecimal money(ResultSet rs, String name) throws SQLException { return scale(rs.getBigDecimal(name)); }
    private BigDecimal decimal(ResultSet rs, String name) throws SQLException { return nz(rs.getBigDecimal(name)); }
    private BigDecimal scale(BigDecimal value) { return nz(value).setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private Integer nullableInt(ResultSet rs, String name) throws SQLException { int value=rs.getInt(name); return rs.wasNull()?null:value; }
}
