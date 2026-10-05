package com.example.Billing.product;

import java.math.BigDecimal;
import lombok.*;
import com.example.Billing.product.TaxType;



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

    private Double stockQuantity;

    private String attributes;

    private boolean active;

    private Double lowStockThreshold;
    private Long supplierId;
    private String supplierName;
    private String hsnCode;
    private BigDecimal gstRate;
    private TaxType taxType;
}

