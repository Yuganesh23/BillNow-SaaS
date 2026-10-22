package com.example.Billing.invoice;

import com.example.Billing.common.AppException;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DocumentNumberService {
    private static final Pattern PREFIX = Pattern.compile("^[A-Za-z0-9/-]{1,8}$");
    private final DocumentSequenceRepository repository;

    public NumberedDocument next(Shop_entity lockedShop, String documentType, String prefix, LocalDate date) {
        if (prefix == null || !PREFIX.matcher(prefix).matches()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_DOCUMENT_PREFIX",
                    "Document prefix may contain only letters, digits, / and - and must be at most 8 characters");
        }
        String fy = financialYear(date);
        DocumentSequence_entity sequence = repository.findForUpdate(lockedShop.getId(), fy, documentType)
                .orElseGet(() -> repository.saveAndFlush(DocumentSequence_entity.builder()
                        .shop(lockedShop).financialYear(fy).documentType(documentType).lastNumber(0).build()));
        int value = sequence.getLastNumber() + 1;
        sequence.setLastNumber(value);
        String number = prefix + "/" + fy + "/" + String.format("%04d", value);
        if (number.length() > 16) {
            throw new AppException(HttpStatus.BAD_REQUEST, "DOCUMENT_NUMBER_TOO_LONG",
                    "Document number exceeds the GST invoice limit of 16 characters");
        }
        repository.save(sequence);
        return new NumberedDocument(number, fy, value);
    }

    public static String financialYear(LocalDate date) {
        int start = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        return start + "-" + String.format("%02d", (start + 1) % 100);
    }

    public record NumberedDocument(String number, String financialYear, int sequence) {}
}
