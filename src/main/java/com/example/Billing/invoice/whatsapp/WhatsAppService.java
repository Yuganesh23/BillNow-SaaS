package com.example.Billing.invoice.whatsapp;

import com.example.Billing.auth.User_entity;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.invoice.pdf.InvoicePdfService;
import com.example.Billing.shop.ShopWhatsAppConfig;
import com.example.Billing.shop.ShopWhatsAppConfigRepository;
import com.example.Billing.subscription.SubscriptionEntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final InvoiceRepository invoiceRepository;
    private final ShopContextResolver shopContextResolver;
    private final InvoicePdfService invoicePdfService;
    private final ShopWhatsAppConfigRepository configRepository;
    private final WhatsAppStatusService whatsappStatusService;
    private final SubscriptionEntitlementService entitlementService;

    // =====================================================
    // PROVIDER SELECTION
    // =====================================================

    @Value("${whatsapp.provider:msg91}")
    private String provider;

    // =====================================================
    // MSG91 WHATSAPP CONFIG
    // =====================================================

    @Value("${msg91.whatsapp.api-url:https://api.msg91.com/api/v5/whatsapp/whatsapp-outbound-message/bulk/}")
    private String msg91ApiUrl;

    @Value("${msg91.whatsapp.authkey:562890AIne6oBFlfL6a9682ecP1}")
    private String msg91AuthKey;

    @Value("${msg91.whatsapp.integrated-number:15553413072}")
    private String msg91IntegratedNumber;

    @Value("${msg91.whatsapp.sender-number:9080824626}")
    private String msg91SenderNumber;

    @Value("${msg91.whatsapp.namespace:f2e44b63_7a0b_4633_9ef1_6f732c68c329}")
    private String msg91Namespace;

    @Value("${msg91.whatsapp.template-name:invoice_pdf}")
    private String msg91TemplateName;

    @Value("${msg91.whatsapp.language:en}")
    private String msg91Language;

    // =====================================================
    // META WHATSAPP CONFIG (LEGACY FALLBACK)
    // =====================================================

    @Value("${whatsapp.api-url:https://graph.facebook.com}")
    private String metaApiUrl;

    @Value("${whatsapp.api-version:v23.0}")
    private String metaApiVersion;

    @Value("${whatsapp.default-country-code:91}")
    private String defaultCountryCode;

    private RestClient getRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder().requestFactory(factory).build();
    }

    // =====================================================
    // SEND INVOICE PDF
    // =====================================================

    public void sendInvoicePdf(
            User_entity user,
            Long invoiceId
    ) {
        if (user == null) {
            throw new RuntimeException("User is not authenticated");
        }

        var shop = shopContextResolver.resolveActiveShop(user);
        if (shop == null || shop.getId() == null) {
            throw new RuntimeException("User is not associated with a valid shop");
        }

        // Enforce subscription entitlement: WhatsApp is available on PRO and ENTERPRISE plans
        entitlementService.assertCanUseWhatsApp(shop);

        Long shopId = shop.getId();

        Invoice_entity invoice = invoiceRepository
                .findByIdAndShopId(invoiceId, shopId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (invoice.getWhatsappStatus() == WhatsAppStatus.SENDING) {
            throw new RuntimeException("WhatsApp message is already being sent");
        }

        if (invoice.getCustomer() == null) {
            throw new RuntimeException("Invoice customer is missing");
        }

        String whatsappNumber = normalizePhoneNumber(invoice.getCustomer().getWhatsappNumber());
        if (whatsappNumber == null || whatsappNumber.isBlank()) {
            throw new RuntimeException("Customer WhatsApp number is missing");
        }

        ShopWhatsAppConfig config = configRepository.findByShopId(shopId).orElse(null);
        if (config != null && !config.isEnabled()) {
            throw new RuntimeException("WhatsApp sending is disabled for this shop");
        }

        whatsappStatusService.markSending(invoiceId);

        try {
            byte[] pdf = invoicePdfService.generateInvoicePdf(user, invoiceId);
            if (pdf == null || pdf.length == 0) {
                throw new RuntimeException("Invoice PDF generation failed");
            }

            if ("meta".equalsIgnoreCase(provider)) {
                sendViaMeta(invoice, whatsappNumber, pdf, config);
            } else {
                sendViaMsg91(invoice, whatsappNumber, pdf, config);
            }

            whatsappStatusService.markSent(invoiceId);
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null || message.isBlank()) {
                message = e.getClass().getSimpleName();
            }

            whatsappStatusService.markFailed(invoiceId, message);
            throw new RuntimeException("Invoice exists, but WhatsApp sending failed: " + message, e);
        }
    }

    // =====================================================
    // SEND VIA MSG91
    // =====================================================

    private void sendViaMsg91(
            Invoice_entity invoice,
            String whatsappNumber,
            byte[] pdf,
            ShopWhatsAppConfig config
    ) {
        String authKey = msg91AuthKey;
        String integratedNumber = msg91IntegratedNumber;

        // Check if shop has custom credentials stored
        if (config != null) {
            if (config.getAccessToken() != null && !config.getAccessToken().isBlank()) {
                authKey = config.getAccessToken().trim();
            }
            if (config.getPhoneNumberId() != null && !config.getPhoneNumberId().isBlank()) {
                integratedNumber = config.getPhoneNumberId().trim();
            }
        }

        if (authKey == null || authKey.isBlank()) {
            throw new RuntimeException("MSG91 AuthKey is not configured");
        }
        if (integratedNumber == null || integratedNumber.isBlank()) {
            throw new RuntimeException("MSG91 Integrated Number is not configured");
        }

        String safeInvoiceNumber = (invoice.getInvoiceNumber() != null && !invoice.getInvoiceNumber().isBlank())
                ? invoice.getInvoiceNumber()
                : "INV-" + invoice.getId();
        String fileName = safeInvoiceNumber.replaceAll("[^A-Za-z0-9._-]", "-") + ".pdf";

        String customerName = (invoice.getCustomer() != null && invoice.getCustomer().getName() != null && !invoice.getCustomer().getName().isBlank())
                ? invoice.getCustomer().getName().trim()
                : "Valued Customer";

        String shopName = (invoice.getShop() != null && invoice.getShop().getName() != null && !invoice.getShop().getName().isBlank())
                ? invoice.getShop().getName().trim()
                : "XenBill Store";

        BigDecimal total = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        String formattedAmount = "₹" + total.setScale(2, RoundingMode.HALF_UP).toPlainString();

        String pdfBase64 = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdf);

        Map<String, Object> header1 = Map.of(
                "filename", fileName,
                "type", "document",
                "value", pdfBase64
        );

        Map<String, Object> body1 = Map.of("type", "text", "value", customerName);
        Map<String, Object> body2 = Map.of("type", "text", "value", shopName);
        Map<String, Object> body3 = Map.of("type", "text", "value", safeInvoiceNumber);
        Map<String, Object> body4 = Map.of("type", "text", "value", formattedAmount);

        Map<String, Object> components = Map.of(
                "header_1", header1,
                "body_1", body1,
                "body_2", body2,
                "body_3", body3,
                "body_4", body4
        );

        Map<String, Object> toAndComponent = Map.of(
                "to", List.of(whatsappNumber),
                "components", components
        );

        Map<String, Object> template = Map.of(
                "name", msg91TemplateName,
                "language", Map.of(
                        "code", msg91Language,
                        "policy", "deterministic"
                ),
                "namespace", msg91Namespace,
                "to_and_components", List.of(toAndComponent)
        );

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "type", "template",
                "template", template
        );

        Map<String, Object> requestBody = Map.of(
                "integrated_number", integratedNumber,
                "content_type", "template",
                "payload", payload
        );

        log.info("Sending invoice #{} via MSG91 WhatsApp to {} (sender: {})",
                safeInvoiceNumber, whatsappNumber, integratedNumber);

        try {
            var response = getRestClient().post()
                    .uri(msg91ApiUrl)
                    .header("authkey", authKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);

            log.info("MSG91 response status {}: {}", response.getStatusCode(), response.getBody());
            if (response.getStatusCode().isError()) {
                throw new RuntimeException("MSG91 returned HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message via MSG91: {}", e.getMessage(), e);
            throw new RuntimeException("MSG91 delivery failed: " + e.getMessage(), e);
        }
    }

    // =====================================================
    // SEND VIA META GRAPH API (LEGACY FALLBACK)
    // =====================================================

    private void sendViaMeta(
            Invoice_entity invoice,
            String whatsappNumber,
            byte[] pdf,
            ShopWhatsAppConfig config
    ) {
        if (config == null) {
            throw new RuntimeException("WhatsApp Meta Cloud API is not configured for this shop");
        }

        String phoneNumberId = config.getPhoneNumberId();
        String accessToken = config.getAccessToken();

        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            throw new RuntimeException("WhatsApp Phone Number ID is missing");
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new RuntimeException("WhatsApp access token is missing");
        }

        String fileName = invoice.getInvoiceNumber() + ".pdf";
        String mediaId = uploadPdfToMeta(pdf, fileName, phoneNumberId, accessToken);
        sendMetaDocument(whatsappNumber, mediaId, fileName, invoice.getInvoiceNumber(), phoneNumberId, accessToken);
    }

    private String uploadPdfToMeta(
            byte[] pdf,
            String fileName,
            String phoneNumberId,
            String accessToken
    ) {
        String url = metaApiUrl + "/" + metaApiVersion + "/" + phoneNumberId + "/media";

        ByteArrayResource resource = new ByteArrayResource(pdf) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.APPLICATION_PDF);

        HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(resource, fileHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("messaging_product", "whatsapp");
        body.add("file", fileEntity);

        Map<?, ?> response;
        try {
            response = getRestClient().post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload invoice PDF to Meta WhatsApp: " + e.getMessage(), e);
        }

        if (response == null || response.get("id") == null) {
            throw new RuntimeException("Meta WhatsApp media upload failed: " + response);
        }

        return response.get("id").toString();
    }

    private void sendMetaDocument(
            String recipient,
            String mediaId,
            String fileName,
            String invoiceNumber,
            String phoneNumberId,
            String accessToken
    ) {
        String url = metaApiUrl + "/" + metaApiVersion + "/" + phoneNumberId + "/messages";

        Map<String, Object> document = Map.of(
                "id", mediaId,
                "filename", fileName,
                "caption", "Invoice " + invoiceNumber
        );

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", recipient,
                "type", "document",
                "document", document
        );

        try {
            getRestClient().post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send invoice document via Meta WhatsApp: " + e.getMessage(), e);
        }
    }

    // =====================================================
    // NORMALIZE PHONE NUMBER
    // =====================================================

    private String normalizePhoneNumber(String number) {
        if (number == null || number.isBlank()) {
            return null;
        }

        String digits = number.replaceAll("\\D", "");

        if (digits.length() == 10) {
            return defaultCountryCode + digits;
        }

        if (digits.length() == 11 && digits.startsWith("0")) {
            return defaultCountryCode + digits.substring(1);
        }

        return digits;
    }
}
