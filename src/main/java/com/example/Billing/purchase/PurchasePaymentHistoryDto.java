package com.example.Billing.purchase;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PurchasePaymentHistoryDto {
    private LocalDateTime date;
    private BigDecimal amount;
    private String type;
    private String notes;
}