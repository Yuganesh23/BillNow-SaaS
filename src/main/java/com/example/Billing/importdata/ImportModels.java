package com.example.Billing.importdata;

import java.util.List;
import java.util.Map;

public final class ImportModels {
    private ImportModels() {}

    public enum Type {
        PRODUCTS, CUSTOMERS, SUPPLIERS, OPENING_BALANCES
    }

    public record Row(int rowNumber, Map<String, String> values, List<String> errors) {
        public boolean valid() { return errors == null || errors.isEmpty(); }
    }

    public record Preview(Type type, List<String> headers, List<Row> rows, int validRows, int invalidRows) {}

    public record CommitRequest(Type type, List<Map<String, String>> rows) {}

    public record CommitResponse(Type type, int importedRows) {}
}
