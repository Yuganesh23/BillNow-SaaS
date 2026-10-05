package com.example.Billing.supplier;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@Builder
public class SupplierLedgerResponse_Dto {
    private Long id;
    private LocalDateTime transactionDate;
    private String type;
    private String reference;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal runningBalance;
    private String notes;
    private String productNames;
}
