package com.example.Billing.inventory;

import com.example.Billing.inventory.StockMovementType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementResponse_Dto {

    private Long id;

    private Long productId;

    private String productName;

    private String sku;

    private StockMovementType movementType;

    private Integer quantity;

    private Integer stockBefore;

    private Integer stockAfter;

    private String reason;

    private LocalDateTime createdAt;
}