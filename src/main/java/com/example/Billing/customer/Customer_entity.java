package com.example.Billing.customer;

import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "customers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_shop_whatsapp",
                        columnNames = {"shop_id", "whatsappNumber"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer_entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop_entity shop;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 20)
    private String whatsappNumber;

    @Column(length = 255)
    private String email;

    @Column(length = 500)
    private String address;
}