package com.example.Billing.supplier;

import com.example.Billing.auth.User_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<List<SupplierResponse_Dto>> getAllSuppliers(Authentication authentication) {
        return ResponseEntity.ok(supplierService.getAllSuppliers((User_entity) authentication.getPrincipal()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse_Dto> getSupplierById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(supplierService.getSupplierById((User_entity) authentication.getPrincipal(), id));
    }

    @PostMapping
    public ResponseEntity<SupplierResponse_Dto> createSupplier(@RequestBody SupplierRequest_Dto request, Authentication authentication) {
        return ResponseEntity.ok(supplierService.createSupplier((User_entity) authentication.getPrincipal(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse_Dto> updateSupplier(@PathVariable Long id, @RequestBody SupplierRequest_Dto request, Authentication authentication) {
        return ResponseEntity.ok(supplierService.updateSupplier((User_entity) authentication.getPrincipal(), id, request));
    }

    @GetMapping("/{id}/ledger")
    public ResponseEntity<List<SupplierLedgerResponse_Dto>> getSupplierLedger(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(supplierService.getLedger((User_entity) authentication.getPrincipal(), id));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<Void> recordPayment(
            @PathVariable Long id, 
            @RequestParam BigDecimal amount, 
            @RequestParam(required = false, defaultValue = "CASH") String paymentMethod,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        supplierService.recordPayment((User_entity) authentication.getPrincipal(), id, amount, paymentMethod, notes);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id, Authentication authentication) {
        supplierService.deleteSupplier((User_entity) authentication.getPrincipal(), id);
        return ResponseEntity.ok().build();
    }
}
