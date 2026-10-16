package com.example.Billing.compliance;

import com.example.Billing.auth.User_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compliance/e-way-bills")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SHOP_OWNER','MANAGER')")
public class EWayBillController {
    private final EWayBillService service;

    @PostMapping("/invoices/{invoiceId}/generate")
    public EWayBillService.Response generate(Authentication auth, @PathVariable Long invoiceId,
                                              @RequestBody EWayBillService.Request request) {
        return service.generate(user(auth), invoiceId, request);
    }

    @PutMapping("/invoices/{invoiceId}/vehicle")
    public EWayBillService.Response updateVehicle(Authentication auth, @PathVariable Long invoiceId,
                                                   @RequestBody EWayBillService.VehicleUpdate request) {
        return service.updateVehicle(user(auth), invoiceId, request);
    }

    @PostMapping("/invoices/{invoiceId}/cancel")
    public EWayBillService.Response cancel(Authentication auth, @PathVariable Long invoiceId,
                                            @RequestBody EInvoiceController.CancelRequest request) {
        return service.cancel(user(auth), invoiceId, request.reason());
    }

    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<EWayBillService.Response> forInvoice(Authentication auth, @PathVariable Long invoiceId) {
        return ResponseEntity.of(service.forInvoice(user(auth), invoiceId));
    }

    @GetMapping("/jobs")
    public List<EWayBillService.Response> jobs(Authentication auth) { return service.jobs(user(auth)); }

    private User_entity user(Authentication auth) { return (User_entity) auth.getPrincipal(); }
}
