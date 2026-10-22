package com.example.Billing.subscription.billing;

import com.example.Billing.common.AppException;
import com.example.Billing.gst.GstinValidator;
import com.example.Billing.invoice.DocumentNumberService;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.subscription.SaaSSubscription_entity;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Records subscription payments and issues BillNow's GST tax invoice for each.
 * GST treatment is configuration (see docs/GST_ASSUMPTIONS.md) pending CA review.
 */
@Slf4j
@Service
public class SaaSBillingService {
    private final SaaSBillingChargeRepository charges;
    private final SaaSInvoiceSequenceRepository sequences;
    private final String sellerLegalName;
    private final String sellerGstin;
    private final String sellerAddress;
    private final String sacCode;
    private final BigDecimal gstRate;
    private final String invoicePrefix;

    public SaaSBillingService(SaaSBillingChargeRepository charges, SaaSInvoiceSequenceRepository sequences,
            @Value("${billing.saas.seller.legal-name:}") String sellerLegalName,
            @Value("${billing.saas.seller.gstin:}") String sellerGstin,
            @Value("${billing.saas.seller.address:}") String sellerAddress,
            @Value("${billing.saas.sac-code:}") String sacCode,
            @Value("${billing.saas.gst-rate:18.00}") BigDecimal gstRate,
            @Value("${billing.saas.invoice-prefix:BN}") String invoicePrefix) {
        this.charges = charges; this.sequences = sequences;
        this.sellerLegalName = sellerLegalName.trim(); this.sellerGstin = sellerGstin.trim().toUpperCase();
        this.sellerAddress = sellerAddress.trim(); this.sacCode = sacCode.trim();
        this.gstRate = gstRate; this.invoicePrefix = invoicePrefix.trim();
        if (!isSellerConfigured()) {
            log.warn("BillNow seller GST details are not configured; subscription payments are recorded "
                    + "but tax invoices are issued only once BILLING_SELLER_* and BILLING_SAAS_SAC_CODE are set");
        }
    }

    /**
     * Records a captured subscription.charged payment once, keyed by Razorpay
     * payment id, and issues its tax invoice when seller details are configured.
     */
    @Transactional
    public void recordCharge(SaaSSubscription_entity subscription, JSONObject payment) {
        String paymentId = payment.optString("id", "");
        if (paymentId.isBlank() || charges.existsByRazorpayPaymentId(paymentId)) return;
        String status = payment.optString("status", "captured");
        if (!"captured".equals(status) && !"authorized".equals(status)) return;
        long amountPaise = payment.optLong("amount", 0);
        if (amountPaise <= 0) return;

        long createdAt = payment.optLong("created_at", 0);
        String invoiceId = payment.optString("invoice_id", null);
        SaaSBillingCharge_entity charge = SaaSBillingCharge_entity.builder()
                .shop(subscription.getShop())
                .subscription(subscription)
                .razorpayPaymentId(paymentId)
                .razorpayInvoiceId(invoiceId == null || invoiceId.isBlank() ? null : invoiceId)
                .plan(subscription.getPlan())
                .paidAt(createdAt > 0 ? toLocal(createdAt) : LocalDateTime.now())
                .periodStart(subscription.getStartDate())
                .periodEnd(subscription.getEndDate())
                .currency(payment.optString("currency", "INR"))
                .totalAmount(BigDecimal.valueOf(amountPaise).movePointLeft(2))
                .build();
        if (isSellerConfigured()) issueInvoice(charge);
        charges.save(charge);
    }

    @Transactional(readOnly = true)
    public List<SaaSBillingCharge_entity> history(Long shopId) {
        return charges.findByShopIdOrderByPaidAtDescIdDesc(shopId);
    }

    /** Returns the charge with its tax invoice, issuing it now if seller details arrived later. */
    @Transactional
    public SaaSBillingCharge_entity invoicedCharge(Long shopId, Long chargeId) {
        SaaSBillingCharge_entity charge = charges.findByIdAndShopId(chargeId, shopId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "CHARGE_NOT_FOUND", "Billing record not found"));
        if (charge.getInvoiceNumber() == null) {
            if (!isSellerConfigured()) {
                throw new AppException(HttpStatus.CONFLICT, "BILLING_ENTITY_NOT_CONFIGURED",
                        "The tax invoice is not available yet. Contact support for a copy.");
            }
            issueInvoice(charge);
            charges.save(charge);
        }
        return charge;
    }

    private void issueInvoice(SaaSBillingCharge_entity charge) {
        Shop_entity shop = charge.getShop();
        String supplierState = GstinValidator.stateCode(sellerGstin);
        String placeOfSupply = placeOfSupply(shop, supplierState);
        SaaSChargeTax tax = SaaSChargeTax.fromInclusive(charge.getTotalAmount(), gstRate, supplierState.equals(placeOfSupply));

        String fy = DocumentNumberService.financialYear(charge.getPaidAt().toLocalDate());
        SaaSInvoiceSequence_entity sequence = sequences.findForUpdate(fy)
                .orElseGet(() -> sequences.saveAndFlush(SaaSInvoiceSequence_entity.builder().financialYear(fy).build()));
        int next = sequence.getLastNumber() + 1;
        String number = invoicePrefix + "/" + fy + "/" + String.format("%05d", next);
        if (number.length() > 16) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "INVOICE_NUMBER_TOO_LONG",
                    "Subscription invoice number exceeds 16 characters; shorten BILLING_SAAS_INVOICE_PREFIX");
        }
        sequence.setLastNumber(next);
        sequences.save(sequence);

        String recipientGstin = GstinValidator.isValid(shop.getGstin()) ? shop.getGstin().trim().toUpperCase() : null;
        charge.setInvoiceNumber(number);
        charge.setFinancialYear(fy);
        charge.setInvoiceIssuedAt(LocalDateTime.now());
        charge.setGstRate(gstRate);
        charge.setTaxableAmount(tax.taxable());
        charge.setCgstAmount(tax.cgst());
        charge.setSgstAmount(tax.sgst());
        charge.setIgstAmount(tax.igst());
        charge.setSacCode(sacCode);
        charge.setSupplierLegalName(sellerLegalName);
        charge.setSupplierGstin(sellerGstin);
        charge.setSupplierAddress(sellerAddress);
        charge.setSupplierStateCode(supplierState);
        charge.setRecipientName(firstNonBlank(shop.getLegalName(), shop.getName()));
        charge.setRecipientGstin(recipientGstin);
        charge.setRecipientAddress(shop.getAddress());
        charge.setPlaceOfSupplyCode(placeOfSupply);
    }

    /*
     * Registered shop: state from its GSTIN. Otherwise its selected state, unless
     * that is the legacy "97" placeholder; with no usable address on record the
     * supplier's state is used.
     */
    static String placeOfSupply(Shop_entity shop, String supplierState) {
        if (GstinValidator.isValid(shop.getGstin())) return GstinValidator.stateCode(shop.getGstin());
        String code = shop.getStateCode();
        if (code != null && code.matches("\\d{2}") && !"97".equals(code)) return code;
        return supplierState;
    }

    public boolean isSellerConfigured() {
        return !sellerLegalName.isBlank() && GstinValidator.isValid(sellerGstin) && !sacCode.isBlank();
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static LocalDateTime toLocal(long epochSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
    }
}
