package com.example.Billing.compliance;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.gst.GstinValidator;
import com.example.Billing.invoice.*;
import com.example.Billing.product.Product_entity;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EInvoiceService {
    private final EInvoiceRecordRepository records;
    private final InvoiceRepository invoices;
    private final ShopRepository shops;
    private final ShopContextResolver shopContext;
    private final EInvoiceProvider provider;
    private final ObjectMapper objectMapper;

    @Value("${compliance.mock-enabled:true}") private boolean mockEnabled;

    @Transactional(readOnly = true)
    public Settings settings(User_entity user) { return settings(shopContext.resolveActiveShop(user)); }

    @Transactional
    public Settings saveSettings(User_entity user, Settings request) {
        Shop_entity shop = shops.findByIdForUpdate(shopContext.resolveActiveShopId(user))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SHOP_NOT_FOUND", "Shop not found"));
        if (request == null || request.cancellationWindowHours() < 1 || request.cancellationWindowHours() > 168)
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_CANCELLATION_WINDOW", "Cancellation window must be between 1 and 168 hours");
        if ((request.eInvoiceEnabled() || request.eWayBillEnabled()) && provider instanceof MockEInvoiceProvider && !mockEnabled)
            throw new AppException(HttpStatus.CONFLICT, "COMPLIANCE_PROVIDER_REQUIRED", "Configure an approved sandbox or production provider before enabling compliance features");
        shop.setEInvoiceEnabled(request.eInvoiceEnabled());
        shop.setEWayBillEnabled(request.eWayBillEnabled());
        shop.setEInvoiceCancellationWindowHours(request.cancellationWindowHours());
        return settings(shops.save(shop));
    }

    @Transactional
    public Response generate(User_entity user, Long invoiceId) {
        Long shopId = shopContext.resolveActiveShopId(user);
        Shop_entity shop = shopContext.resolveActiveShop(user);
        if (!shop.isEInvoiceEnabled())
            throw new AppException(HttpStatus.CONFLICT, "E_INVOICE_DISABLED", "E-invoicing is disabled for this shop");
        Invoice_entity invoice = invoices.findByIdAndShopIdForUpdate(invoiceId, shopId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found"));
        Optional<EInvoiceRecord> existing = records.findByInvoiceIdAndShopId(invoiceId, shopId);
        if (existing.isPresent() && existing.get().getStatus() == ComplianceStatus.GENERATED) return map(existing.get());
        Map<String, Object> payload = payload(invoice, shop);
        String requestJson = json(payload);
        EInvoiceRecord record = existing.orElseGet(() -> EInvoiceRecord.builder().shop(shop).invoice(invoice).build());
        record.setStatus(ComplianceStatus.PENDING); record.setRequestPayload(requestJson);
        record.setAttemptCount(record.getAttemptCount() == null ? 1 : record.getAttemptCount() + 1);
        record.setLastError(null); records.save(record);
        try {
            EInvoiceProvider.Result result = provider.generate("invoice:" + shopId + ":" + invoiceId, payload);
            record.setProviderReference(result.providerReference()); record.setIrn(result.irn());
            record.setAcknowledgementNumber(result.acknowledgementNumber()); record.setAcknowledgementDate(result.acknowledgementDate());
            record.setSignedQrData(result.signedQrData()); record.setResponsePayload(json(result.rawResponse()));
            record.setGeneratedAt(LocalDateTime.now()); record.setStatus(ComplianceStatus.GENERATED);
            invoice.setComplianceLocked(true); invoices.save(invoice);
        } catch (RuntimeException exception) {
            record.setStatus(ComplianceStatus.FAILED); record.setLastError("Provider request failed; retry from the failed-jobs screen");
        }
        return map(records.save(record));
    }

    @Transactional
    public Response cancel(User_entity user, Long invoiceId, String reason) {
        Long shopId=shopContext.resolveActiveShopId(user); Shop_entity shop=shopContext.resolveActiveShop(user);
        EInvoiceRecord record=records.findByInvoiceIdAndShopId(invoiceId,shopId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,"E_INVOICE_NOT_FOUND","E-invoice record not found"));
        if(record.getStatus()!=ComplianceStatus.GENERATED)
            throw new AppException(HttpStatus.CONFLICT,"E_INVOICE_NOT_ACTIVE","Only a generated e-invoice can be cancelled");
        LocalDateTime deadline=record.getAcknowledgementDate().plusHours(shop.getEInvoiceCancellationWindowHours());
        if(LocalDateTime.now().isAfter(deadline))
            throw new AppException(HttpStatus.CONFLICT,"CANCELLATION_WINDOW_EXPIRED","The configured e-invoice cancellation window has expired");
        if(reason==null||reason.isBlank()||reason.length()>500)
            throw new AppException(HttpStatus.BAD_REQUEST,"CANCELLATION_REASON_REQUIRED","A cancellation reason of at most 500 characters is required");
        try {
            EInvoiceProvider.Cancellation result=provider.cancel(record.getIrn(),reason.trim());
            record.setCancelledAt(result.cancelledAt()); record.setResponsePayload(json(result.rawResponse()));
            record.setStatus(ComplianceStatus.CANCELLED); return map(records.save(record));
        } catch (RuntimeException exception) {
            throw new AppException(HttpStatus.BAD_GATEWAY,"COMPLIANCE_PROVIDER_UNAVAILABLE","The compliance provider could not cancel the e-invoice; try again later");
        }
    }

    @Transactional(readOnly = true)
    public List<Response> jobs(User_entity user) {
        return records.findTop100ByShopIdOrderByUpdatedAtDesc(shopContext.resolveActiveShopId(user)).stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Response> forInvoice(User_entity user, Long invoiceId) {
        return records.findByInvoiceIdAndShopId(invoiceId, shopContext.resolveActiveShopId(user)).map(this::map);
    }

    private Map<String,Object> payload(Invoice_entity invoice, Shop_entity shop) {
        List<String> errors=new ArrayList<>();
        require(GstinValidator.isValid(shop.getGstin()),errors,"Seller GSTIN is missing or invalid");
        require(text(shop.getLegalName(),shop.getName()),errors,"Seller legal name is required");
        require(shop.getAddress(),errors,"Seller address is required"); require(shop.getLocality(),errors,"Seller locality is required");
        requirePin(shop.getPincode(),errors,"Seller PIN code is required"); require(shop.getStateCode(),errors,"Seller state code is required");
        var customer=invoice.getCustomer(); require(GstinValidator.isValid(customer.getGstin()),errors,"Buyer GSTIN is missing or invalid");
        require(customer.getName(),errors,"Buyer legal name is required"); require(text(customer.getBillingAddress(),customer.getAddress()),errors,"Buyer address is required");
        require(customer.getLocality(),errors,"Buyer locality is required"); requirePin(customer.getPincode(),errors,"Buyer PIN code is required");
        require(customer.getStateCode(),errors,"Buyer state code is required");
        for(InvoiceItem item:invoice.getItems()) { Product_entity product=item.getProduct(); require(product.getHsnCode(),errors,"HSN is required for "+product.getName()); }
        if(invoice.getStatus()==InvoiceStatus.CANCELLED) errors.add("Cancelled invoices cannot be submitted");
        if(!errors.isEmpty()) throw new AppException(HttpStatus.BAD_REQUEST,"E_INVOICE_VALIDATION_FAILED",String.join("; ",errors));
        Map<String,Object> transaction=Map.of("TaxSch","GST","SupTyp","B2B","RegRev",invoice.isReverseCharge()?"Y":"N");
        Map<String,Object> document=Map.of("Typ","INV","No",invoice.getInvoiceNumber(),"Dt",invoice.getCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        Map<String,Object> seller=party(shop.getGstin(),text(shop.getLegalName(),shop.getName()),shop.getName(),shop.getAddress(),shop.getLocality(),shop.getPincode(),shop.getStateCode());
        Map<String,Object> buyer=party(customer.getGstin(),customer.getName(),customer.getName(),text(customer.getBillingAddress(),customer.getAddress()),customer.getLocality(),customer.getPincode(),customer.getStateCode());
        List<Map<String,Object>> items=new ArrayList<>(); int line=1;
        for(InvoiceItem item:invoice.getItems()) items.add(new LinkedHashMap<>(Map.ofEntries(
                Map.entry("SlNo",String.valueOf(line++)),Map.entry("PrdDesc",item.getProduct().getName()),Map.entry("IsServc","N"),
                Map.entry("HsnCd",item.getProduct().getHsnCode()),Map.entry("Qty",BigDecimal.valueOf(item.getQuantity())),Map.entry("Unit",item.getProduct().getUqc()),
                Map.entry("UnitPrice",item.getUnitPrice()),Map.entry("TotAmt",item.getTaxableAmount().add(item.getLineDiscount())),Map.entry("Discount",item.getLineDiscount()),
                Map.entry("AssAmt",item.getTaxableAmount()),Map.entry("GstRt",item.getGstRate()),Map.entry("CgstAmt",item.getCgst()),Map.entry("SgstAmt",item.getSgst()),
                Map.entry("IgstAmt",item.getIgst()),Map.entry("CesAmt",item.getCess()),Map.entry("TotItemVal",item.getTotalPrice()))));
        Map<String,Object> values=Map.of("AssVal",invoice.getTaxableAmount(),"CgstVal",invoice.getCgstTotal(),"SgstVal",invoice.getSgstTotal(),
                "IgstVal",invoice.getIgstTotal(),"CesVal",invoice.getCessTotal(),"Discount",invoice.getDiscountAmount(),"RndOffAmt",invoice.getRoundOff(),"TotInvVal",invoice.getTotalAmount());
        return new LinkedHashMap<>(Map.of("Version","1.1","TranDtls",transaction,"DocDtls",document,"SellerDtls",seller,"BuyerDtls",buyer,"ItemList",items,"ValDtls",values));
    }

    private Map<String,Object> party(String gstin,String legal,String trade,String address,String locality,String pin,String state) {
        return new LinkedHashMap<>(Map.of("Gstin",gstin,"LglNm",legal,"TrdNm",trade,"Addr1",address,"Loc",locality,"Pin",Integer.valueOf(pin),"Stcd",state));
    }
    private void require(String value,List<String> errors,String message){if(value==null||value.isBlank())errors.add(message);}
    private void require(boolean condition,List<String> errors,String message){if(!condition)errors.add(message);}
    private void requirePin(String value,List<String> errors,String message){if(value==null||!value.matches("[1-9][0-9]{5}"))errors.add(message);}
    private String text(String preferred,String fallback){return preferred==null||preferred.isBlank()?fallback:preferred;}
    private String json(Object value){try{return objectMapper.writeValueAsString(value);}catch(JacksonException e){throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,"COMPLIANCE_SERIALIZATION_FAILED","Could not build compliance payload");}}
    private Settings settings(Shop_entity s){return new Settings(s.isEInvoiceEnabled(),s.isEWayBillEnabled(),s.getEInvoiceCancellationWindowHours());}
    private Response map(EInvoiceRecord r){return new Response(r.getInvoice().getId(),r.getStatus(),r.getIrn(),r.getAcknowledgementNumber(),r.getAcknowledgementDate(),r.getSignedQrData(),r.getAttemptCount(),r.getLastError(),r.getGeneratedAt(),r.getCancelledAt());}

    public record Settings(boolean eInvoiceEnabled,boolean eWayBillEnabled,int cancellationWindowHours){}
    public record Response(Long invoiceId,ComplianceStatus status,String irn,String acknowledgementNumber,LocalDateTime acknowledgementDate,
                           String signedQrData,int attemptCount,String lastError,LocalDateTime generatedAt,LocalDateTime cancelledAt){}
}
