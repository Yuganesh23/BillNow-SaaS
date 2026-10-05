package com.example.Billing.inventory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustmentRequest_Dto {

    @NotNull(message = "Product ID is required")
    private Long productId;


    @NotNull(message = "Quantity is required")
    private Double quantity;


    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}