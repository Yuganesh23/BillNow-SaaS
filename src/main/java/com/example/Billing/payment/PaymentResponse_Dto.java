package com.example.Billing.payment;

import java.math.BigDecimal;

import lombok.*;


import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse_Dto {

    private Long id;

    private Long invoiceId;

    private String invoiceNumber;

    private Long customerId;

    private String customerName;

    private PaymentMethod paymentMethod;

    private BigDecimal amount;

    private LocalDateTime paidAt;
}