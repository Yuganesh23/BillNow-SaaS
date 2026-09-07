package com.example.Billing.invoice;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemResponse_Dto {
    private Long id;

    private Long productId;

    private String productName;

    private String sku;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;
}
