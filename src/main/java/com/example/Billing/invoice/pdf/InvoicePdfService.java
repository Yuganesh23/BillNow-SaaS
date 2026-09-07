package com.example.Billing.invoice.pdf;

import com.example.Billing.invoice.InvoiceItem;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.auth.User_entity;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private final InvoiceRepository invoiceRepository;


    // =====================================================
    // GENERATE PDF
    // =====================================================

    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(
            User_entity user,
            Long invoiceId
    ) {

        Invoice_entity invoice =
                invoiceRepository
                        .findByIdAndShopId(
                                invoiceId,
                                user.getShop().getId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invoice not found"
                                )
                        );

        try {

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            Document document =
                    new Document(
                            PageSize.A4,
                            36,
                            36,
                            36,
                            36
                    );

            PdfWriter.getInstance(
                    document,
                    outputStream
            );

            document.open();


            // =================================================
            // FONTS
            // =================================================

            Font shopNameFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            18
                    );

            Font titleFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            16
                    );

            Font boldFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            10
                    );

            Font normalFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA,
                            10
                    );

            Font smallFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA,
                            9
                    );


            // =================================================
            // SHOP DETAILS
            // =================================================

            Shop_entity shop =
                    invoice.getShop();

            Paragraph shopName =
                    new Paragraph(
                            safe(shop.getName()),
                            shopNameFont
                    );

            shopName.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(shopName);


            Paragraph shopEmail =
                    new Paragraph(
                            "Email: "
                                    + safe(shop.getEmail()),
                            smallFont
                    );

            shopEmail.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(shopEmail);


            document.add(
                    new Paragraph(" ")
            );


            // =================================================
            // INVOICE TITLE
            // =================================================

            Paragraph invoiceTitle =
                    new Paragraph(
                            "INVOICE",
                            titleFont
                    );

            invoiceTitle.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(invoiceTitle);

            document.add(
                    new Paragraph(" ")
            );


            // =================================================
            // INVOICE INFO
            // =================================================

            PdfPTable infoTable =
                    new PdfPTable(2);

            infoTable.setWidthPercentage(100);

            infoTable.setWidths(
                    new float[]{50, 50}
            );


            PdfPCell leftCell =
                    new PdfPCell();

            leftCell.setBorder(
                    Rectangle.NO_BORDER
            );

            leftCell.addElement(
                    new Paragraph(
                            "Invoice Number: "
                                    + safe(invoice.getInvoiceNumber()),
                            normalFont
                    )
            );

            leftCell.addElement(
                    new Paragraph(
                            "Date: "
                                    + invoice.getCreatedAt()
                                    .format(
                                            DateTimeFormatter.ofPattern(
                                                    "dd-MM-yyyy HH:mm"
                                            )
                                    ),
                            normalFont
                    )
            );

            leftCell.addElement(
                    new Paragraph(
                            "Status: "
                                    + invoice.getStatus(),
                            normalFont
                    )
            );


            PdfPCell rightCell =
                    new PdfPCell();

            rightCell.setBorder(
                    Rectangle.NO_BORDER
            );

            rightCell.addElement(
                    new Paragraph(
                            "Customer: "
                                    + safe(
                                    invoice.getCustomer()
                                            .getName()
                            ),
                            normalFont
                    )
            );

            rightCell.addElement(
                    new Paragraph(
                            "WhatsApp: "
                                    + safe(
                                    invoice.getCustomer()
                                            .getWhatsappNumber()
                            ),
                            normalFont
                    )
            );

            if (invoice.getCustomer().getEmail() != null) {

                rightCell.addElement(
                        new Paragraph(
                                "Email: "
                                        + invoice.getCustomer()
                                        .getEmail(),
                                normalFont
                        )
                );
            }


            infoTable.addCell(leftCell);
            infoTable.addCell(rightCell);

            document.add(infoTable);

            document.add(
                    new Paragraph(" ")
            );


            // =================================================
            // ITEMS TABLE
            // =================================================

            PdfPTable itemTable =
                    new PdfPTable(5);

            itemTable.setWidthPercentage(100);

            itemTable.setWidths(
                    new float[]{
                            34,
                            15,
                            17,
                            17,
                            17
                    }
            );


            addHeader(itemTable, "Product", boldFont);
            addHeader(itemTable, "Qty", boldFont);
            addHeader(itemTable, "Unit Price", boldFont);
            addHeader(itemTable, "Total", boldFont);
            addHeader(itemTable, "SKU", boldFont);


            for (InvoiceItem item :
                    invoice.getItems()) {

                addCell(
                        itemTable,
                        item.getProduct().getName(),
                        normalFont
                );

                addCell(
                        itemTable,
                        String.valueOf(
                                item.getQuantity()
                        ),
                        normalFont
                );

                addCell(
                        itemTable,
                        formatMoney(
                                item.getUnitPrice()
                        ),
                        normalFont
                );

                addCell(
                        itemTable,
                        formatMoney(
                                item.getTotalPrice()
                        ),
                        normalFont
                );

                addCell(
                        itemTable,
                        item.getProduct().getSku(),
                        normalFont
                );
            }


            document.add(itemTable);

            document.add(
                    new Paragraph(" ")
            );


            // =================================================
            // TOTALS
            // =================================================

            PdfPTable totalsTable =
                    new PdfPTable(2);

            totalsTable.setWidthPercentage(45);

            totalsTable.setHorizontalAlignment(
                    Element.ALIGN_RIGHT
            );

            totalsTable.setWidths(
                    new float[]{60, 40}
            );


            addTotalRow(
                    totalsTable,
                    "Subtotal",
                    formatMoney(
                            invoice.getSubtotal()
                    ),
                    normalFont,
                    false
            );

            addTotalRow(
                    totalsTable,
                    "Discount",
                    formatMoney(
                            invoice.getDiscountAmount()
                    ),
                    normalFont,
                    false
            );

            addTotalRow(
                    totalsTable,
                    "TOTAL",
                    formatMoney(
                            invoice.getTotalAmount()
                    ),
                    boldFont,
                    true
            );


            document.add(
                    totalsTable
            );


            document.add(
                    new Paragraph(" ")
            );


            // =================================================
            // BILLER
            // =================================================

            Paragraph biller =
                    new Paragraph(
                            "Biller: "
                                    + safe(
                                    invoice.getBiller()
                                            .getName()
                            ),
                            normalFont
                    );

            document.add(biller);


            // =================================================
            // FOOTER
            // =================================================

            document.add(
                    new Paragraph(" ")
            );

            Paragraph footer =
                    new Paragraph(
                            "Thank you for your business!",
                            boldFont
                    );

            footer.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(footer);


            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate invoice PDF",
                    e
            );
        }
    }


    // =====================================================
    // HELPER METHODS
    // =====================================================

    private void addHeader(
            PdfPTable table,
            String text,
            Font font
    ) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(text, font)
                );

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setPadding(6);

        table.addCell(cell);
    }


    private void addCell(
            PdfPTable table,
            String text,
            Font font
    ) {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                safe(text),
                                font
                        )
                );

        cell.setPadding(5);

        table.addCell(cell);
    }


    private void addTotalRow(
            PdfPTable table,
            String label,
            String value,
            Font font,
            boolean emphasized
    ) {

        PdfPCell labelCell =
                new PdfPCell(
                        new Phrase(label, font)
                );

        PdfPCell valueCell =
                new PdfPCell(
                        new Phrase(value, font)
                );

        labelCell.setBorder(
                emphasized
                        ? Rectangle.BOX
                        : Rectangle.NO_BORDER
        );

        valueCell.setBorder(
                emphasized
                        ? Rectangle.BOX
                        : Rectangle.NO_BORDER
        );

        labelCell.setPadding(5);
        valueCell.setPadding(5);

        valueCell.setHorizontalAlignment(
                Element.ALIGN_RIGHT
        );

        table.addCell(labelCell);
        table.addCell(valueCell);
    }


    private String formatMoney(
            BigDecimal amount
    ) {

        if (amount == null) {
            return "0.00";
        }

        return "Rs. "
                + amount.setScale(
                2,
                java.math.RoundingMode.HALF_UP
        );
    }


    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }
}