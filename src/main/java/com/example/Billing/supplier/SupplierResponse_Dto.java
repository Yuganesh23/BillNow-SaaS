package com.example.Billing.supplier;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@Builder
public class SupplierResponse_Dto {
    private Long id;
    private Long shopId;
    private String businessName;
    private String contactPerson;
    private String phone;
    private String whatsapp;
    private String email;
    private String gstin;
    private String address;
    private String notes;
    private BigDecimal totalPurchases;
    private BigDecimal amountPaid;
    private BigDecimal outstandingBalance;
    private LocalDateTime createdAt;
}
