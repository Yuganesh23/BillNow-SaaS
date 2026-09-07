package com.example.Billing.product;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse_Dto {
    private Long id;

    private Long shopId;

    private String name;

    private String sku;

    private String category;

    private String description;

    private BigDecimal purchasePrice;

    private BigDecimal sellingPrice;

    private Integer stockQuantity;

    private String attributes;

    private boolean active;

    private Integer lowStockThreshold;
}
