package com.example.Billing.portal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class CustomerPortalController {
    private final CustomerPortalService portalService;

    @GetMapping
    public ResponseEntity<CustomerPortalService.PortalSummary> summary(
            @RequestHeader(name = "X-Portal-Token", required = false) String token) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(portalService.summary(token));
    }

    @GetMapping("/invoices/{invoiceId}/pdf")
    public ResponseEntity<byte[]> invoicePdf(
            @RequestHeader(name = "X-Portal-Token", required = false) String token,
            @PathVariable Long invoiceId) {
        var pdf = portalService.invoicePdf(token, invoiceId);
        String filename = "invoice-" + pdf.invoiceNumber().replaceAll("[^A-Za-z0-9._-]", "-") + ".pdf";
        return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(pdf.bytes());
    }
}
