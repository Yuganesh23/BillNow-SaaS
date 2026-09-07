package com.example.Billing.invoice.whatsapp;

import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.invoice.Invoice_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WhatsAppStatusService {

    private final InvoiceRepository invoiceRepository;


    // =====================================================
    // MARK SENDING
    // =====================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSending(
            Long invoiceId
    ) {

        Invoice_entity invoice =
                invoiceRepository
                        .findById(invoiceId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invoice not found"
                                )
                        );


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
            Long invoiceId
    ) {

        Invoice_entity invoice =
                invoiceRepository
                        .findById(invoiceId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invoice not found"
                                )
                        );


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
            String errorMessage
    ) {

        Invoice_entity invoice =
                invoiceRepository
                        .findById(invoiceId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invoice not found"
                                )
                        );


        invoice.setWhatsappStatus(
                WhatsAppStatus.FAILED
        );

        invoice.setWhatsappError(
                errorMessage
        );


        invoiceRepository.save(invoice);
    }
}