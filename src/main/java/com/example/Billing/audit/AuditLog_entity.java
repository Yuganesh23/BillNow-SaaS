package com.example.Billing.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_shop_created", columnList = "shop_id,created_at"),
        @Index(name = "idx_audit_actor", columnList = "actor_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog_entity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "shop_id") private Long shopId;
    @Column(name = "actor_user_id") private Long actorUserId;
    @Column(nullable = false, length = 10) private String method;
    @Column(nullable = false, length = 500) private String path;
    @Column(nullable = false) private int status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
