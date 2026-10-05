package com.example.Billing.reports;

import java.math.BigDecimal;



public interface ProductSalesProjection {

    Long getProductId();

    String getProductName();

    String getSku();

    Double getQuantitySold();

    BigDecimal getRevenue();
}