package com.example.Billing.reports;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReportResponse_Dto {

    private LocalDate from;

    private LocalDate to;

    private long totalInvoices;

    private BigDecimal subtotal;

    private BigDecimal totalDiscount;

    private BigDecimal totalSales;
}