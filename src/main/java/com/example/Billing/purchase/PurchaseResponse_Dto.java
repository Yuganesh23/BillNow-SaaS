package com.example.Billing.purchase;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Data
@Builder
public class PurchaseResponse_Dto {
    private Long id;
    private Long shopId;
    private Long supplierId;
    private String supplierName;
    private String supplierGstin;
    private String purchaseNumber;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal grandTotal;
    private String status;
    private String paymentMethod;
    private BigDecimal amountPaid;
    private LocalDateTime createdAt;
    private String createdBy;
    private List<PurchaseItemResponse_Dto> items;
    private List<PurchasePaymentHistoryDto> payments;
}
