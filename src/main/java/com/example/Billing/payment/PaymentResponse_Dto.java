package com.example.Billing.payment;

import lombok.*;

import java.math.BigDecimal;
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