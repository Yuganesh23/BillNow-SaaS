package com.example.Billing.importdata;

import com.example.Billing.common.AppException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ImportFileParser {
    private static final int MAX_ROWS = 5_000;

    public List<Map<String, String>> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "IMPORT_FILE_REQUIRED", "Choose a CSV or Excel file");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            List<Map<String, String>> rows = name.endsWith(".csv") ? parseCsv(file) : parseWorkbook(file);
            if (rows.size() > MAX_ROWS) {
                throw new AppException(HttpStatus.BAD_REQUEST, "IMPORT_TOO_LARGE", "Imports are limited to 5,000 rows");
            }
            return rows;
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(HttpStatus.BAD_REQUEST, "IMPORT_FILE_INVALID", "The import file could not be read");
        }
    }

    private List<Map<String, String>> parseWorkbook(MultipartFile file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) return List.of();
            List<String> headers = new ArrayList<>();
            for (int cell = 0; cell < headerRow.getLastCellNum(); cell++) {
                headers.add(normalize(formatter.formatCellValue(headerRow.getCell(cell))));
            }
            List<Map<String, String>> rows = new ArrayList<>();
            for (int index = headerRow.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                Map<String, String> values = new LinkedHashMap<>();
                boolean populated = false;
                for (int cell = 0; cell < headers.size(); cell++) {
                    String value = formatter.formatCellValue(row.getCell(cell)).trim();
                    if (!value.isBlank()) populated = true;
                    if (!headers.get(cell).isBlank()) values.put(headers.get(cell), value);
                }
                if (populated) rows.add(values);
            }
            return rows;
        }
    }

    private List<Map<String, String>> parseCsv(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return List.of();
            List<String> headers = parseCsvLine(headerLine).stream().map(this::normalize).toList();
            List<Map<String, String>> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                List<String> cells = parseCsvLine(line);
                Map<String, String> values = new LinkedHashMap<>();
                for (int index = 0; index < headers.size(); index++) {
                    values.put(headers.get(index), index < cells.size() ? cells.get(index).trim() : "");
                }
                rows.add(values);
            }
            return rows;
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        values.add(current.toString());
        return values;
    }

    private String normalize(String header) {
        return header == null ? "" : header.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }
}
