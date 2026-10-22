package com.example.Billing.workflow;
import com.example.Billing.auth.User_entity;
import com.example.Billing.invoice.InvoiceResponse_Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController @RequestMapping("/api/recurring-invoices") @RequiredArgsConstructor
public class RecurringInvoiceController {
    private final RecurringInvoiceService service;
    @PostMapping @PreAuthorize("hasAnyRole('SHOP_OWNER','MANAGER')")
    ResponseEntity<Long> create(Authentication auth, @RequestBody RecurringInvoiceService.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create((User_entity) auth.getPrincipal(), request));
    }
    @PostMapping("/{id}/run") @PreAuthorize("hasAnyRole('SHOP_OWNER','MANAGER')")
    InvoiceResponse_Dto run(Authentication auth, @PathVariable Long id, @RequestParam LocalDate scheduledDate) {
        return service.run((User_entity) auth.getPrincipal(), id, scheduledDate);
    }
}
