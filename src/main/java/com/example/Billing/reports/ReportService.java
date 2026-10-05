package com.example.Billing.reports;

import java.math.BigDecimal;

import com.example.Billing.auth.User_entity;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.Billing.config.ShopContextResolver;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final ShopContextResolver shopContextResolver;


    // =====================================================
    // SALES REPORT
    // =====================================================

    public SalesReportResponse_Dto getSalesReport(
            User_entity user,
            LocalDate from,
            LocalDate to
    ) {

        Shop_entity shop = getUserShop(user);

        validateDates(from, to);

        LocalDateTime start =
                from.atStartOfDay();

        LocalDateTime end =
                to.plusDays(1).atStartOfDay();


        List<Object[]> result =
                reportRepository.getSalesReport(
                        shop.getId(),
                        start,
                        end
                );
                
        List<Object[]> costResult = 
                reportRepository.getSalesCostAndItems(
                        shop.getId(),
                        start,
                        end
                );

        Object[] row = result.get(0);

        long totalInvoices =
                ((Number) row[0]).longValue();

        java.math.BigDecimal subtotal = row[1] instanceof java.math.BigDecimal ? (java.math.BigDecimal) row[1] : (row[1] != null ? java.math.BigDecimal.valueOf(((Number) row[1]).doubleValue()) : java.math.BigDecimal.ZERO);
        java.math.BigDecimal totalDiscount = row[2] instanceof java.math.BigDecimal ? (java.math.BigDecimal) row[2] : (row[2] != null ? java.math.BigDecimal.valueOf(((Number) row[2]).doubleValue()) : java.math.BigDecimal.ZERO);
        java.math.BigDecimal totalSales = row[3] instanceof java.math.BigDecimal ? (java.math.BigDecimal) row[3] : (row[3] != null ? java.math.BigDecimal.valueOf(((Number) row[3]).doubleValue()) : java.math.BigDecimal.ZERO);
                
        java.math.BigDecimal totalCost = java.math.BigDecimal.ZERO;
        Double totalItemsSold = 0.0;
        
        for (Object[] itemRow : costResult) {
            Double qty = itemRow[0] != null ? ((Number) itemRow[0]).doubleValue() : 0.0;
            java.math.BigDecimal purchasePrice = java.math.BigDecimal.ZERO;
            if (itemRow[1] != null) {
                if (itemRow[1] instanceof java.math.BigDecimal) {
                    purchasePrice = (java.math.BigDecimal) itemRow[1];
                } else if (itemRow[1] instanceof Number) {
                    purchasePrice = java.math.BigDecimal.valueOf(((Number) itemRow[1]).doubleValue());
                }
            }
            totalItemsSold += qty;
            totalCost = totalCost.add(purchasePrice.multiply(java.math.BigDecimal.valueOf(qty)));
        }
                
        java.math.BigDecimal totalProfit = totalSales.subtract(totalCost);

        List<Object[]> highestRevResult = reportRepository.getHighestRevenueDay(shop.getId(), start, end, org.springframework.data.domain.PageRequest.of(0, 1));
        String highestRevenueDate = null;
        java.math.BigDecimal highestRevenueAmount = java.math.BigDecimal.ZERO;
        if (!highestRevResult.isEmpty()) {
            Object[] hrRow = highestRevResult.get(0);
            highestRevenueDate = hrRow[0] != null ? hrRow[0].toString() : null;
            if (hrRow[1] != null) {
                if (hrRow[1] instanceof java.math.BigDecimal) {
                    highestRevenueAmount = (java.math.BigDecimal) hrRow[1];
                } else if (hrRow[1] instanceof Number) {
                    highestRevenueAmount = java.math.BigDecimal.valueOf(((Number) hrRow[1]).doubleValue());
                }
            }
        }

        return SalesReportResponse_Dto.builder()
                .from(from)
                .to(to)
                .totalInvoices(totalInvoices)
                .subtotal(subtotal)
                .totalDiscount(totalDiscount)
                .totalSales(totalSales)
                .totalProfit(totalProfit)
                .totalItemsSold(totalItemsSold)
                .highestRevenueDate(highestRevenueDate)
                .highestRevenueAmount(highestRevenueAmount)
                .build();
    }


    // =====================================================
    
    public List<CustomerSalesResponse_Dto> getNewCustomers(User_entity user, LocalDate from, LocalDate to) {
        com.example.Billing.shop.Shop_entity shop = getUserShop(user);
        validateDates(from, to);
        java.time.LocalDateTime start = from.atStartOfDay();
        java.time.LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<CustomerSalesProjection> result = reportRepository.getNewCustomers(shop.getId(), start, end);
        return result.stream().map(customer -> CustomerSalesResponse_Dto.builder()
                .customerId(customer.getCustomerId())
                .customerName(customer.getCustomerName())
                .whatsappNumber(customer.getWhatsappNumber())
                .address(customer.getAddress())
                .invoiceCount(customer.getInvoiceCount())
                .totalPurchase(customer.getTotalPurchase())
                .build()).toList();
    }

    // PRODUCT SALES
    // =====================================================

    public List<ProductSalesResponse_Dto> getProductSales(
            User_entity user,
            LocalDate from,
            LocalDate to
    ) {

        Shop_entity shop = getUserShop(user);

        validateDates(from, to);

        LocalDateTime start =
                from.atStartOfDay();

        LocalDateTime end =
                to.plusDays(1).atStartOfDay();


        List<ProductSalesProjection> result =
                reportRepository.getProductSales(
                        shop.getId(),
                        start,
                        end
                );


        return result.stream()
                .map(product ->
                        ProductSalesResponse_Dto.builder()
                                .productId(
                                        product.getProductId()
                                )
                                .productName(
                                        product.getProductName()
                                )
                                .sku(
                                        product.getSku()
                                )
                                .quantitySold(
                                        product.getQuantitySold()
                                )
                                .revenue(
                                        product.getRevenue()
                                )
                                .build()
                )
                .toList();
    }


    // =====================================================
    // CUSTOMER SALES
    // =====================================================

    public List<CustomerSalesResponse_Dto> getCustomerSales(
            User_entity user,
            LocalDate from,
            LocalDate to
    ) {

        Shop_entity shop = getUserShop(user);

        validateDates(from, to);

        LocalDateTime start =
                from.atStartOfDay();

        LocalDateTime end =
                to.plusDays(1).atStartOfDay();


        List<CustomerSalesProjection> result =
                reportRepository.getCustomerSales(
                        shop.getId(),
                        start,
                        end
                );


        return result.stream()
                .map(customer ->
                        CustomerSalesResponse_Dto.builder()
                                .customerId(
                                        customer.getCustomerId()
                                )
                                .customerName(
                                        customer.getCustomerName()
                                )
                                .whatsappNumber(
                                        customer.getWhatsappNumber()
                                )
                                .address(
                                        customer.getAddress()
                                )
                                .invoiceCount(
                                        customer.getInvoiceCount()
                                )
                                .totalPurchase(
                                        customer.getTotalPurchase()
                                )
                                .build()
                )
                .toList();
    }


    // =====================================================
    // PAYMENT REPORT
    // =====================================================

    public PaymentReportResponse_Dto getPaymentReport(
            User_entity user,
            LocalDate from,
            LocalDate to
    ) {

        Shop_entity shop = getUserShop(user);

        validateDates(from, to);

        LocalDateTime start =
                from.atStartOfDay();

        LocalDateTime end =
                to.plusDays(1).atStartOfDay();


        List<Object[]> result =
                reportRepository.getPaymentReport(
                        shop.getId(),
                        start,
                        end
                );


        Object[] row = result.get(0);


        java.math.BigDecimal cashAmount = row[0] instanceof java.math.BigDecimal ? (java.math.BigDecimal) row[0] : (row[0] != null ? java.math.BigDecimal.valueOf(((Number) row[0]).doubleValue()) : java.math.BigDecimal.ZERO);
        java.math.BigDecimal upiAmount = row[1] instanceof java.math.BigDecimal ? (java.math.BigDecimal) row[1] : (row[1] != null ? java.math.BigDecimal.valueOf(((Number) row[1]).doubleValue()) : java.math.BigDecimal.ZERO);
        java.math.BigDecimal cardAmount = row[2] instanceof java.math.BigDecimal ? (java.math.BigDecimal) row[2] : (row[2] != null ? java.math.BigDecimal.valueOf(((Number) row[2]).doubleValue()) : java.math.BigDecimal.ZERO);
        java.math.BigDecimal totalAmount = row[3] instanceof java.math.BigDecimal ? (java.math.BigDecimal) row[3] : (row[3] != null ? java.math.BigDecimal.valueOf(((Number) row[3]).doubleValue()) : java.math.BigDecimal.ZERO);

        return PaymentReportResponse_Dto.builder()
                .cashAmount(cashAmount)
                .upiAmount(upiAmount)
                .cardAmount(cardAmount)
                .otherAmount(java.math.BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .build();
    }


    // =====================================================
    // VALIDATE DATE
    // =====================================================

    private void validateDates(
            LocalDate from,
            LocalDate to
    ) {

        if (from == null || to == null) {

            throw new RuntimeException(
                    "From and to dates are required"
            );
        }


        if (from.isAfter(to)) {

            throw new RuntimeException(
                    "From date cannot be after to date"
            );
        }
    }


    // =====================================================
    // GET USER SHOP
    // =====================================================

    private Shop_entity getUserShop(
            User_entity user
    ) {

        if (user == null) {

            throw new RuntimeException(
                    "User not authenticated"
            );
        }


        if (shopContextResolver.resolveActiveShop(user) == null) {

            throw new RuntimeException(
                    "User is not assigned to a shop"
            );
        }


        if (shopContextResolver.resolveActiveShop(user).getId() == null) {

            throw new RuntimeException(
                    "Shop ID is missing"
            );
        }


        return shopContextResolver.resolveActiveShop(user);
    }
}