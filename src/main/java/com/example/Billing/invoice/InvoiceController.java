package com.example.Billing.invoice;

import com.example.Billing.auth.User_entity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.Billing.invoice.pdf.InvoicePdfService;
import com.example.Billing.invoice.whatsapp.WhatsAppService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final WhatsAppService whatsAppService;


    // =====================================================
    // CREATE INVOICE
    // =====================================================

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @PostMapping
    public ResponseEntity<InvoiceResponse_Dto> createInvoice(

            Authentication authentication,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,

            @Valid
            @RequestBody
            CreateInvoiceRequest_Dto request

    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        InvoiceResponse_Dto response =
                invoiceService.createInvoice(
                        user,
                        request,
                        idempotencyKey
                );


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =====================================================
    // GET ALL INVOICES
    // =====================================================

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @GetMapping
    public ResponseEntity<List<InvoiceResponse_Dto>>
    getAllInvoices(

            Authentication authentication

    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        List<InvoiceResponse_Dto> invoices =
                invoiceService.getAllInvoices(user);


        return ResponseEntity.ok(invoices);
    }


    // =====================================================
    // GET INVOICE BY ID
    // =====================================================

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse_Dto>
    getInvoice(

            Authentication authentication,

            @PathVariable
            Long invoiceId

    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        InvoiceResponse_Dto invoice =
                invoiceService.getInvoice(
                        user,
                        invoiceId
                );


        return ResponseEntity.ok(invoice);
    }


    // =====================================================
    // CANCEL INVOICE
    // =====================================================

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @PutMapping("/{invoiceId}/cancel")
    public ResponseEntity<InvoiceResponse_Dto>
    cancelInvoice(

            Authentication authentication,

            @PathVariable
            Long invoiceId

    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        InvoiceResponse_Dto invoice =
                invoiceService.cancelInvoice(
                        user,
                        invoiceId
                );


        return ResponseEntity.ok(invoice);
    }
    // =====================================================
// DOWNLOAD / VIEW INVOICE PDF
// OWNER + BILLER
// =====================================================

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @GetMapping("/{invoiceId}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            Authentication authentication,
            @PathVariable Long invoiceId
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);

        byte[] pdf =
                invoicePdfService.generateInvoicePdf(
                        user,
                        invoiceId
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=invoice-"
                                + invoiceId
                                + ".pdf"
                )
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .body(pdf);
    }


// =====================================================
// SEND INVOICE PDF TO WHATSAPP
// OWNER + BILLER
// =====================================================

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @PostMapping("/{invoiceId}/send-whatsapp")
    public ResponseEntity<String> sendInvoiceToWhatsApp(
            Authentication authentication,
            @PathVariable Long invoiceId
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);

        whatsAppService.sendInvoicePdf(
                user,
                invoiceId
        );

        return ResponseEntity.ok(
                "Invoice PDF sent successfully to customer WhatsApp"
        );
    }


    // =====================================================
    // AUTHENTICATED USER
    // =====================================================

    private User_entity getAuthenticatedUser(
            Authentication authentication
    ) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "User is not authenticated"
            );
        }


        Object principal =
                authentication.getPrincipal();


        if (!(principal instanceof User_entity)) {

            throw new RuntimeException(
                    "Invalid authenticated user"
            );
        }


        return (User_entity) principal;
    }
}
