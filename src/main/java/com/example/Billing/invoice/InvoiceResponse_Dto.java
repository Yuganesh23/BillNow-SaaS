package com.example.Billing.invoice;

import com.example.Billing.invoice.whatsapp.WhatsAppStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse_Dto {

    private Long id;

    private Long shopId;

    private Long customerId;

    private String customerName;

    private String invoiceNumber;

    private BigDecimal subtotal;

    private BigDecimal discountAmount;

    private BigDecimal totalAmount;

    private InvoiceStatus status;

    private LocalDateTime createdAt;

    private List<InvoiceItemResponse_Dto> items;

    private Long billerId;

    private String billerName;


    // =====================================================
    // WHATSAPP
    // =====================================================

    private WhatsAppStatus whatsappStatus;

    private LocalDateTime whatsappSentAt;

    private String whatsappError;
}