package com.example.Billing.reports;

import java.math.BigDecimal;



public interface CustomerSalesProjection {

    Long getCustomerId();

    String getCustomerName();

    String getWhatsappNumber();
    String getAddress();

    Long getInvoiceCount();

    BigDecimal getTotalPurchase();
}