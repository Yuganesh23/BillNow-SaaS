package com.example.Billing.purchase;
import lombok.*;
import java.math.BigDecimal;
@Data
public class PurchaseItemRequest_Dto {
    private Long productId;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
}
