package com.example.Billing.invoice;

import java.math.BigDecimal;
import lombok.*;

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
    private Double quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String hsnCode;
    private BigDecimal taxableAmount;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal igst;
    private BigDecimal gstRate;
}
