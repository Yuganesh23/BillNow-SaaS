package com.example.Billing.dashboard;

import java.math.BigDecimal;

import lombok.*;


import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse_Dto {

    // =====================================================
    // SALES
    // =====================================================

    private BigDecimal todaySales;

    private BigDecimal totalSales;


    // =====================================================
    // INVOICES
    // =====================================================

    private Long todayInvoices;

    private Long totalInvoices;


    // =====================================================
    // CUSTOMERS
    // =====================================================

    private Long totalCustomers;


    // =====================================================
    // PRODUCTS
    // =====================================================

    private Long totalProducts;

    private Long lowStockProducts;


    // =====================================================
    // PAYMENTS
    // =====================================================

    private BigDecimal cashPayments;

    private BigDecimal upiPayments;

    private BigDecimal cardPayments;

    private BigDecimal otherPayments;


    // =====================================================
    // RECENT INVOICES
    // =====================================================

    private List<RecentInvoice_Dto> recentInvoices;
}