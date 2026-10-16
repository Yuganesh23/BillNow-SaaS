package com.example.Billing.gst;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class IndianAmountInWords {
    private static final String[] SMALL = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

    private IndianAmountInWords() {}

    public static String rupees(BigDecimal amount) {
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = normalized.longValue();
        int paise = normalized.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
        String result = (rupees == 0 ? "Zero" : number(rupees)) + " Rupees";
        if (paise > 0) result += " and " + number(paise) + " Paise";
        return result + " Only";
    }

    private static String number(long value) {
        if (value < 20) return SMALL[(int) value];
        if (value < 100) return TENS[(int) value / 10] + suffix(value % 10);
        if (value < 1_000) return number(value / 100) + " Hundred" + suffix(value % 100);
        if (value < 100_000) return number(value / 1_000) + " Thousand" + suffix(value % 1_000);
        if (value < 10_000_000) return number(value / 100_000) + " Lakh" + suffix(value % 100_000);
        return number(value / 10_000_000) + " Crore" + suffix(value % 10_000_000);
    }

    private static String suffix(long remainder) { return remainder == 0 ? "" : " " + number(remainder); }
}
