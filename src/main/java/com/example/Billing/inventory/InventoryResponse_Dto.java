package com.example.Billing.inventory;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse_Dto {

    private Long productId;

    private String productName;

    private String sku;

    private Double stockQuantity;

    private Boolean lowStock;
}