package com.example.Billing.auth;

import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User_entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, length = 150)
    private String name;


    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;


    @Column(nullable = false)
    @JsonIgnore
    private String password;


    @Column(nullable = false, length = 50)
    private String role;


    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shop_id",nullable = true)
    @JsonIgnore
    private Shop_entity shop;

}