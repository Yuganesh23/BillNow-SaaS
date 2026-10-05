package com.example.Billing.invoice;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemRequest_Dto {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @jakarta.validation.constraints.DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private Double quantity;

    private java.math.BigDecimal taxableAmount;
    private java.math.BigDecimal cgst;
    private java.math.BigDecimal sgst;
    private java.math.BigDecimal igst;
    private java.math.BigDecimal gstRate;
}

