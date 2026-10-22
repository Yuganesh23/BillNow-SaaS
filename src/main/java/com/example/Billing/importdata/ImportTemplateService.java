package com.example.Billing.importdata;

import com.example.Billing.common.AppException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ImportTemplateService {
    public List<String> headers(ImportModels.Type type) {
        return switch (type) {
            case PRODUCTS -> List.of("name", "sku", "category", "purchase_price", "selling_price",
                    "stock_quantity", "low_stock_threshold", "hsn_code", "gst_rate", "tax_type", "uqc", "cess_rate");
            case CUSTOMERS -> List.of("name", "whatsapp_number", "email", "address", "locality", "pincode",
                    "gstin", "state_code", "billing_address", "shipping_address");
            case SUPPLIERS -> List.of("business_name", "contact_person", "phone", "whatsapp", "email", "gstin", "address", "notes");
            case OPENING_BALANCES -> List.of("account_type", "identifier", "amount");
        };
    }

    public byte[] create(ImportModels.Type type, String format) {
        if ("csv".equalsIgnoreCase(format)) {
            return (String.join(",", headers(type)) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        }
        if (!"xlsx".equalsIgnoreCase(format)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "IMPORT_FORMAT_INVALID", "Template format must be csv or xlsx");
        }
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(type.name().toLowerCase());
            Row row = sheet.createRow(0);
            CellStyle header = workbook.createCellStyle();
            header.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            List<String> columns = headers(type);
            for (int index = 0; index < columns.size(); index++) {
                row.createCell(index).setCellValue(columns.get(index));
                row.getCell(index).setCellStyle(header);
                sheet.setColumnWidth(index, Math.max(14, columns.get(index).length() + 2) * 256);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "IMPORT_TEMPLATE_FAILED", "Template could not be generated");
        }
    }
}
