package com.example.Billing.export;

import com.example.Billing.auth.User_entity;
import com.example.Billing.auth.userRepository;
import com.example.Billing.common.AppException;
import com.example.Billing.compliance.EInvoiceRecordRepository;
import com.example.Billing.compliance.EWayBillRecordRepository;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.inventory.InventoryRepository;
import com.example.Billing.invoice.DocumentNoteRepository;
import com.example.Billing.invoice.DocumentSequenceRepository;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.payment.PaymentRepository;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.purchase.PurchaseRepository;
import com.example.Billing.reports.ReportExportSettingsRepository;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.supplier.SupplierLedgerRepository;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.workflow.QuotationRepository;
import com.example.Billing.workflow.RecurringInvoiceRepository;
import com.example.Billing.workflow.RecurringInvoiceRunRepository;
import com.example.Billing.workflow.ReminderLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ShopDataExportService {
    private final ShopContextResolver shopContext;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final SupplierRepository suppliers;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final PurchaseRepository purchases;
    private final userRepository users;
    private final InventoryRepository inventoryMovements;
    private final DocumentNoteRepository documentNotes;
    private final DocumentSequenceRepository documentSequences;
    private final SupplierLedgerRepository supplierLedger;
    private final QuotationRepository quotations;
    private final RecurringInvoiceRepository recurringInvoices;
    private final RecurringInvoiceRunRepository recurringInvoiceRuns;
    private final ReminderLogRepository reminderLogs;
    private final EInvoiceRecordRepository eInvoiceRecords;
    private final EWayBillRecordRepository eWayBillRecords;
    private final ReportExportSettingsRepository reportExportSettings;
    private final ObjectMapper objectMapper;

    public ExportBundle export(User_entity user) {
        Shop_entity shop = shopContext.resolveActiveShop(user);
        Long shopId = shop.getId();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("formatVersion", 1);
        data.put("exportedAt", Instant.now().toString());
        data.put("shop", List.of(shopRow(shop)));
        data.put("users", users.findByShopId(shopId).stream().map(this::userRow).toList());
        data.put("products", products.findByShopId(shopId).stream().map(this::productRow).toList());
        data.put("customers", customers.findByShopId(shopId).stream().map(this::customerRow).toList());
        data.put("suppliers", suppliers.findByShopIdOrderByCreatedAtDesc(shopId).stream().map(this::supplierRow).toList());

        List<Map<String, Object>> invoiceRows = new ArrayList<>();
        List<Map<String, Object>> invoiceItemRows = new ArrayList<>();
        invoices.findByShopIdOrderByCreatedAtDesc(shopId).forEach(invoice -> {
            invoiceRows.add(row("id", invoice.getId(), "invoice_number", invoice.getInvoiceNumber(),
                    "customer_id", invoice.getCustomer().getId(), "customer_name", invoice.getCustomer().getName(),
                    "biller_id", invoice.getBiller().getId(), "document_type", invoice.getDocumentType(),
                    "status", invoice.getStatus(), "subtotal", invoice.getSubtotal(), "discount_amount", invoice.getDiscountAmount(),
                    "taxable_amount", invoice.getTaxableAmount(), "cgst_total", invoice.getCgstTotal(), "sgst_total", invoice.getSgstTotal(),
                    "igst_total", invoice.getIgstTotal(), "cess_total", invoice.getCessTotal(), "round_off", invoice.getRoundOff(),
                    "total_amount", invoice.getTotalAmount(), "place_of_supply_code", invoice.getPlaceOfSupplyCode(),
                    "is_inter_state", invoice.getIsInterState(), "reverse_charge", invoice.isReverseCharge(), "created_at", invoice.getCreatedAt()));
            invoice.getItems().forEach(item -> invoiceItemRows.add(row("id", item.getId(), "invoice_id", invoice.getId(),
                    "product_id", item.getProduct().getId(), "product_name", item.getProduct().getName(), "quantity", item.getQuantity(),
                    "unit_price", item.getUnitPrice(), "taxable_amount", item.getTaxableAmount(), "gst_rate", item.getGstRate(),
                    "cgst", item.getCgst(), "sgst", item.getSgst(), "igst", item.getIgst(), "cess", item.getCess(),
                    "line_discount", item.getLineDiscount(), "total_price", item.getTotalPrice())));
        });
        data.put("invoices", invoiceRows);
        data.put("invoiceItems", invoiceItemRows);
        data.put("payments", payments.findByInvoiceShopId(shopId).stream().map(payment -> row("id", payment.getId(),
                "invoice_id", payment.getInvoice().getId(), "invoice_number", payment.getInvoice().getInvoiceNumber(),
                "payment_method", payment.getPaymentMethod(), "amount", payment.getAmount(), "reference", payment.getReference(),
                "notes", payment.getNotes(), "paid_at", payment.getPaidAt())).toList());

        List<Map<String, Object>> purchaseRows = new ArrayList<>();
        List<Map<String, Object>> purchaseItemRows = new ArrayList<>();
        purchases.findByShopIdOrderByCreatedAtDesc(shopId).forEach(purchase -> {
            purchaseRows.add(row("id", purchase.getId(), "purchase_number", purchase.getPurchaseNumber(),
                    "supplier_id", purchase.getSupplier().getId(), "supplier_name", purchase.getSupplier().getBusinessName(),
                    "subtotal", purchase.getSubtotal(), "discount_amount", purchase.getDiscountAmount(), "taxable_amount", purchase.getTaxableAmount(),
                    "cgst_total", purchase.getCgstTotal(), "sgst_total", purchase.getSgstTotal(), "igst_total", purchase.getIgstTotal(),
                    "cess_total", purchase.getCessTotal(), "tax_amount", purchase.getTaxAmount(), "grand_total", purchase.getGrandTotal(),
                    "amount_paid", purchase.getAmountPaid(), "payment_method", purchase.getPaymentMethod(), "status", purchase.getStatus(),
                    "created_at", purchase.getCreatedAt()));
            purchase.getItems().forEach(item -> purchaseItemRows.add(row("id", item.getId(), "purchase_id", purchase.getId(),
                    "product_id", item.getProduct().getId(), "product_name", item.getProduct().getName(), "quantity", item.getQuantity(),
                    "purchase_price", item.getPurchasePrice(), "total_price", item.getTotalPrice())));
        });
        data.put("purchases", purchaseRows);
        data.put("purchaseItems", purchaseItemRows);

        data.put("inventoryMovements", inventoryMovements.findByShopIdOrderByCreatedAtDesc(shopId).stream()
                .map(movement -> row("id", movement.getId(), "product_id", movement.getProduct().getId(),
                        "product_name", movement.getProduct().getName(), "movement_type", movement.getMovementType(),
                        "quantity", movement.getQuantity(), "stock_before", movement.getStockBefore(),
                        "stock_after", movement.getStockAfter(), "reason", movement.getReason(), "created_at", movement.getCreatedAt()))
                .toList());
        data.put("documentNotes", documentNotes.findByShopIdOrderByCreatedAtDesc(shopId).stream()
                .map(note -> row("id", note.getId(), "original_invoice_id", note.getOriginalInvoice().getId(),
                        "note_number", note.getNoteNumber(), "note_type", note.getNoteType(), "financial_year", note.getFinancialYear(),
                        "series_number", note.getSeriesNumber(), "amount", note.getAmount(), "reason", note.getReason(),
                        "restock", note.isRestock(), "created_at", note.getCreatedAt()))
                .toList());
        data.put("documentSequences", documentSequences.findByShopIdOrderByFinancialYearDescDocumentTypeAsc(shopId).stream()
                .map(sequence -> row("id", sequence.getId(), "financial_year", sequence.getFinancialYear(),
                        "document_type", sequence.getDocumentType(), "last_number", sequence.getLastNumber()))
                .toList());
        data.put("supplierLedger", supplierLedger.findByShopIdOrderByTransactionDateDesc(shopId).stream()
                .map(entry -> row("id", entry.getId(), "supplier_id", entry.getSupplier().getId(),
                        "purchase_id", entry.getPurchase() == null ? null : entry.getPurchase().getId(),
                        "transaction_date", entry.getTransactionDate(), "type", entry.getType(), "debit_amount", entry.getDebitAmount(),
                        "credit_amount", entry.getCreditAmount(), "running_balance", entry.getRunningBalance(), "notes", entry.getNotes()))
                .toList());

        List<Map<String, Object>> quotationRows = new ArrayList<>();
        List<Map<String, Object>> quotationItemRows = new ArrayList<>();
        quotations.findByShopIdOrderByCreatedAtDesc(shopId).forEach(quotation -> {
            quotationRows.add(row("id", quotation.getId(), "quote_number", quotation.getQuoteNumber(),
                    "document_kind", quotation.getDocumentKind(), "status", quotation.getStatus(), "customer_id", quotation.getCustomer().getId(),
                    "created_by_id", quotation.getCreatedBy().getId(), "valid_until", quotation.getValidUntil(),
                    "taxable_amount", quotation.getTaxableAmount(), "discount_amount", quotation.getDiscountAmount(),
                    "cgst_total", quotation.getCgstTotal(), "sgst_total", quotation.getSgstTotal(), "igst_total", quotation.getIgstTotal(),
                    "cess_total", quotation.getCessTotal(), "round_off", quotation.getRoundOff(), "total_amount", quotation.getTotalAmount(),
                    "converted_invoice_id", quotation.getConvertedInvoice() == null ? null : quotation.getConvertedInvoice().getId(),
                    "created_at", quotation.getCreatedAt()));
            quotation.getItems().forEach(item -> quotationItemRows.add(row("id", item.getId(), "quotation_id", quotation.getId(),
                    "product_id", item.getProduct().getId(), "quantity", item.getQuantity(), "unit_price", item.getUnitPrice(),
                    "line_discount", item.getLineDiscount(), "taxable_amount", item.getTaxableAmount(), "gst_rate", item.getGstRate(),
                    "cgst", item.getCgst(), "sgst", item.getSgst(), "igst", item.getIgst(), "cess", item.getCess(),
                    "total_amount", item.getTotalAmount())));
        });
        data.put("quotations", quotationRows);
        data.put("quotationItems", quotationItemRows);

        List<Map<String, Object>> recurringRows = new ArrayList<>();
        List<Map<String, Object>> recurringItemRows = new ArrayList<>();
        recurringInvoices.findByShopIdOrderByIdDesc(shopId).forEach(recurring -> {
            recurringRows.add(row("id", recurring.getId(), "customer_id", recurring.getCustomer().getId(),
                    "created_by_id", recurring.getCreatedBy().getId(), "frequency", recurring.getFrequency(),
                    "next_run_date", recurring.getNextRunDate(), "issue_automatically", recurring.isIssueAutomatically(),
                    "active", recurring.isActive(), "discount_amount", recurring.getDiscountAmount()));
            recurring.getItems().forEach(item -> recurringItemRows.add(row("id", item.getId(),
                    "recurring_invoice_id", recurring.getId(), "product_id", item.getProduct().getId(),
                    "quantity", item.getQuantity(), "line_discount", item.getLineDiscount())));
        });
        data.put("recurringInvoices", recurringRows);
        data.put("recurringInvoiceItems", recurringItemRows);
        data.put("recurringInvoiceRuns", recurringInvoiceRuns.findByRecurringInvoiceShopIdOrderByCreatedAtDesc(shopId).stream()
                .map(run -> row("id", run.getId(), "recurring_invoice_id", run.getRecurringInvoice().getId(),
                        "scheduled_date", run.getScheduledDate(), "invoice_id", run.getInvoice().getId(), "created_at", run.getCreatedAt()))
                .toList());
        data.put("reminderLogs", reminderLogs.findByShopIdOrderBySentAtDesc(shopId).stream()
                .map(log -> row("id", log.getId(), "customer_id", log.getCustomer().getId(), "invoice_id", log.getInvoice().getId(),
                        "channel", log.getChannel(), "reminder_key", log.getReminderKey(), "sent_at", log.getSentAt()))
                .toList());

        data.put("eInvoiceRecords", eInvoiceRecords.findByShopIdOrderByUpdatedAtDesc(shopId).stream()
                .map(record -> row("id", record.getId(), "invoice_id", record.getInvoice().getId(), "status", record.getStatus(),
                        "provider_reference", record.getProviderReference(), "irn", record.getIrn(),
                        "acknowledgement_number", record.getAcknowledgementNumber(), "acknowledgement_date", record.getAcknowledgementDate(),
                        "signed_qr_data", record.getSignedQrData(), "last_error", record.getLastError(), "attempt_count", record.getAttemptCount(),
                        "generated_at", record.getGeneratedAt(), "cancelled_at", record.getCancelledAt(),
                        "created_at", record.getCreatedAt(), "updated_at", record.getUpdatedAt()))
                .toList());
        data.put("eWayBillRecords", eWayBillRecords.findByShopIdOrderByUpdatedAtDesc(shopId).stream()
                .map(record -> row("id", record.getId(), "invoice_id", record.getInvoice().getId(), "status", record.getStatus(),
                        "e_way_bill_number", record.getEWayBillNumber(), "valid_until", record.getValidUntil(),
                        "transporter_id", record.getTransporterId(), "transporter_name", record.getTransporterName(),
                        "transport_document_number", record.getTransportDocumentNumber(), "transport_document_date", record.getTransportDocumentDate(),
                        "transport_mode", record.getTransportMode(), "vehicle_number", record.getVehicleNumber(), "vehicle_type", record.getVehicleType(),
                        "distance_km", record.getDistanceKm(), "last_error", record.getLastError(), "attempt_count", record.getAttemptCount(),
                        "generated_at", record.getGeneratedAt(), "cancelled_at", record.getCancelledAt(),
                        "created_at", record.getCreatedAt(), "updated_at", record.getUpdatedAt()))
                .toList());
        data.put("reportExportSettings", reportExportSettings.findByShopId(shopId).stream()
                .map(settings -> row("sales_ledger", settings.getSalesLedger(), "purchase_ledger", settings.getPurchaseLedger(),
                        "cgst_ledger", settings.getCgstLedger(), "sgst_ledger", settings.getSgstLedger(),
                        "igst_ledger", settings.getIgstLedger(), "cess_ledger", settings.getCessLedger()))
                .toList());
        return new ExportBundle(shop.getId(), shop.getName(), data);
    }

    public byte[] json(User_entity user) {
        try { return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(export(user).data()); }
        catch (Exception exception) { throw exportFailure(exception); }
    }

    public byte[] zip(User_entity user) {
        ExportBundle bundle = export(user);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            add(zip, "billnow-export.json", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(bundle.data()));
            for (Map.Entry<String, Object> entry : bundle.data().entrySet()) {
                if (entry.getValue() instanceof List<?> list) {
                    @SuppressWarnings("unchecked") List<Map<String, Object>> rows = (List<Map<String, Object>>) list;
                    add(zip, entry.getKey() + ".csv", csv(rows));
                }
            }
            zip.finish();
            return output.toByteArray();
        } catch (Exception exception) { throw exportFailure(exception); }
    }

    private void add(ZipOutputStream zip, String name, byte[] content) throws Exception {
        zip.putNextEntry(new ZipEntry(name)); zip.write(content); zip.closeEntry();
    }

    private byte[] csv(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return new byte[0];
        StringBuilder csv = new StringBuilder("\uFEFF");
        List<String> headers = new ArrayList<>(rows.get(0).keySet());
        csv.append(String.join(",", headers)).append("\r\n");
        rows.forEach(row -> {
            for (int index = 0; index < headers.size(); index++) {
                if (index > 0) csv.append(',');
                csv.append(csvCell(row.get(headers.get(index))));
            }
            csv.append("\r\n");
        });
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private Map<String, Object> shopRow(Shop_entity shop) { return row("id", shop.getId(), "name", shop.getName(),
            "invoice_name", shop.getInvoiceName(), "email", shop.getEmail(), "mobile_number", shop.getMobileNumber(),
            "gstin", shop.getGstin(), "legal_name", shop.getLegalName(), "registration_type", shop.getRegistrationType(),
            "state", shop.getState(), "state_code", shop.getStateCode(), "address", shop.getAddress(), "locality", shop.getLocality(),
            "pincode", shop.getPincode(), "invoice_prefix", shop.getInvoicePrefix(), "credit_note_prefix", shop.getCreditNotePrefix(),
            "debit_note_prefix", shop.getDebitNotePrefix(), "bank_details", shop.getBankDetails(), "invoice_terms", shop.getInvoiceTerms(),
            "upi_id", shop.getUpiId(), "subscription_tier", shop.getSubscriptionTier(), "logo_base64", shop.getLogoBase64(),
            "logo_object_key", shop.getLogoObjectKey()); }
    private Map<String, Object> userRow(User_entity user) { return row("id", user.getId(), "name", user.getName(), "email", user.getEmail(),
            "mobile_number", user.getMobileNumber(), "role", user.getRole(), "active", user.isActive()); }
    private Map<String, Object> productRow(com.example.Billing.product.Product_entity product) { return row("id", product.getId(), "name", product.getName(),
            "sku", product.getSku(), "category", product.getCategory(), "description", product.getDescription(), "purchase_price", product.getPurchasePrice(),
            "selling_price", product.getSellingPrice(), "stock_quantity", product.getStockQuantity(), "low_stock_threshold", product.getLowStockThreshold(),
            "hsn_code", product.getHsnCode(), "uqc", product.getUqc(), "gst_rate", product.getGstRate(), "cess_rate", product.getCessRate(),
            "tax_type", product.getTaxType(), "active", product.isActive()); }
    private Map<String, Object> customerRow(com.example.Billing.customer.Customer_entity customer) { return row("id", customer.getId(), "name", customer.getName(),
            "whatsapp_number", customer.getWhatsappNumber(), "email", customer.getEmail(), "address", customer.getAddress(), "locality", customer.getLocality(),
            "pincode", customer.getPincode(), "gstin", customer.getGstin(), "state", customer.getState(), "state_code", customer.getStateCode(),
            "billing_address", customer.getBillingAddress(), "shipping_address", customer.getShippingAddress(), "credit_balance", customer.getCreditBalance(),
            "created_at", customer.getCreatedAt()); }
    private Map<String, Object> supplierRow(com.example.Billing.supplier.Supplier_entity supplier) { return row("id", supplier.getId(),
            "business_name", supplier.getBusinessName(), "contact_person", supplier.getContactPerson(), "phone", supplier.getPhone(), "whatsapp", supplier.getWhatsapp(),
            "email", supplier.getEmail(), "gstin", supplier.getGstin(), "state", supplier.getState(), "state_code", supplier.getStateCode(), "address", supplier.getAddress(),
            "notes", supplier.getNotes(), "total_purchases", supplier.getTotalPurchases(), "amount_paid", supplier.getAmountPaid(),
            "outstanding_balance", supplier.getOutstandingBalance(), "created_at", supplier.getCreatedAt()); }

    private Map<String, Object> row(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) result.put(String.valueOf(values[index]), values[index + 1]);
        return result;
    }

    private AppException exportFailure(Exception exception) {
        log.error("Unable to create shop data export", exception);
        return new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "SHOP_EXPORT_FAILED",
                "Unable to create the shop data export. Please try again.");
    }

    public String filename(String extension) { return "billnow-shop-export-" + LocalDate.now() + "." + extension; }
    public record ExportBundle(Long shopId, String shopName, Map<String, Object> data) {}
}
