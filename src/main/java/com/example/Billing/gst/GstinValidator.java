package com.example.Billing.gst;

import java.util.Locale;
import java.util.regex.Pattern;

public final class GstinValidator {
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Pattern FORMAT = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z][Z][0-9A-Z]$");

    private GstinValidator() {}

    public static boolean isValid(String value) {
        if (value == null) return false;
        String gstin = value.trim().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(gstin).matches() || IndianState.fromCode(gstin.substring(0, 2)).isEmpty()) return false;
        return gstin.charAt(14) == checksum(gstin.substring(0, 14));
    }

    public static String stateCode(String value) {
        if (!isValid(value)) throw new IllegalArgumentException("Invalid GSTIN");
        return value.trim().substring(0, 2);
    }

    static char checksum(String firstFourteen) {
        int factor = 2;
        int sum = 0;
        for (int i = firstFourteen.length() - 1; i >= 0; i--) {
            int product = ALPHABET.indexOf(firstFourteen.charAt(i)) * factor;
            sum += product / 36 + product % 36;
            factor = factor == 2 ? 1 : 2;
        }
        return ALPHABET.charAt((36 - sum % 36) % 36);
    }
}
