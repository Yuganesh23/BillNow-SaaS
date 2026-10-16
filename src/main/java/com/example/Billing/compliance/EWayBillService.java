package com.example.Billing.compliance;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.gst.GstinValidator;
import com.example.Billing.invoice.InvoiceItem;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.invoice.InvoiceStatus;
import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EWayBillService {
    private static final int CANCELLATION_WINDOW_HOURS = 24;

    private final EWayBillRecordRepository records;
    private final EInvoiceRecordRepository eInvoiceRecords;
    private final InvoiceRepository invoices;
    private final ShopContextResolver shopContext;
    private final EInvoiceProvider provider;
    private final ObjectMapper objectMapper;

    @Transactional
    public Response generate(User_entity user, Long invoiceId, Request request) {
        Long shopId = shopContext.resolveActiveShopId(user);
        Shop_entity shop = shopContext.resolveActiveShop(user);
        if (!shop.isEWayBillEnabled()) {
            throw new AppException(HttpStatus.CONFLICT, "E_WAY_BILL_DISABLED", "E-way billing is disabled for this shop");
        }
        Invoice_entity invoice = invoices.findByIdAndShopIdForUpdate(invoiceId, shopId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found"));
        Optional<EWayBillRecord> existing = records.findByInvoiceIdAndShopId(invoiceId, shopId);
        if (existing.isPresent() && existing.get().getStatus() == ComplianceStatus.GENERATED) {
            return map(existing.get());
        }
        validate(invoice, shop, request);
        Map<String, Object> payload = payload(invoice, shop, request);
        EWayBillRecord record = existing.orElseGet(() -> EWayBillRecord.builder().shop(shop).invoice(invoice).build());
        applyRequest(record, request);
        record.setStatus(ComplianceStatus.PENDING);
        record.setRequestPayload(json(payload));
        record.setAttemptCount(record.getAttemptCount() == null ? 1 : record.getAttemptCount() + 1);
        record.setLastError(null);
        records.save(record);
        try {
            EInvoiceProvider.EWayBillResult result = provider.generateEWayBill("eway:" + shopId + ":" + invoiceId, payload);
            record.setEWayBillNumber(result.eWayBillNumber());
            record.setGeneratedAt(result.generatedAt());
            record.setValidUntil(result.validUntil());
            record.setResponsePayload(json(result.rawResponse()));
            record.setStatus(ComplianceStatus.GENERATED);
        } catch (RuntimeException exception) {
            record.setStatus(ComplianceStatus.FAILED);
            record.setLastError("Provider request failed; retry from the failed-jobs screen");
        }
        return map(records.save(record));
    }

    @Transactional
    public Response updateVehicle(User_entity user, Long invoiceId, VehicleUpdate request) {
        Long shopId = shopContext.resolveActiveShopId(user);
        EWayBillRecord record = records.findByInvoiceIdAndShopId(invoiceId, shopId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "E_WAY_BILL_NOT_FOUND", "E-way bill record not found"));
        if (record.getStatus() != ComplianceStatus.GENERATED) {
            throw new AppException(HttpStatus.CONFLICT, "E_WAY_BILL_NOT_ACTIVE", "Only a generated e-way bill can be updated");
        }
        requireVehicle(request.vehicleNumber(), request.transportMode(), request.reason());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("vehicleNumber", request.vehicleNumber().trim().toUpperCase(Locale.ROOT));
        payload.put("vehicleType", normalized(request.vehicleType(), "REGULAR"));
        payload.put("transportMode", request.transportMode().trim().toUpperCase(Locale.ROOT));
        payload.put("reason", request.reason().trim());
        payload.put("place", blankToNull(request.place()));
        payload.put("stateCode", blankToNull(request.stateCode()));
        EInvoiceProvider.EWayBillResult result;
        try {
            result = provider.updateEWayBillVehicle(record.getEWayBillNumber(), payload);
        } catch (RuntimeException exception) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "COMPLIANCE_PROVIDER_UNAVAILABLE", "The compliance provider could not update the vehicle; try again later");
        }
        record.setVehicleNumber((String) payload.get("vehicleNumber"));
        record.setVehicleType((String) payload.get("vehicleType"));
        record.setTransportMode((String) payload.get("transportMode"));
        record.setValidUntil(result.validUntil());
        record.setResponsePayload(json(result.rawResponse()));
        return map(records.save(record));
    }

    @Transactional
    public Response cancel(User_entity user, Long invoiceId, String reason) {
        Long shopId = shopContext.resolveActiveShopId(user);
        EWayBillRecord record = records.findByInvoiceIdAndShopId(invoiceId, shopId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "E_WAY_BILL_NOT_FOUND", "E-way bill record not found"));
        if (record.getStatus() != ComplianceStatus.GENERATED) {
            throw new AppException(HttpStatus.CONFLICT, "E_WAY_BILL_NOT_ACTIVE", "Only a generated e-way bill can be cancelled");
        }
        if (record.getGeneratedAt() == null || LocalDateTime.now().isAfter(record.getGeneratedAt().plusHours(CANCELLATION_WINDOW_HOURS))) {
            throw new AppException(HttpStatus.CONFLICT, "E_WAY_BILL_CANCELLATION_WINDOW_EXPIRED", "The 24-hour e-way bill cancellation window has expired");
        }
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new AppException(HttpStatus.BAD_REQUEST, "CANCELLATION_REASON_REQUIRED", "A cancellation reason of at most 500 characters is required");
        }
        EInvoiceProvider.Cancellation result;
        try {
            result = provider.cancelEWayBill(record.getEWayBillNumber(), reason.trim());
        } catch (RuntimeException exception) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "COMPLIANCE_PROVIDER_UNAVAILABLE", "The compliance provider could not cancel the e-way bill; try again later");
        }
        record.setCancelledAt(result.cancelledAt());
        record.setResponsePayload(json(result.rawResponse()));
        record.setStatus(ComplianceStatus.CANCELLED);
        return map(records.save(record));
    }

    @Transactional(readOnly = true)
    public Optional<Response> forInvoice(User_entity user, Long invoiceId) {
        return records.findByInvoiceIdAndShopId(invoiceId, shopContext.resolveActiveShopId(user)).map(this::map);
    }

    @Transactional(readOnly = true)
    public List<Response> jobs(User_entity user) {
        return records.findTop100ByShopIdOrderByUpdatedAtDesc(shopContext.resolveActiveShopId(user)).stream().map(this::map).toList();
    }

    private void validate(Invoice_entity invoice, Shop_entity shop, Request request) {
        List<String> errors = new ArrayList<>();
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) errors.add("Cancelled invoices cannot be submitted");
        if (!GstinValidator.isValid(shop.getGstin())) errors.add("Seller GSTIN is missing or invalid");
        if (!GstinValidator.isValid(invoice.getCustomer().getGstin())) errors.add("Buyer GSTIN is missing or invalid");
        if (shop.getPincode() == null || !shop.getPincode().matches("[1-9][0-9]{5}")) errors.add("Seller PIN code is required");
        if (invoice.getCustomer().getPincode() == null || !invoice.getCustomer().getPincode().matches("[1-9][0-9]{5}")) errors.add("Buyer PIN code is required");
        if (request == null) errors.add("Transport details are required");
        else {
            if (request.distanceKm() == null || request.distanceKm() < 0 || request.distanceKm() > 4000) errors.add("Distance must be between 0 and 4000 km");
            if (blank(request.transportMode())) errors.add("Transport mode is required");
            if (blank(request.vehicleNumber()) && blank(request.transporterId()) && blank(request.transportDocumentNumber()))
                errors.add("Provide a vehicle number, transporter ID, or transport document number");
        }
        if (!errors.isEmpty()) throw new AppException(HttpStatus.BAD_REQUEST, "E_WAY_BILL_VALIDATION_FAILED", String.join("; ", errors));
    }

    private Map<String, Object> payload(Invoice_entity invoice, Shop_entity shop, Request request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("documentNumber", invoice.getInvoiceNumber());
        payload.put("documentDate", invoice.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_DATE));
        payload.put("documentType", invoice.getDocumentType());
        payload.put("sellerGstin", shop.getGstin());
        payload.put("sellerPincode", shop.getPincode());
        payload.put("buyerGstin", invoice.getCustomer().getGstin());
        payload.put("buyerPincode", invoice.getCustomer().getPincode());
        payload.put("placeOfSupply", invoice.getPlaceOfSupplyCode());
        payload.put("totalValue", invoice.getTotalAmount());
        payload.put("irn", eInvoiceRecords.findByInvoiceIdAndShopId(invoice.getId(), shop.getId()).map(EInvoiceRecord::getIrn).orElse(null));
        payload.put("transporterId", blankToNull(request.transporterId()));
        payload.put("transporterName", blankToNull(request.transporterName()));
        payload.put("transportDocumentNumber", blankToNull(request.transportDocumentNumber()));
        payload.put("transportDocumentDate", request.transportDocumentDate());
        payload.put("transportMode", request.transportMode().trim().toUpperCase(Locale.ROOT));
        payload.put("vehicleNumber", blankToNull(request.vehicleNumber()) == null ? null : request.vehicleNumber().trim().toUpperCase(Locale.ROOT));
        payload.put("vehicleType", normalized(request.vehicleType(), "REGULAR"));
        payload.put("distanceKm", request.distanceKm());
        payload.put("items", invoice.getItems().stream().map(this::item).toList());
        return payload;
    }

    private Map<String, Object> item(InvoiceItem item) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("name", item.getProduct().getName());
        value.put("hsn", item.getProduct().getHsnCode());
        value.put("quantity", item.getQuantity());
        value.put("taxableValue", item.getTaxableAmount());
        value.put("cgst", item.getCgst()); value.put("sgst", item.getSgst()); value.put("igst", item.getIgst());
        return value;
    }

    private void applyRequest(EWayBillRecord record, Request request) {
        record.setTransporterId(blankToNull(request.transporterId())); record.setTransporterName(blankToNull(request.transporterName()));
        record.setTransportDocumentNumber(blankToNull(request.transportDocumentNumber())); record.setTransportDocumentDate(request.transportDocumentDate());
        record.setTransportMode(request.transportMode().trim().toUpperCase(Locale.ROOT));
        record.setVehicleNumber(blank(request.vehicleNumber()) ? null : request.vehicleNumber().trim().toUpperCase(Locale.ROOT));
        record.setVehicleType(normalized(request.vehicleType(), "REGULAR")); record.setDistanceKm(request.distanceKm());
    }

    private void requireVehicle(String number, String mode, String reason) {
        if (blank(number) || blank(mode) || blank(reason) || reason.length() > 500)
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_VEHICLE_UPDATE", "Vehicle number, transport mode, and a reason of at most 500 characters are required");
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String blankToNull(String value) { return blank(value) ? null : value.trim(); }
    private String normalized(String value, String fallback) { return blank(value) ? fallback : value.trim().toUpperCase(Locale.ROOT); }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JacksonException exception) {
        throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "COMPLIANCE_SERIALIZATION_FAILED", "Could not build compliance payload");
    }}
    private Response map(EWayBillRecord record) { return new Response(record.getInvoice().getId(), record.getStatus(), record.getEWayBillNumber(),
            record.getGeneratedAt(), record.getValidUntil(), record.getTransporterId(), record.getTransporterName(), record.getTransportMode(),
            record.getVehicleNumber(), record.getVehicleType(), record.getDistanceKm(), record.getAttemptCount(), record.getLastError(), record.getCancelledAt()); }

    public record Request(String transporterId, String transporterName, String transportDocumentNumber,
                          LocalDate transportDocumentDate, String transportMode, String vehicleNumber,
                          String vehicleType, Integer distanceKm) {}
    public record VehicleUpdate(String vehicleNumber, String vehicleType, String transportMode,
                                String reason, String place, String stateCode) {}
    public record Response(Long invoiceId, ComplianceStatus status, String eWayBillNumber,
                           LocalDateTime generatedAt, LocalDateTime validUntil, String transporterId,
                           String transporterName, String transportMode, String vehicleNumber,
                           String vehicleType, Integer distanceKm, int attemptCount, String lastError,
                           LocalDateTime cancelledAt) {}
}
