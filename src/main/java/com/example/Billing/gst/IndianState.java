package com.example.Billing.gst;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record IndianState(String code, String name) {
    private static final Map<String, IndianState> STATES = new LinkedHashMap<>();

    static {
        add("01", "Jammu and Kashmir"); add("02", "Himachal Pradesh");
        add("03", "Punjab"); add("04", "Chandigarh"); add("05", "Uttarakhand");
        add("06", "Haryana"); add("07", "Delhi"); add("08", "Rajasthan");
        add("09", "Uttar Pradesh"); add("10", "Bihar"); add("11", "Sikkim");
        add("12", "Arunachal Pradesh"); add("13", "Nagaland"); add("14", "Manipur");
        add("15", "Mizoram"); add("16", "Tripura"); add("17", "Meghalaya");
        add("18", "Assam"); add("19", "West Bengal"); add("20", "Jharkhand");
        add("21", "Odisha"); add("22", "Chhattisgarh"); add("23", "Madhya Pradesh");
        add("24", "Gujarat"); add("26", "Dadra and Nagar Haveli and Daman and Diu");
        add("27", "Maharashtra"); add("29", "Karnataka"); add("30", "Goa");
        add("31", "Lakshadweep"); add("32", "Kerala"); add("33", "Tamil Nadu");
        add("34", "Puducherry"); add("35", "Andaman and Nicobar Islands");
        add("36", "Telangana"); add("37", "Andhra Pradesh"); add("38", "Ladakh");
        add("97", "Other Territory");
    }

    private static void add(String code, String name) {
        STATES.put(code, new IndianState(code, name));
    }

    public static Optional<IndianState> fromCode(String code) {
        return Optional.ofNullable(STATES.get(code));
    }

    public static Map<String, IndianState> all() {
        return Map.copyOf(STATES);
    }
}
