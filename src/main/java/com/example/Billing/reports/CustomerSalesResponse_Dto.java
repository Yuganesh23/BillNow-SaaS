package com.example.Billing.reports;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSalesResponse_Dto {

    private Long customerId;

    private String customerName;

    private String whatsappNumber;

    private Long invoiceCount;

    private BigDecimal totalPurchase;
}