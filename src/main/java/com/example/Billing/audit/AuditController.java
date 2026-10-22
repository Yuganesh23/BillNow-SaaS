package com.example.Billing.audit;
import com.example.Billing.auth.User_entity;
import com.example.Billing.config.ShopContextResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/audit") @RequiredArgsConstructor
public class AuditController {
    private final AuditLogRepository repository;
    private final ShopContextResolver shopContext;
    @GetMapping @PreAuthorize("hasRole('SHOP_OWNER')")
    public List<AuditLog_entity> list(Authentication auth) {
        return repository.findTop200ByShopIdOrderByCreatedAtDesc(shopContext.resolveActiveShopId((User_entity) auth.getPrincipal()));
    }
}
