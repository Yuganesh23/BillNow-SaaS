package com.example.Billing.reports;

import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "report_export_settings", uniqueConstraints =
        @UniqueConstraint(name = "uk_report_export_settings_shop", columnNames = "shop_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportExportSettings_entity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id", nullable = false) private Shop_entity shop;
    @Column(name = "sales_ledger", nullable = false, length = 100) private String salesLedger;
    @Column(name = "purchase_ledger", nullable = false, length = 100) private String purchaseLedger;
    @Column(name = "cgst_ledger", nullable = false, length = 100) private String cgstLedger;
    @Column(name = "sgst_ledger", nullable = false, length = 100) private String sgstLedger;
    @Column(name = "igst_ledger", nullable = false, length = 100) private String igstLedger;
    @Column(name = "cess_ledger", nullable = false, length = 100) private String cessLedger;
}
