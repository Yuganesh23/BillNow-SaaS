package com.example.Billing.subscription.billing;

import com.example.Billing.common.AppException;
import com.example.Billing.gst.IndianAmountInWords;
import com.example.Billing.gst.IndianState;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/** Renders BillNow's GST tax invoice for one subscription charge. */
@Service
public class SaaSInvoicePdfService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public byte[] render(SaaSBillingCharge_entity charge) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36); PdfWriter.getInstance(document, output); document.open();
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            document.add(new Paragraph("TAX INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph("Invoice no: " + charge.getInvoiceNumber()
                    + "    Date: " + charge.getPaidAt().format(DATE)));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Supplier", bold));
            document.add(new Paragraph(charge.getSupplierLegalName()));
            if (charge.getSupplierAddress() != null && !charge.getSupplierAddress().isBlank()) document.add(new Paragraph(charge.getSupplierAddress()));
            document.add(new Paragraph("GSTIN: " + charge.getSupplierGstin() + "    State: " + state(charge.getSupplierStateCode())));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Billed to", bold));
            document.add(new Paragraph(charge.getRecipientName()));
            if (charge.getRecipientAddress() != null && !charge.getRecipientAddress().isBlank()) document.add(new Paragraph(charge.getRecipientAddress()));
            document.add(new Paragraph("GSTIN: " + (charge.getRecipientGstin() == null ? "Unregistered" : charge.getRecipientGstin())));
            document.add(new Paragraph("Place of supply: " + state(charge.getPlaceOfSupplyCode())));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{5, 1.5f, 2});
            table.setWidthPercentage(100);
            table.addCell(new Phrase("Description", bold)); table.addCell(new Phrase("SAC", bold)); table.addCell(new Phrase("Taxable value (INR)", bold));
            String period = charge.getPeriodStart() != null && charge.getPeriodEnd() != null
                    ? " (" + charge.getPeriodStart().format(DATE) + " to " + charge.getPeriodEnd().format(DATE) + ")" : "";
            table.addCell("BillNow " + charge.getPlan() + " subscription" + period);
            table.addCell(charge.getSacCode());
            table.addCell(money(charge.getTaxableAmount()));
            document.add(table);
            document.add(new Paragraph(" "));

            String rate = charge.getGstRate().stripTrailingZeros().toPlainString();
            if (charge.getIgstAmount().signum() > 0) {
                document.add(new Paragraph("IGST @ " + rate + "%: INR " + money(charge.getIgstAmount())));
            } else {
                String half = charge.getGstRate().divide(new BigDecimal("2")).stripTrailingZeros().toPlainString();
                document.add(new Paragraph("CGST @ " + half + "%: INR " + money(charge.getCgstAmount())));
                document.add(new Paragraph("SGST @ " + half + "%: INR " + money(charge.getSgstAmount())));
            }
            document.add(new Paragraph("Total: INR " + money(charge.getTotalAmount()), bold));
            document.add(new Paragraph(IndianAmountInWords.rupees(charge.getTotalAmount())));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Paid via Razorpay. Payment reference: " + charge.getRazorpayPaymentId()));
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "INVOICE_GENERATION_FAILED", "Unable to generate the tax invoice");
        }
    }

    private static String state(String code) {
        return IndianState.fromCode(code).map(state -> state.name() + " (" + code + ")").orElse(code);
    }

    private static String money(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}
