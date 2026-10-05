package com.example.Billing.reports;

import java.math.BigDecimal;

import lombok.*;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSalesResponse_Dto {

    private Long customerId;

    private String customerName;

    private String whatsappNumber;
    private String address;

    private Long invoiceCount;

    private BigDecimal totalPurchase;
}