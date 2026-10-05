package com.example.Billing.reports;

import java.math.BigDecimal;

import lombok.*;



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