package com.example.Billing.reports;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSalesResponse_Dto {

    private Long productId;

    private String productName;

    private String sku;

    private Long quantitySold;

    private BigDecimal revenue;
}