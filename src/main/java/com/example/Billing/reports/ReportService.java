package com.example.Billing.reports;

import com.example.Billing.auth.User_entity;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;


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


        Object[] row = result.get(0);


        long totalInvoices =
                ((Number) row[0]).longValue();

        BigDecimal subtotal =
                (BigDecimal) row[1];

        BigDecimal totalDiscount =
                (BigDecimal) row[2];

        BigDecimal totalSales =
                (BigDecimal) row[3];


        return SalesReportResponse_Dto.builder()
                .from(from)
                .to(to)
                .totalInvoices(totalInvoices)
                .subtotal(subtotal)
                .totalDiscount(totalDiscount)
                .totalSales(totalSales)
                .build();
    }


    // =====================================================
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


        BigDecimal cashAmount =
                (BigDecimal) row[0];

        BigDecimal upiAmount =
                (BigDecimal) row[1];

        BigDecimal cardAmount =
                (BigDecimal) row[2];

        BigDecimal otherAmount =
                (BigDecimal) row[3];

        BigDecimal totalAmount =
                (BigDecimal) row[4];


        return PaymentReportResponse_Dto.builder()
                .cashAmount(cashAmount)
                .upiAmount(upiAmount)
                .cardAmount(cardAmount)
                .otherAmount(otherAmount)
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


        if (user.getShop() == null) {

            throw new RuntimeException(
                    "User is not assigned to a shop"
            );
        }


        if (user.getShop().getId() == null) {

            throw new RuntimeException(
                    "Shop ID is missing"
            );
        }


        return user.getShop();
    }
}