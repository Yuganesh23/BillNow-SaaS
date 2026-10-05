package com.example.Billing.reports;

import java.math.BigDecimal;

import lombok.*;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSalesResponse_Dto {

    private Long productId;

    private String productName;

    private String sku;

    private Double quantitySold;

    private BigDecimal revenue;
}