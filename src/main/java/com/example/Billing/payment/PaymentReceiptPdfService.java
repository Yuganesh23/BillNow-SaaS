package com.example.Billing.payment;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.gst.IndianAmountInWords;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;

@Service @RequiredArgsConstructor
public class PaymentReceiptPdfService {
    private final PaymentRepository payments;
    private final ShopContextResolver shopContext;

    @Transactional(readOnly = true)
    public byte[] generate(User_entity user, Long paymentId) {
        Long shopId = shopContext.resolveActiveShopId(user);
        Payment_entity payment = payments.findByIdAndInvoiceShopId(paymentId, shopId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found"));
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A5, 36, 36, 36, 36); PdfWriter.getInstance(document, output); document.open();
            document.add(new Paragraph("PAYMENT RECEIPT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph(payment.getInvoice().getShop().getName()));
            document.add(new Paragraph("Receipt: PAY-" + payment.getId()));
            document.add(new Paragraph("Invoice: " + payment.getInvoice().getInvoiceNumber()));
            document.add(new Paragraph("Received from: " + payment.getInvoice().getCustomer().getName()));
            document.add(new Paragraph("Method: " + payment.getPaymentMethod()));
            document.add(new Paragraph("Amount: INR " + payment.getAmount().setScale(2)));
            document.add(new Paragraph(IndianAmountInWords.rupees(payment.getAmount())));
            document.add(new Paragraph("Paid at: " + payment.getPaidAt())); document.close(); return output.toByteArray();
        } catch (Exception exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "RECEIPT_GENERATION_FAILED", "Unable to generate payment receipt");
        }
    }
}
