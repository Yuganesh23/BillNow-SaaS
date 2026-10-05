package com.example.Billing.config;

public class BranchContextHolder {
    private static final ThreadLocal<Long> CONTEXT = new ThreadLocal<>();

    public static void setBranchId(Long branchId) {
        CONTEXT.set(branchId);
    }

    public static Long getBranchId() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
