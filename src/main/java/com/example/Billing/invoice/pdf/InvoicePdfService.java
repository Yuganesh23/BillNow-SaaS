package com.example.Billing.invoice.pdf;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

import com.example.Billing.invoice.InvoiceItem;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.payment.PaymentRepository;
import com.example.Billing.payment.Payment_entity;
import java.util.List;
import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.auth.User_entity;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.Billing.config.ShopContextResolver;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private final InvoiceRepository invoiceRepository;
    private final ShopContextResolver shopContextResolver;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(User_entity user, Long invoiceId) {

        Invoice_entity invoice = invoiceRepository
                .findByIdAndShopId(invoiceId, shopContextResolver.resolveActiveShop(user).getId())
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        
        Shop_entity shop = invoice.getShop();
        boolean hasShopGst = shop.getGstin() != null && !shop.getGstin().isEmpty();
        boolean hasCustomerGst = invoice.getCustomer() != null && invoice.getCustomer().getGstin() != null && !invoice.getCustomer().getGstin().isEmpty();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.GRAY);

            // =================================================
            // TOP HEADER (Business Info, Logo & Invoice Details)
            // =================================================
            PdfPTable headerTable = new PdfPTable(3);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{40, 20, 40});

            // Left: Business Info
            PdfPCell leftHeader = new PdfPCell();
            leftHeader.setBorder(Rectangle.NO_BORDER);
            String displayName = (shop.getInvoiceName() != null && !shop.getInvoiceName().trim().isEmpty()) ? shop.getInvoiceName() : shop.getName();
            leftHeader.addElement(new Paragraph(safe(displayName), headerFont));
            if (shop.getLegalName() != null && !shop.getLegalName().isEmpty()) {
                leftHeader.addElement(new Paragraph("Legal Name: " + shop.getLegalName(), smallFont));
            }
            if (shop.getAddress() != null && !shop.getAddress().isEmpty()) {
                leftHeader.addElement(new Paragraph(shop.getAddress(), smallFont));
            }
            if (shop.getMobileNumber() != null && !shop.getMobileNumber().isEmpty()) {
                leftHeader.addElement(new Paragraph("Phone: " + shop.getMobileNumber(), smallFont));
            }
            if (shop.getEmail() != null && !shop.getEmail().isEmpty()) {
                leftHeader.addElement(new Paragraph("Email: " + shop.getEmail(), smallFont));
            }
            if (hasShopGst) {
                leftHeader.addElement(new Paragraph("GSTIN: " + shop.getGstin(), boldFont));
            }
            headerTable.addCell(leftHeader);

            // Middle: Logo
            PdfPCell middleHeader = new PdfPCell();
            middleHeader.setBorder(Rectangle.NO_BORDER);
            middleHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            middleHeader.setVerticalAlignment(Element.ALIGN_MIDDLE);
            
            if (shop.getLogoBase64() != null && !shop.getLogoBase64().isEmpty()) {
                try {
                    String base64Data = shop.getLogoBase64();
                    if (base64Data.contains(",")) {
                        base64Data = base64Data.split(",")[1];
                    }
                    byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
                    Image logo = Image.getInstance(imageBytes);
                    logo.scaleToFit(80, 60);
                    logo.setAlignment(Element.ALIGN_CENTER);
                    middleHeader.addElement(logo);
                } catch (Exception e) {
                    System.err.println("Failed to load logo: " + e.getMessage());
                }
            }
            headerTable.addCell(middleHeader);

            // Right: INVOICE title & details
            PdfPCell rightHeader = new PdfPCell();
            rightHeader.setBorder(Rectangle.NO_BORDER);
            rightHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            Paragraph title = new Paragraph("INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_RIGHT);
            rightHeader.addElement(title);
            
            Paragraph invDetails = new Paragraph(
                safe(invoice.getInvoiceNumber()) + "\n" +
                (invoice.getCreatedAt() != null ? invoice.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")) : "N/A"), 
                normalFont
            );
            invDetails.setAlignment(Element.ALIGN_RIGHT);
            rightHeader.addElement(invDetails);
            
            Paragraph statusPara = new Paragraph("Status: " + safe(invoice.getStatus() != null ? invoice.getStatus().name() : ""), boldFont);
            statusPara.setAlignment(Element.ALIGN_RIGHT);
            rightHeader.addElement(statusPara);

            headerTable.addCell(rightHeader);
            document.add(headerTable);
            
            document.add(new Paragraph(" "));
            addLineSeparator(document);
            document.add(new Paragraph(" "));

            // =================================================
            // BILL TO & BILLER SECTION
            // =================================================
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[]{60, 40});

            // Left: Bill To
            PdfPCell billToCell = new PdfPCell();
            billToCell.setBorder(Rectangle.NO_BORDER);
            billToCell.addElement(new Paragraph("BILL TO", labelFont));
            if (invoice.getCustomer() != null && !invoice.getCustomer().getName().equalsIgnoreCase("Cash Customer")) {
                billToCell.addElement(new Paragraph(safe(invoice.getCustomer().getName()), boldFont));
                if (invoice.getCustomer().getWhatsappNumber() != null && !invoice.getCustomer().getWhatsappNumber().isEmpty()) {
                    billToCell.addElement(new Paragraph("Mobile: " + invoice.getCustomer().getWhatsappNumber(), normalFont));
                }
                if (invoice.getCustomer().getEmail() != null && !invoice.getCustomer().getEmail().isEmpty()) {
                    billToCell.addElement(new Paragraph("Email: " + invoice.getCustomer().getEmail(), normalFont));
                }
                if (invoice.getCustomer().getAddress() != null && !invoice.getCustomer().getAddress().isEmpty()) {
                    billToCell.addElement(new Paragraph(invoice.getCustomer().getAddress(), normalFont));
                }
                if (hasCustomerGst) {
                    billToCell.addElement(new Paragraph("GSTIN: " + invoice.getCustomer().getGstin(), boldFont));
                }
            } else {
                billToCell.addElement(new Paragraph("Cash Customer", normalFont));
            }
            metaTable.addCell(billToCell);

            // Right: Biller
            PdfPCell billerCell = new PdfPCell();
            billerCell.setBorder(Rectangle.NO_BORDER);
            Paragraph billerLabel = new Paragraph("BILLER", labelFont);
            billerLabel.setAlignment(Element.ALIGN_RIGHT);
            billerCell.addElement(billerLabel);
            
            Paragraph billerName = new Paragraph(safe(invoice.getBiller() != null ? invoice.getBiller().getName() : "System"), boldFont);
            billerName.setAlignment(Element.ALIGN_RIGHT);
            billerCell.addElement(billerName);

            metaTable.addCell(billerCell);
            document.add(metaTable);
            
            document.add(new Paragraph(" "));
            addLineSeparator(document);
            document.add(new Paragraph(" "));

            // =================================================
            // ITEMS TABLE
            // =================================================
            PdfPTable itemTable = new PdfPTable(hasShopGst ? 6 : 5);
            itemTable.setWidthPercentage(100);
            if (hasShopGst) {
                itemTable.setWidths(new float[]{30, 15, 10, 15, 15, 15});
                addTableHeader(itemTable, "Product", boldFont, Element.ALIGN_LEFT);
                addTableHeader(itemTable, "HSN", boldFont, Element.ALIGN_CENTER);
                addTableHeader(itemTable, "Qty", boldFont, Element.ALIGN_CENTER);
                addTableHeader(itemTable, "Rate", boldFont, Element.ALIGN_RIGHT);
                addTableHeader(itemTable, "Taxable", boldFont, Element.ALIGN_RIGHT);
                addTableHeader(itemTable, "Total", boldFont, Element.ALIGN_RIGHT);
            } else {
                itemTable.setWidths(new float[]{40, 15, 15, 15, 15});
                addTableHeader(itemTable, "Product", boldFont, Element.ALIGN_LEFT);
                addTableHeader(itemTable, "HSN", boldFont, Element.ALIGN_CENTER);
                addTableHeader(itemTable, "Qty", boldFont, Element.ALIGN_CENTER);
                addTableHeader(itemTable, "Price", boldFont, Element.ALIGN_RIGHT);
                addTableHeader(itemTable, "Total", boldFont, Element.ALIGN_RIGHT);
            }

            for (InvoiceItem item : invoice.getItems()) {
                addTableCell(itemTable, item.getProduct().getName(), normalFont, Element.ALIGN_LEFT);
                addTableCell(itemTable, safe(item.getProduct().getHsnCode()), normalFont, Element.ALIGN_CENTER);
                addTableCell(itemTable, String.valueOf(item.getQuantity()), normalFont, Element.ALIGN_CENTER);
                addTableCell(itemTable, formatMoney(item.getUnitPrice()), normalFont, Element.ALIGN_RIGHT);
                if (hasShopGst) {
                    addTableCell(itemTable, formatMoney(item.getTaxableAmount()), normalFont, Element.ALIGN_RIGHT);
                }
                addTableCell(itemTable, formatMoney(item.getTotalPrice()), normalFont, Element.ALIGN_RIGHT);
            }
            document.add(itemTable);

            document.add(new Paragraph(" "));

            // =================================================
            // TOTALS SECTION
            // =================================================
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(100);
            totalsTable.setWidths(new float[]{65, 35});

            // Left side (Payment info or empty)
            PdfPCell leftTotalCell = new PdfPCell();
            leftTotalCell.setBorder(Rectangle.NO_BORDER);
            
            String paymentMethodStr = "";
            java.util.Optional<Payment_entity> paymentOpt = paymentRepository.findByInvoiceId(invoiceId);
            if (paymentOpt.isPresent() && paymentOpt.get().getPaymentMethod() != null) {
                paymentMethodStr = paymentOpt.get().getPaymentMethod().name();
            }
            if (!paymentMethodStr.isEmpty()) {
                String formattedMethod = paymentMethodStr.substring(0, 1).toUpperCase() + paymentMethodStr.substring(1).toLowerCase();
                leftTotalCell.addElement(new Paragraph("Payment Method: " + formattedMethod, smallFont));
            }
            totalsTable.addCell(leftTotalCell);

            // Right cell containing the totals table
            PdfPTable calcTable = new PdfPTable(2);
            calcTable.setWidthPercentage(100);
            calcTable.setWidths(new float[]{60, 40});

            if (hasShopGst) {
                addCalculationRow(calcTable, "Taxable Amount", formatMoney(invoice.getTaxableAmount()), normalFont, false);
                if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    addCalculationRow(calcTable, "Discount", "- " + formatMoney(invoice.getDiscountAmount()), normalFont, false);
                }
                
                boolean isInterState = invoice.getIsInterState() != null && invoice.getIsInterState();
                if (isInterState) {
                    addCalculationRow(calcTable, "IGST Total", "+ " + formatMoney(invoice.getIgstTotal()), normalFont, true);
                } else {
                    addCalculationRow(calcTable, "CGST Total", "+ " + formatMoney(invoice.getCgstTotal()), normalFont, false);
                    addCalculationRow(calcTable, "SGST Total", "+ " + formatMoney(invoice.getSgstTotal()), normalFont, true);
                }
            } else {
                addCalculationRow(calcTable, "Subtotal", formatMoney(invoice.getSubtotal()), normalFont, false);
                if (invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    addCalculationRow(calcTable, "Discount", "- " + formatMoney(invoice.getDiscountAmount()), normalFont, true);
                }
            }

            addCalculationRow(calcTable, "GRAND TOTAL", formatMoney(invoice.getTotalAmount()), totalFont, false);
            
            PdfPCell totalsWrapper = new PdfPCell(calcTable);
            totalsWrapper.setBorder(Rectangle.NO_BORDER);
            totalsTable.addCell(totalsWrapper);

            document.add(totalsTable);

            // =================================================
            // FOOTER
            // =================================================
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            
            Paragraph thanks = new Paragraph("Thank you", boldFont);
            thanks.setAlignment(Element.ALIGN_CENTER);
            document.add(thanks);
            
            Paragraph footerInfo = new Paragraph("Generated by BillNow", labelFont);
            footerInfo.setAlignment(Element.ALIGN_CENTER);
            document.add(footerInfo);

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    private void addLineSeparator(Document document) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setBorderWidth(1f);
        cell.setFixedHeight(2f);
        line.addCell(cell);
        document.add(line);
    }

    private void addTableHeader(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPaddingBottom(8f);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(Color.GRAY);
        cell.setBorderWidth(1f);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(safe(text), font));
        cell.setHorizontalAlignment(alignment);
        cell.setPaddingTop(6f);
        cell.setPaddingBottom(6f);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    private void addCalculationRow(PdfPTable table, String label, String value, Font font, boolean bottomBorder) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        labelCell.setPaddingTop(5f);
        labelCell.setPaddingBottom(5f);
        labelCell.setBorder(bottomBorder ? Rectangle.BOTTOM : Rectangle.NO_BORDER);
        if (bottomBorder) labelCell.setBorderColor(Color.GRAY);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPaddingTop(5f);
        valueCell.setPaddingBottom(5f);
        valueCell.setBorder(bottomBorder ? Rectangle.BOTTOM : Rectangle.NO_BORDER);
        if (bottomBorder) valueCell.setBorderColor(Color.GRAY);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0.00";
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(amount);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
