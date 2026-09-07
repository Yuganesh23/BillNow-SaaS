package com.example.Billing.invoice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceRequest_Dto {
    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotEmpty(message = "Invoice must contain at least one item")
    @Valid
    private List<InvoiceItemRequest_Dto> items;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal discountAmount;
}
