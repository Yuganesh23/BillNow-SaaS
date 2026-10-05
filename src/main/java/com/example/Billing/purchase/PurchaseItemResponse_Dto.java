package com.example.Billing.purchase;
import lombok.*;
import java.math.BigDecimal;
@Data
@Builder
public class PurchaseItemResponse_Dto {
    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private String hsnCode;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal totalPrice;
}
