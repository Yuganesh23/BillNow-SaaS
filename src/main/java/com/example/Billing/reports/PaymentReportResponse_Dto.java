package com.example.Billing.reports;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReportResponse_Dto {

    private BigDecimal cashAmount;

    private BigDecimal upiAmount;

    private BigDecimal cardAmount;

    private BigDecimal otherAmount;

    private BigDecimal totalAmount;
}