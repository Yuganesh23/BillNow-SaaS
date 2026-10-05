package com.example.Billing.purchase;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
@Data
public class PurchaseRequest_Dto {
    private Long supplierId;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private String paymentMethod;
    private String paymentStatus; // PAID, PENDING, PARTIAL
    private BigDecimal amountPaid; // If partial or paid, how much?
    private List<PurchaseItemRequest_Dto> items;
}
