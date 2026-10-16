package com.example.Billing.importdata;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.customer.Customer_entity;
import com.example.Billing.gst.GstinValidator;
import com.example.Billing.gst.IndianState;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.product.Product_entity;
import com.example.Billing.product.TaxType;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.supplier.Supplier_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImportService {
    private final ShopContextResolver shopContextResolver;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final ImportTemplateService templateService;

    @Transactional(readOnly = true)
    public ImportModels.Preview preview(User_entity user, ImportModels.Type type, List<Map<String, String>> rawRows) {
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        return validate(shop, type, rawRows);
    }

    @Transactional
    public ImportModels.CommitResponse commit(User_entity user, ImportModels.CommitRequest request) {
        if (request == null || request.type() == null || request.rows() == null || request.rows().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "IMPORT_ROWS_REQUIRED", "No import rows were supplied");
        }
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        ImportModels.Preview preview = validate(shop, request.type(), request.rows());
        if (preview.invalidRows() > 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "IMPORT_VALIDATION_FAILED",
                    "Import was not committed because one or more rows are invalid");
        }
        for (ImportModels.Row row : preview.rows()) {
            persist(shop, request.type(), row.values());
        }
        return new ImportModels.CommitResponse(request.type(), preview.validRows());
    }

    private ImportModels.Preview validate(Shop_entity shop, ImportModels.Type type, List<Map<String, String>> rawRows) {
        if (type == null) throw new AppException(HttpStatus.BAD_REQUEST, "IMPORT_TYPE_REQUIRED", "Import type is required");
        if (rawRows == null || rawRows.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "IMPORT_EMPTY", "The file contains no data rows");
        }
        Set<String> batchKeys = new HashSet<>();
        List<ImportModels.Row> rows = new ArrayList<>();
        int rowNumber = 2;
        for (Map<String, String> raw : rawRows) {
            Map<String, String> values = normalize(raw);
            List<String> errors = validateRow(shop, type, values, batchKeys);
            rows.add(new ImportModels.Row(rowNumber++, values, errors));
        }
        int valid = (int) rows.stream().filter(ImportModels.Row::valid).count();
        return new ImportModels.Preview(type, templateService.headers(type), rows, valid, rows.size() - valid);
    }

    private List<String> validateRow(Shop_entity shop, ImportModels.Type type, Map<String, String> row, Set<String> batchKeys) {
        List<String> errors = new ArrayList<>();
        switch (type) {
            case PRODUCTS -> validateProduct(shop, row, batchKeys, errors);
            case CUSTOMERS -> validateCustomer(shop, row, batchKeys, errors);
            case SUPPLIERS -> validateSupplier(row, errors);
            case OPENING_BALANCES -> validateOpeningBalance(shop, row, batchKeys, errors);
        }
        return errors;
    }

    private void validateProduct(Shop_entity shop, Map<String, String> row, Set<String> keys, List<String> errors) {
        required(row, "name", errors);
        required(row, "sku", errors);
        String sku = value(row, "sku").toLowerCase(Locale.ROOT);
        if (!sku.isBlank() && (!keys.add("product:" + sku) || productRepository.existsByShopIdAndSku(shop.getId(), value(row, "sku")))) {
            errors.add("SKU already exists in this shop or file");
        }
        money(row, "purchase_price", true, errors);
        money(row, "selling_price", true, errors);
        decimal(row, "stock_quantity", true, errors);
        decimal(row, "low_stock_threshold", false, errors);
        money(row, "gst_rate", false, errors);
        money(row, "cess_rate", false, errors);
        String hsn = value(row, "hsn_code");
        int expected = shop.getHsnCodeLength() == null ? 4 : shop.getHsnCodeLength();
        if (!hsn.isBlank() && !hsn.matches("\\d{" + expected + "}")) errors.add("hsn_code must contain " + expected + " digits");
        enumValue(row, "tax_type", TaxType.class, false, errors);
    }

    private void validateCustomer(Shop_entity shop, Map<String, String> row, Set<String> keys, List<String> errors) {
        required(row, "name", errors);
        required(row, "whatsapp_number", errors);
        String whatsapp = value(row, "whatsapp_number");
        if (!whatsapp.isBlank() && (!keys.add("customer:" + whatsapp)
                || customerRepository.findByShopIdAndWhatsappNumber(shop.getId(), whatsapp).isPresent())) {
            errors.add("WhatsApp number already exists in this shop or file");
        }
        gstin(row, errors);
        String stateCode = value(row, "state_code");
        if (!stateCode.isBlank() && IndianState.fromCode(stateCode).isEmpty()) errors.add("state_code is invalid");
        String pincode = value(row, "pincode");
        if (!pincode.isBlank() && !pincode.matches("[1-9][0-9]{5}")) errors.add("pincode is invalid");
    }

    private void validateSupplier(Map<String, String> row, List<String> errors) {
        required(row, "business_name", errors);
        gstin(row, errors);
    }

    private void validateOpeningBalance(Shop_entity shop, Map<String, String> row, Set<String> keys, List<String> errors) {
        required(row, "account_type", errors);
        required(row, "identifier", errors);
        money(row, "amount", true, errors);
        String type = value(row, "account_type").toUpperCase(Locale.ROOT);
        String identifier = value(row, "identifier");
        if (!type.equals("CUSTOMER") && !type.equals("SUPPLIER")) {
            errors.add("account_type must be CUSTOMER or SUPPLIER");
            return;
        }
        if (!identifier.isBlank() && !keys.add(type + ":" + identifier.toLowerCase(Locale.ROOT))) {
            errors.add("Opening balance account is duplicated in this file");
        } else if (type.equals("CUSTOMER") && customerRepository.findByShopIdAndWhatsappNumber(shop.getId(), identifier).isEmpty()) {
            errors.add("Customer identifier was not found in this shop");
        } else if (type.equals("SUPPLIER") && supplierRepository.findByShopIdAndBusinessNameIgnoreCase(shop.getId(), identifier).isEmpty()) {
            errors.add("Supplier identifier was not found in this shop");
        }
    }

    private void persist(Shop_entity shop, ImportModels.Type type, Map<String, String> row) {
        switch (type) {
            case PRODUCTS -> productRepository.save(Product_entity.builder()
                    .shop(shop).name(value(row, "name")).sku(value(row, "sku")).category(blankToNull(value(row, "category")))
                    .purchasePrice(moneyValue(row, "purchase_price")).sellingPrice(moneyValue(row, "selling_price"))
                    .stockQuantity(numberValue(row, "stock_quantity", 0d)).lowStockThreshold(numberValue(row, "low_stock_threshold", 10d))
                    .hsnCode(blankToNull(value(row, "hsn_code"))).gstRate(moneyValue(row, "gst_rate"))
                    .cessRate(moneyValue(row, "cess_rate")).taxType(enumOrDefault(row, "tax_type", TaxType.NONE))
                    .uqc(defaultIfBlank(value(row, "uqc"), "NOS").toUpperCase(Locale.ROOT)).active(true).build());
            case CUSTOMERS -> {
                String gstin = upperOrNull(value(row, "gstin"));
                String stateCode = gstin != null ? GstinValidator.stateCode(gstin)
                        : defaultIfBlank(value(row, "state_code"), shop.getStateCode());
                customerRepository.save(Customer_entity.builder().shop(shop).name(value(row, "name"))
                        .whatsappNumber(value(row, "whatsapp_number")).email(blankToNull(value(row, "email")))
                        .address(blankToNull(value(row, "address"))).locality(blankToNull(value(row, "locality")))
                        .pincode(blankToNull(value(row, "pincode"))).gstin(gstin).stateCode(stateCode)
                        .billingAddress(defaultIfBlank(value(row, "billing_address"), blankToNull(value(row, "address"))))
                        .shippingAddress(blankToNull(value(row, "shipping_address"))).creditBalance(BigDecimal.ZERO.setScale(2)).build());
            }
            case SUPPLIERS -> {
                String gstin = upperOrNull(value(row, "gstin"));
                Supplier_entity supplier = Supplier_entity.builder().shop(shop).businessName(value(row, "business_name"))
                        .contactPerson(blankToNull(value(row, "contact_person"))).phone(blankToNull(value(row, "phone")))
                        .whatsapp(blankToNull(value(row, "whatsapp"))).email(blankToNull(value(row, "email"))).gstin(gstin)
                        .address(blankToNull(value(row, "address"))).notes(blankToNull(value(row, "notes"))).build();
                if (gstin != null) supplier.setStateCode(GstinValidator.stateCode(gstin));
                supplierRepository.save(supplier);
            }
            case OPENING_BALANCES -> {
                BigDecimal amount = moneyValue(row, "amount");
                String identifier = value(row, "identifier");
                if ("CUSTOMER".equalsIgnoreCase(value(row, "account_type"))) {
                    Customer_entity customer = customerRepository.findByShopIdAndWhatsappNumber(shop.getId(), identifier)
                            .orElseThrow(() -> notFound("Customer"));
                    customer.setCreditBalance(amount);
                    customerRepository.save(customer);
                } else {
                    Supplier_entity supplier = supplierRepository.findByShopIdAndBusinessNameIgnoreCase(shop.getId(), identifier)
                            .orElseThrow(() -> notFound("Supplier"));
                    supplier.setOutstandingBalance(amount);
                    supplierRepository.save(supplier);
                }
            }
        }
    }

    private void required(Map<String, String> row, String field, List<String> errors) {
        if (value(row, field).isBlank()) errors.add(field + " is required");
    }

    private void gstin(Map<String, String> row, List<String> errors) {
        String gstin = value(row, "gstin");
        if (!gstin.isBlank() && !GstinValidator.isValid(gstin)) errors.add("gstin is invalid");
    }

    private void money(Map<String, String> row, String field, boolean required, List<String> errors) {
        decimal(row, field, required, errors);
        String raw = value(row, field);
        if (!raw.isBlank()) try {
            if (new BigDecimal(raw).scale() > 2) errors.add(field + " must have at most 2 decimal places");
        } catch (NumberFormatException ignored) { }
    }

    private void decimal(Map<String, String> row, String field, boolean required, List<String> errors) {
        String raw = value(row, field);
        if (raw.isBlank()) {
            if (required) errors.add(field + " is required");
            return;
        }
        try {
            if (new BigDecimal(raw).compareTo(BigDecimal.ZERO) < 0) errors.add(field + " cannot be negative");
        } catch (NumberFormatException exception) {
            errors.add(field + " must be a number");
        }
    }

    private <E extends Enum<E>> void enumValue(Map<String, String> row, String field, Class<E> type, boolean required, List<String> errors) {
        String raw = value(row, field);
        if (raw.isBlank()) {
            if (required) errors.add(field + " is required");
            return;
        }
        try { Enum.valueOf(type, raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { errors.add(field + " is invalid"); }
    }

    private Map<String, String> normalize(Map<String, String> raw) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (raw == null) return normalized;
        raw.forEach((key, val) -> normalized.put(key == null ? "" : key.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_"), val == null ? "" : val.trim()));
        return normalized;
    }

    private String value(Map<String, String> row, String key) { return row.getOrDefault(key, "").trim(); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private String upperOrNull(String value) { String result = blankToNull(value); return result == null ? null : result.toUpperCase(Locale.ROOT); }
    private String defaultIfBlank(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private BigDecimal moneyValue(Map<String, String> row, String key) {
        String raw = value(row, key);
        return (raw.isBlank() ? BigDecimal.ZERO : new BigDecimal(raw)).setScale(2, RoundingMode.HALF_UP);
    }
    private Double numberValue(Map<String, String> row, String key, Double fallback) {
        String raw = value(row, key); return raw.isBlank() ? fallback : new BigDecimal(raw).doubleValue();
    }
    private <E extends Enum<E>> E enumOrDefault(Map<String, String> row, String key, E fallback) {
        String raw = value(row, key);
        if (raw.isBlank()) return fallback;
        @SuppressWarnings("unchecked") Class<E> type = (Class<E>) fallback.getDeclaringClass();
        return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT));
    }
    private AppException notFound(String account) {
        return new AppException(HttpStatus.NOT_FOUND, "IMPORT_ACCOUNT_NOT_FOUND", account + " was not found in this shop");
    }
}
