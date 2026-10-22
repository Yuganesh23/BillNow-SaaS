package com.example.Billing.invoice.whatsapp;

import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.invoice.Invoice_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import com.example.Billing.common.AppException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class WhatsAppStatusService {

    private final InvoiceRepository invoiceRepository;


    // =====================================================
    // MARK SENDING
    // =====================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSending(
            Long invoiceId,
            Long shopId
    ) {

        Invoice_entity invoice =
                invoiceRepository
                        .findByIdAndShopId(invoiceId, shopId)
                        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found"));


        invoice.setWhatsappStatus(
                WhatsAppStatus.SENDING
        );

        invoice.setWhatsappSentAt(
                null
        );

        invoice.setWhatsappError(
                null
        );


        invoiceRepository.save(invoice);
    }


    // =====================================================
    // MARK SENT
    // =====================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(
            Long invoiceId,
            Long shopId
    ) {

        Invoice_entity invoice =
                invoiceRepository
                        .findByIdAndShopId(invoiceId, shopId)
                        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found"));


        invoice.setWhatsappStatus(
                WhatsAppStatus.SENT
        );

        invoice.setWhatsappSentAt(
                LocalDateTime.now()
        );

        invoice.setWhatsappError(
                null
        );


        invoiceRepository.save(invoice);
    }


    // =====================================================
    // MARK FAILED
    // =====================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(
            Long invoiceId,
            Long shopId,
            String errorMessage
    ) {

        Invoice_entity invoice =
                invoiceRepository
                        .findByIdAndShopId(invoiceId, shopId)
                        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found"));


        invoice.setWhatsappStatus(
                WhatsAppStatus.FAILED
        );

        invoice.setWhatsappError(
                errorMessage
        );


        invoiceRepository.save(invoice);
    }
}
