package com.example.Billing.invoice.whatsapp;

import com.example.Billing.auth.User_entity;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.invoice.pdf.InvoicePdfService;
import com.example.Billing.shop.ShopWhatsAppConfig;
import com.example.Billing.shop.ShopWhatsAppConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import com.example.Billing.config.ShopContextResolver;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final InvoiceRepository invoiceRepository;
    private final ShopContextResolver shopContextResolver;

    private final InvoicePdfService invoicePdfService;

    private final ShopWhatsAppConfigRepository configRepository;

    private final WhatsAppStatusService whatsappStatusService;


    private final RestClient restClient =
            RestClient.builder().build();


    // =====================================================
    // GLOBAL WHATSAPP CONFIG
    // =====================================================

    @Value("${whatsapp.api-url}")
    private String apiUrl;


    @Value("${whatsapp.api-version}")
    private String apiVersion;


    @Value("${whatsapp.default-country-code:91}")
    private String defaultCountryCode;


    // =====================================================
    // SEND INVOICE PDF
    // =====================================================

    public void sendInvoicePdf(
            User_entity user,
            Long invoiceId
    ) {

        // -------------------------------------------------
        // VALIDATE USER
        // -------------------------------------------------

        if (user == null) {

            throw new RuntimeException(
                    "User is not authenticated"
            );
        }


        // -------------------------------------------------
        // VALIDATE SHOP
        // -------------------------------------------------

        if (shopContextResolver.resolveActiveShop(user) == null ||
                shopContextResolver.resolveActiveShop(user).getId() == null) {

            throw new RuntimeException(
                    "User is not associated with a valid shop"
            );
        }


        Long shopId =
                shopContextResolver.resolveActiveShop(user).getId();


        // -------------------------------------------------
        // FIND INVOICE
        // -------------------------------------------------

        Invoice_entity invoice =
                invoiceRepository
                        .findByIdAndShopId(
                                invoiceId,
                                shopId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invoice not found"
                                )
                        );


        // -------------------------------------------------
        // PREVENT DUPLICATE SEND
        // -------------------------------------------------

        if (invoice.getWhatsappStatus()
                == WhatsAppStatus.SENDING) {

            throw new RuntimeException(
                    "WhatsApp message is already being sent"
            );
        }


        // -------------------------------------------------
        // CUSTOMER
        // -------------------------------------------------

        if (invoice.getCustomer() == null) {

            throw new RuntimeException(
                    "Invoice customer is missing"
            );
        }


        String whatsappNumber =
                normalizePhoneNumber(
                        invoice.getCustomer()
                                .getWhatsappNumber()
                );


        if (whatsappNumber == null ||
                whatsappNumber.isBlank()) {

            throw new RuntimeException(
                    "Customer WhatsApp number is missing"
            );
        }


        // -------------------------------------------------
        // SHOP WHATSAPP CONFIG
        // -------------------------------------------------

        ShopWhatsAppConfig config =
                configRepository
                        .findByShopId(shopId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "WhatsApp is not configured for this shop"
                                )
                        );


        // -------------------------------------------------
        // CHECK ENABLED
        // -------------------------------------------------

        if (!config.isEnabled()) {

            throw new RuntimeException(
                    "WhatsApp sending is disabled for this shop"
            );
        }


        // -------------------------------------------------
        // GET CREDENTIALS
        // -------------------------------------------------

        String phoneNumberId =
                config.getPhoneNumberId();

        String accessToken =
                config.getAccessToken();


        if (phoneNumberId == null ||
                phoneNumberId.isBlank()) {

            throw new RuntimeException(
                    "WhatsApp Phone Number ID is missing"
            );
        }


        if (accessToken == null ||
                accessToken.isBlank()) {

            throw new RuntimeException(
                    "WhatsApp access token is missing"
            );
        }


        // -------------------------------------------------
        // MARK SENDING
        // -------------------------------------------------

        whatsappStatusService.markSending(
                invoiceId
        );


        try {

            // =============================================
            // GENERATE PDF
            // =============================================

            byte[] pdf =
                    invoicePdfService.generateInvoicePdf(
                            user,
                            invoiceId
                    );


            if (pdf == null ||
                    pdf.length == 0) {

                throw new RuntimeException(
                        "Invoice PDF generation failed"
                );
            }


            // =============================================
            // FILE NAME
            // =============================================

            String fileName =
                    invoice.getInvoiceNumber()
                            + ".pdf";


            // =============================================
            // UPLOAD PDF
            // =============================================

            String mediaId =
                    uploadPdf(
                            pdf,
                            fileName,
                            phoneNumberId,
                            accessToken
                    );


            // =============================================
            // SEND PDF
            // =============================================

            sendDocument(
                    whatsappNumber,
                    mediaId,
                    fileName,
                    invoice.getInvoiceNumber(),
                    phoneNumberId,
                    accessToken
            );


            // =============================================
            // MARK SENT
            // =============================================

            whatsappStatusService.markSent(
                    invoiceId
            );

        } catch (Exception e) {

            // =============================================
            // MARK FAILED
            // =============================================

            String message =
                    e.getMessage();

            if (message == null ||
                    message.isBlank()) {

                message =
                        e.getClass()
                                .getSimpleName();
            }


            whatsappStatusService.markFailed(
                    invoiceId,
                    message
            );


            throw new RuntimeException(
                    "Invoice exists, but WhatsApp sending failed: "
                            + message,
                    e
            );
        }
    }


    // =====================================================
    // UPLOAD PDF
    // =====================================================

    private String uploadPdf(
            byte[] pdf,
            String fileName,
            String phoneNumberId,
            String accessToken
    ) {

        String url =
                apiUrl
                        + "/"
                        + apiVersion
                        + "/"
                        + phoneNumberId
                        + "/media";


        // -------------------------------------------------
        // RESOURCE
        // -------------------------------------------------

        ByteArrayResource resource =
                new ByteArrayResource(pdf) {

                    @Override
                    public String getFilename() {

                        return fileName;
                    }
                };


        // -------------------------------------------------
        // FILE HEADERS
        // -------------------------------------------------

        HttpHeaders fileHeaders =
                new HttpHeaders();

        fileHeaders.setContentType(
                MediaType.APPLICATION_PDF
        );


        HttpEntity<ByteArrayResource> fileEntity =
                new HttpEntity<>(
                        resource,
                        fileHeaders
                );


        // -------------------------------------------------
        // MULTIPART BODY
        // -------------------------------------------------

        MultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();


        body.add(
                "messaging_product",
                "whatsapp"
        );


        body.add(
                "file",
                fileEntity
        );


        // -------------------------------------------------
        // REQUEST
        // -------------------------------------------------

        Map<?, ?> response;

        try {

            response =
                    restClient
                            .post()
                            .uri(url)
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + accessToken
                            )
                            .contentType(
                                    MediaType.MULTIPART_FORM_DATA
                            )
                            .body(body)
                            .retrieve()
                            .body(Map.class);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to upload invoice PDF to WhatsApp: "
                            + e.getMessage(),
                    e
            );
        }


        // -------------------------------------------------
        // VALIDATE RESPONSE
        // -------------------------------------------------

        if (response == null) {

            throw new RuntimeException(
                    "WhatsApp media upload returned empty response"
            );
        }


        if (response.get("id") == null) {

            throw new RuntimeException(
                    "WhatsApp media upload failed: "
                            + response
            );
        }


        return response
                .get("id")
                .toString();
    }


    // =====================================================
    // SEND DOCUMENT
    // =====================================================

    private void sendDocument(
            String recipient,
            String mediaId,
            String fileName,
            String invoiceNumber,
            String phoneNumberId,
            String accessToken
    ) {

        String url =
                apiUrl
                        + "/"
                        + apiVersion
                        + "/"
                        + phoneNumberId
                        + "/messages";


        // -------------------------------------------------
        // DOCUMENT
        // -------------------------------------------------

        Map<String, Object> document =
                Map.of(
                        "id",
                        mediaId,

                        "filename",
                        fileName,

                        "caption",
                        "Invoice "
                                + invoiceNumber
                );


        // -------------------------------------------------
        // BODY
        // -------------------------------------------------

        Map<String, Object> body =
                Map.of(
                        "messaging_product",
                        "whatsapp",

                        "recipient_type",
                        "individual",

                        "to",
                        recipient,

                        "type",
                        "document",

                        "document",
                        document
                );


        // -------------------------------------------------
        // REQUEST
        // -------------------------------------------------

        try {

            restClient
                    .post()
                    .uri(url)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + accessToken
                    )
                    .contentType(
                            MediaType.APPLICATION_JSON
                    )
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to send invoice to WhatsApp: "
                            + e.getMessage(),
                    e
            );
        }
    }


    // =====================================================
    // NORMALIZE PHONE NUMBER
    // =====================================================

    private String normalizePhoneNumber(
            String number
    ) {

        if (number == null ||
                number.isBlank()) {

            return null;
        }


        String digits =
                number.replaceAll(
                        "\\D",
                        ""
                );


        // -------------------------------------------------
        // 9876543210
        // -> 919876543210
        // -------------------------------------------------

        if (digits.length() == 10) {

            return defaultCountryCode
                    + digits;
        }


        // -------------------------------------------------
        // 09876543210
        // -> 919876543210
        // -------------------------------------------------

        if (digits.length() == 11 &&
                digits.startsWith("0")) {

            return defaultCountryCode
                    + digits.substring(1);
        }


        // -------------------------------------------------
        // ALREADY INTERNATIONAL
        // -------------------------------------------------

        return digits;
    }
}