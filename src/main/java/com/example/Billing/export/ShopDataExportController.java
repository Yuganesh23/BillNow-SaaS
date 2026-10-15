package com.example.Billing.export;

import com.example.Billing.auth.User_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-export")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SHOP_OWNER')")
public class ShopDataExportController {
    private final ShopDataExportService exports;

    @GetMapping(value = "/shop.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> json(Authentication authentication) {
        return download(exports.json(user(authentication)), exports.filename("json"), MediaType.APPLICATION_JSON);
    }

    @GetMapping(value = "/shop.zip", produces = "application/zip")
    public ResponseEntity<byte[]> zip(Authentication authentication) {
        return download(exports.zip(user(authentication)), exports.filename("zip"), MediaType.parseMediaType("application/zip"));
    }

    private ResponseEntity<byte[]> download(byte[] data, String filename, MediaType type) {
        return ResponseEntity.ok().contentType(type).contentLength(data.length).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(data);
    }

    private User_entity user(Authentication authentication) { return (User_entity) authentication.getPrincipal(); }
}
