package com.example.Billing.compliance;

import com.example.Billing.auth.User_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SHOP_OWNER','MANAGER')")
public class EInvoiceController {
    private final EInvoiceService service;
    @GetMapping("/settings") public EInvoiceService.Settings settings(Authentication auth){return service.settings(user(auth));}
    @PutMapping("/settings") @PreAuthorize("hasRole('SHOP_OWNER')")
    public EInvoiceService.Settings settings(Authentication auth,@RequestBody EInvoiceService.Settings request){return service.saveSettings(user(auth),request);}
    @PostMapping("/invoices/{invoiceId}/generate") public EInvoiceService.Response generate(Authentication auth,@PathVariable Long invoiceId){return service.generate(user(auth),invoiceId);}
    @PostMapping("/invoices/{invoiceId}/cancel") public EInvoiceService.Response cancel(Authentication auth,@PathVariable Long invoiceId,@RequestBody CancelRequest request){return service.cancel(user(auth),invoiceId,request.reason());}
    @GetMapping("/invoices/{invoiceId}") public ResponseEntity<EInvoiceService.Response> forInvoice(Authentication auth,@PathVariable Long invoiceId){return ResponseEntity.of(service.forInvoice(user(auth),invoiceId));}
    @GetMapping("/jobs") public List<EInvoiceService.Response> jobs(Authentication auth){return service.jobs(user(auth));}
    private User_entity user(Authentication auth){return (User_entity)auth.getPrincipal();}
    public record CancelRequest(String reason){}
}
