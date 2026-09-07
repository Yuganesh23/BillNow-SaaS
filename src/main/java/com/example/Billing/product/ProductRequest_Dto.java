package com.example.Billing.product;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest_Dto {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "SKU is required")
    private String sku;

    private String category;

    private String description;

    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal purchasePrice;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal sellingPrice;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockQuantity;

    private String attributes;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold;
}