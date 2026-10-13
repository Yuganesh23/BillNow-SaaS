package com.example.Billing.workflow;

import com.example.Billing.config.BranchContextHolder;
import com.example.Billing.invoice.*;
import com.example.Billing.invoice.whatsapp.WhatsAppService;
import com.example.Billing.notification.EmailService;
import com.example.Billing.payment.PaymentRepository;
import com.example.Billing.shop.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Component @RequiredArgsConstructor @Slf4j
@ConditionalOnProperty(name = "billing.reminders.enabled", havingValue = "true")
public class ReminderScheduler {
    private final ShopRepository shops; private final InvoiceRepository invoices; private final PaymentRepository payments;
    private final ReminderLogRepository logs; private final EmailService email; private final WhatsAppService whatsapp;

    @Scheduled(cron = "${billing.reminders.cron}")
    @Transactional
    public void sendDueReminders() {
        for (var shop : shops.findAllByActiveTrue()) {
            BranchContextHolder.setBranchId(shop.getId());
            try {
                for (var invoice : invoices.findByShopIdOrderByCreatedAtDesc(shop.getId())) {
                    if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.REFUNDED) continue;
                    BigDecimal due = invoice.getTotalAmount().subtract(payments.sumByInvoiceAndShop(invoice.getId(), shop.getId()));
                    if (due.signum() <= 0 || invoice.getCreatedAt().toLocalDate().isAfter(LocalDate.now().minusDays(1))) continue;
                    String key = invoice.getId() + "-" + LocalDate.now();
                    if (invoice.getCustomer().getEmail() != null && !logs.existsByShopIdAndReminderKeyAndChannel(shop.getId(), key, "EMAIL")) {
                        email.sendPaymentReminder(invoice.getCustomer().getEmail(), invoice.getInvoiceNumber(), due);
                        logs.save(ReminderLog_entity.builder().shop(shop).customer(invoice.getCustomer()).invoice(invoice).channel("EMAIL").reminderKey(key).build());
                    }
                    if (!logs.existsByShopIdAndReminderKeyAndChannel(shop.getId(), key, "WHATSAPP")) {
                        try { whatsapp.sendInvoicePdf(shop.getOwner(), invoice.getId());
                            logs.save(ReminderLog_entity.builder().shop(shop).customer(invoice.getCustomer()).invoice(invoice).channel("WHATSAPP").reminderKey(key).build());
                        } catch (Exception exception) { log.warn("WhatsApp reminder failed for invoice {}", invoice.getId()); }
                    }
                }
            } finally { BranchContextHolder.clear(); }
        }
    }
}
