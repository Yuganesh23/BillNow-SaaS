package com.example.Billing.workflow;
import com.example.Billing.customer.Customer_entity;
import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "reminder_logs", uniqueConstraints = @UniqueConstraint(name = "uk_reminder_delivery", columnNames = {"shop_id", "reminder_key", "channel"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReminderLog_entity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id") private Shop_entity shop;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id") private Customer_entity customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "invoice_id") private Invoice_entity invoice;
    @Column(name = "channel", nullable = false, length = 20) private String channel;
    @Column(name = "reminder_key", nullable = false, length = 100) private String reminderKey;
    @Column(name = "sent_at", nullable = false) private LocalDateTime sentAt;
    @PrePersist void sent() { if (sentAt == null) sentAt = LocalDateTime.now(); }
}
