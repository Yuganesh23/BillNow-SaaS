package com.example.Billing.reports;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportExportService {
    private final AdvancedReportService reports;

    public byte[] registerCsv(com.example.Billing.auth.User_entity user, String type, LocalDate from, LocalDate to,
                              Long partyId, BigDecimal taxRate, String documentType) {
        StringBuilder csv = new StringBuilder("Number,Date,Party,GSTIN,Document Type,Status,Taxable,Discount,CGST,SGST,IGST,Cess,Round Off,Total\r\n");
        for (ReportDtos.RegisterRow r : reports.registerExport(user, type, from, to, partyId, taxRate, documentType)) {
            append(csv, r.number(), r.date(), r.partyName(), r.partyGstin(), r.documentType(), r.status(),
                    r.taxableAmount(), r.discountAmount(), r.cgst(), r.sgst(), r.igst(), r.cess(), r.roundOff(), r.totalAmount());
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] registerExcel(com.example.Billing.auth.User_entity user, String type, LocalDate from, LocalDate to,
                                Long partyId, BigDecimal taxRate, String documentType) {
        List<ReportDtos.RegisterRow> rows = reports.registerExport(user, type, from, to, partyId, taxRate, documentType);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(safeSheet(type));
            String[] headers = {"Number","Date","Party","GSTIN","Document Type","Status","Taxable","Discount","CGST","SGST","IGST","Cess","Round Off","Total"};
            Row header = sheet.createRow(0); CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont(); font.setBold(true); style.setFont(font);
            for (int i=0;i<headers.length;i++) { Cell cell=header.createCell(i); cell.setCellValue(headers[i]); cell.setCellStyle(style); }
            int index=1;
            for (ReportDtos.RegisterRow r : rows) {
                Row row=sheet.createRow(index++); Object[] values={r.number(),r.date().toString(),r.partyName(),r.partyGstin(),
                        r.documentType(),r.status(),r.taxableAmount(),r.discountAmount(),r.cgst(),r.sgst(),r.igst(),r.cess(),r.roundOff(),r.totalAmount()};
                writeRow(row, values);
            }
            for (int i=0;i<headers.length;i++) sheet.autoSizeColumn(i);
            workbook.write(out); return out.toByteArray();
        } catch (java.io.IOException e) { throw new IllegalStateException("Could not build Excel export", e); }
    }

    public byte[] paymentsCsv(com.example.Billing.auth.User_entity user, LocalDate from, LocalDate to) {
        StringBuilder csv=new StringBuilder("Invoice,Date,Party,Method,Amount,Reference\r\n");
        for(ReportDtos.PaymentRow r:reports.paymentExport(user,from,to)) append(csv,r.invoiceNumber(),r.date(),r.partyName(),r.method(),r.amount(),r.reference());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] dayBookCsv(com.example.Billing.auth.User_entity user, LocalDate from, LocalDate to) {
        StringBuilder csv=new StringBuilder("Date,Type,Reference,Party,Debit,Credit\r\n");
        for(ReportDtos.DayBookRow r:reports.dayBookExport(user,from,to)) append(csv,r.date(),r.kind(),r.reference(),r.partyName(),r.debit(),r.credit());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] gstExcel(com.example.Billing.auth.User_entity user, LocalDate from, LocalDate to, BigDecimal threshold) {
        ReportDtos.Gstr1Summary gstr1 = reports.gstr1(user, from, to, threshold);
        ReportDtos.Gstr3bSummary gstr3b = reports.gstr3b(user, from, to);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet outward=workbook.createSheet("GSTR-1 Summary");
            writeRows(outward, new String[]{"Category","Documents","Taxable","CGST","SGST","IGST","Cess"},
                    gstr1.outwardSupplies().stream().map(v -> new Object[]{v.category(),v.documentCount(),v.taxableValue(),v.cgst(),v.sgst(),v.igst(),v.cess()}).toList());
            Sheet hsn=workbook.createSheet("HSN Summary");
            writeRows(hsn, new String[]{"HSN","Description","UQC","Quantity","Taxable","CGST","SGST","IGST","Cess"},
                    gstr1.hsnSummary().stream().map(v -> new Object[]{v.hsn(),v.description(),v.uqc(),v.quantity(),v.taxableValue(),v.cgst(),v.sgst(),v.igst(),v.cess()}).toList());
            Sheet threeB=workbook.createSheet("GSTR-3B Summary");
            writeRows(threeB, new String[]{"Metric","Amount"}, List.of(
                    new Object[]{"Outward taxable",gstr3b.outwardTaxable()}, new Object[]{"Outward CGST",gstr3b.outwardCgst()},
                    new Object[]{"Outward SGST",gstr3b.outwardSgst()}, new Object[]{"Outward IGST",gstr3b.outwardIgst()},
                    new Object[]{"Outward cess",gstr3b.outwardCess()}, new Object[]{"Eligible ITC CGST",gstr3b.eligibleItcCgst()},
                    new Object[]{"Eligible ITC SGST",gstr3b.eligibleItcSgst()}, new Object[]{"Eligible ITC IGST",gstr3b.eligibleItcIgst()},
                    new Object[]{"Eligible ITC cess",gstr3b.eligibleItcCess()}, new Object[]{"Net tax payable",gstr3b.netTaxPayable()}));
            workbook.write(out); return out.toByteArray();
        } catch (java.io.IOException e) { throw new IllegalStateException("Could not build GST workbook", e); }
    }

    public byte[] tallyXml(com.example.Billing.auth.User_entity user, String type, LocalDate from, LocalDate to) {
        ReportDtos.ExportSettings settings=reports.settings(user);
        List<ReportDtos.RegisterRow> rows=reports.registerExport(user, type, from, to, null, null, null);
        String mainLedger="sales".equals(type)?settings.salesLedger():settings.purchaseLedger();
        StringBuilder xml=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><ENVELOPE><HEADER><TALLYREQUEST>Import Data</TALLYREQUEST></HEADER><BODY><IMPORTDATA><REQUESTDATA>");
        for (ReportDtos.RegisterRow r:rows) {
            xml.append("<TALLYMESSAGE><VOUCHER VCHTYPE=\"").append("sales".equals(type)?"Sales":"Purchase").append("\"><DATE>")
                    .append(r.date().toLocalDate().toString().replace("-","")).append("</DATE><VOUCHERNUMBER>").append(escape(r.number()))
                    .append("</VOUCHERNUMBER><PARTYLEDGERNAME>").append(escape(r.partyName())).append("</PARTYLEDGERNAME>")
                    .append(entry(r.partyName(),r.totalAmount().negate()))
                    .append(entry(mainLedger,r.taxableAmount())).append(entry(settings.cgstLedger(),r.cgst()))
                    .append(entry(settings.sgstLedger(),r.sgst())).append(entry(settings.igstLedger(),r.igst()))
                    .append(entry(settings.cessLedger(),r.cess())).append("</VOUCHER></TALLYMESSAGE>");
        }
        xml.append("</REQUESTDATA></IMPORTDATA></BODY></ENVELOPE>");
        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String entry(String ledger, BigDecimal amount) { return amount.signum()==0?"":"<ALLLEDGERENTRIES.LIST><LEDGERNAME>"+escape(ledger)+"</LEDGERNAME><AMOUNT>"+amount+"</AMOUNT></ALLLEDGERENTRIES.LIST>"; }
    private String escape(String value) { return value==null?"":value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;"); }
    private void append(StringBuilder csv,Object... values) { for(int i=0;i<values.length;i++){if(i>0)csv.append(','); csv.append('"').append(String.valueOf(values[i]==null?"":values[i]).replace("\"","\"\"")).append('"');} csv.append("\r\n"); }
    private void writeRows(Sheet sheet,String[] headers,List<Object[]> values){Row h=sheet.createRow(0);for(int i=0;i<headers.length;i++)h.createCell(i).setCellValue(headers[i]);int row=1;for(Object[] value:values)writeRow(sheet.createRow(row++),value);for(int i=0;i<headers.length;i++)sheet.autoSizeColumn(i);}
    private void writeRow(Row row,Object[] values){for(int i=0;i<values.length;i++){Cell cell=row.createCell(i);Object v=values[i];if(v instanceof BigDecimal b)cell.setCellValue(b.toPlainString());else cell.setCellValue(v==null?"":v.toString());}}
    private String safeSheet(String value){return value.length()>31?value.substring(0,31):value;}
}
