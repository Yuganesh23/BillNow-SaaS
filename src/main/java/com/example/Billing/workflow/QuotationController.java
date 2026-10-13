package com.example.Billing.workflow;

import com.example.Billing.auth.User_entity;
import com.example.Billing.invoice.InvoiceResponse_Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/quotations") @RequiredArgsConstructor
public class QuotationController {
    private final QuotationService service;
    @PostMapping @PreAuthorize("hasAnyRole('SHOP_OWNER','MANAGER','BILLER')")
    ResponseEntity<QuotationService.Response> create(Authentication auth, @RequestBody QuotationService.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create((User_entity) auth.getPrincipal(), request));
    }
    @GetMapping @PreAuthorize("hasAnyRole('SHOP_OWNER','MANAGER','BILLER','VIEWER')")
    List<QuotationService.Response> list(Authentication auth) { return service.list((User_entity) auth.getPrincipal()); }
    @PostMapping("/{id}/convert") @PreAuthorize("hasAnyRole('SHOP_OWNER','MANAGER','BILLER')")
    InvoiceResponse_Dto convert(Authentication auth, @PathVariable Long id) { return service.convert((User_entity) auth.getPrincipal(), id); }
}
