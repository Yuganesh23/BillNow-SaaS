package com.example.Billing.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest_Dto {

    @NotBlank(message = "Tier is required")
    private String tier;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    private BigDecimal amount;
}
