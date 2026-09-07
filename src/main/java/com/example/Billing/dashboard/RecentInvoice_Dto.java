package com.example.Billing.dashboard;

import com.example.Billing.invoice.InvoiceStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentInvoice_Dto {

    private Long id;

    private String invoiceNumber;

    private Long customerId;

    private String customerName;

    private BigDecimal totalAmount;

    private InvoiceStatus status;

    private LocalDateTime createdAt;
}