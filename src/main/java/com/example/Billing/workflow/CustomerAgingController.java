package com.example.Billing.workflow;
import com.example.Billing.auth.User_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/customer-ledger") @RequiredArgsConstructor
public class CustomerAgingController {
    private final CustomerAgingService service;
    @GetMapping("/aging") @PreAuthorize("hasAnyRole('SHOP_OWNER','MANAGER','VIEWER')")
    List<CustomerAgingService.Aging> aging(Authentication auth, @RequestParam(required = false) LocalDate asOf) {
        return service.calculate((User_entity) auth.getPrincipal(), asOf == null ? LocalDate.now() : asOf);
    }
}
