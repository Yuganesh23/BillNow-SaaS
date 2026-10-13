package com.example.Billing.workflow;

import com.example.Billing.auth.User_entity;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.invoice.*;
import com.example.Billing.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor
public class CustomerAgingService {
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final ShopContextResolver shopContext;

    @Transactional(readOnly = true)
    public List<Aging> calculate(User_entity user, LocalDate asOf) {
        Long shopId = shopContext.resolveActiveShopId(user);
        Map<Long, Mutable> totals = new LinkedHashMap<>();
        for (Invoice_entity invoice : invoices.findByShopIdOrderByCreatedAtDesc(shopId)) {
            if (invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.REFUNDED) continue;
            BigDecimal outstanding = invoice.getTotalAmount().subtract(payments.sumByInvoiceAndShop(invoice.getId(), shopId))
                    .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            if (outstanding.signum() == 0) continue;
            Mutable row = totals.computeIfAbsent(invoice.getCustomer().getId(), id -> new Mutable(invoice.getCustomer().getName()));
            long days = Math.max(0, ChronoUnit.DAYS.between(invoice.getCreatedAt().toLocalDate(), asOf));
            if (days <= 30) row.current = row.current.add(outstanding);
            else if (days <= 60) row.days31To60 = row.days31To60.add(outstanding);
            else if (days <= 90) row.days61To90 = row.days61To90.add(outstanding);
            else row.days90Plus = row.days90Plus.add(outstanding);
        }
        return totals.entrySet().stream().map(e -> new Aging(e.getKey(), e.getValue().name, e.getValue().current,
                e.getValue().days31To60, e.getValue().days61To90, e.getValue().days90Plus,
                e.getValue().current.add(e.getValue().days31To60).add(e.getValue().days61To90).add(e.getValue().days90Plus))).toList();
    }
    private static class Mutable { final String name; BigDecimal current=BigDecimal.ZERO, days31To60=BigDecimal.ZERO, days61To90=BigDecimal.ZERO, days90Plus=BigDecimal.ZERO; Mutable(String name){this.name=name;} }
    public record Aging(Long customerId, String customerName, BigDecimal days0To30, BigDecimal days31To60, BigDecimal days61To90, BigDecimal days90Plus, BigDecimal total) {}
}
