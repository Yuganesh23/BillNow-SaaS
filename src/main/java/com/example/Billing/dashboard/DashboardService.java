package com.example.Billing.dashboard;

import com.example.Billing.auth.User_entity;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.invoice.InvoiceStatus;
import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.payment.PaymentMethod;
import com.example.Billing.payment.PaymentRepository;
import com.example.Billing.payment.Payment_entity;
import com.example.Billing.product.ProductRepository;
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
public class DashboardService {

    private final InvoiceRepository invoiceRepository;

    private final CustomerRepository customerRepository;

    private final ProductRepository productRepository;

    private final PaymentRepository paymentRepository;


    // =====================================================
    // GET DASHBOARD
    // =====================================================

    public DashboardResponse_Dto getDashboard(
            User_entity user
    ) {

        // -------------------------
        // GET SHOP
        // -------------------------

        Shop_entity shop =
                getUserShop(user);

        Long shopId =
                shop.getId();


        // =================================================
        // GET INVOICES
        // =================================================

        List<Invoice_entity> invoices =
                invoiceRepository
                        .findByShopIdOrderByCreatedAtDesc(
                                shopId
                        );


        // =================================================
        // TODAY
        // =================================================

        LocalDate today =
                LocalDate.now();


        LocalDateTime startOfDay =
                today.atStartOfDay();


        LocalDateTime endOfDay =
                today.plusDays(1)
                        .atStartOfDay();


        // =================================================
        // SALES CALCULATION
        // =================================================

        BigDecimal todaySales =
                BigDecimal.ZERO;


        BigDecimal totalSales =
                BigDecimal.ZERO;


        long todayInvoices = 0;


        for (Invoice_entity invoice :
                invoices) {

            // Ignore cancelled invoices

            if (invoice.getStatus()
                    == InvoiceStatus.CANCELLED) {

                continue;
            }


            BigDecimal amount =
                    invoice.getTotalAmount();


            if (amount == null) {

                amount =
                        BigDecimal.ZERO;
            }


            // -------------------------
            // TOTAL SALES
            // -------------------------

            totalSales =
                    totalSales.add(amount);


            // -------------------------
            // TODAY SALES
            // -------------------------

            LocalDateTime createdAt =
                    invoice.getCreatedAt();


            if (createdAt != null &&
                    !createdAt.isBefore(startOfDay) &&
                    createdAt.isBefore(endOfDay)) {

                todaySales =
                        todaySales.add(amount);

                todayInvoices++;
            }
        }


        // =================================================
        // CUSTOMER COUNT
        // =================================================

        long totalCustomers =
                customerRepository.countByShopId(
                        shopId
                );


        // =================================================
        // PRODUCT COUNT
        // =================================================

        long totalProducts =
                productRepository.countByShopId(
                        shopId
                );


        // =================================================
        // LOW STOCK
        // =================================================

        long lowStockProducts =
                productRepository
                        .countByShopIdAndStockQuantityLessThanEqual(
                                shopId,
                                5
                        );


        // =================================================
        // PAYMENT SUMMARY
        // =================================================

        BigDecimal cashPayments =
                BigDecimal.ZERO;

        BigDecimal upiPayments =
                BigDecimal.ZERO;

        BigDecimal cardPayments =
                BigDecimal.ZERO;

        BigDecimal otherPayments =
                BigDecimal.ZERO;


        // Get payments belonging to this shop

        List<Payment_entity> payments =
                paymentRepository
                        .findByInvoiceShopId(
                                shopId
                        );


        for (Payment_entity payment :
                payments) {

            BigDecimal amount =
                    payment.getAmount();


            if (amount == null) {

                continue;
            }


            PaymentMethod method =
                    payment.getPaymentMethod();


            if (method == PaymentMethod.CASH) {

                cashPayments =
                        cashPayments.add(amount);

            } else if (method == PaymentMethod.UPI) {

                upiPayments =
                        upiPayments.add(amount);

            } else if (method == PaymentMethod.CARD) {

                cardPayments =
                        cardPayments.add(amount);

            } else {

                otherPayments =
                        otherPayments.add(amount);
            }
        }


        // =================================================
        // RECENT INVOICES
        // =================================================

        List<RecentInvoice_Dto> recentInvoices =
                invoices
                        .stream()
                        .limit(5)
                        .map(this::mapRecentInvoice)
                        .toList();


        // =================================================
        // RESPONSE
        // =================================================

        return DashboardResponse_Dto.builder()

                .todaySales(
                        todaySales
                )

                .totalSales(
                        totalSales
                )

                .todayInvoices(
                        todayInvoices
                )

                .totalInvoices(
                        (long) invoices.size()
                )

                .totalCustomers(
                        totalCustomers
                )

                .totalProducts(
                        totalProducts
                )

                .lowStockProducts(
                        lowStockProducts
                )

                .cashPayments(
                        cashPayments
                )

                .upiPayments(
                        upiPayments
                )

                .cardPayments(
                        cardPayments
                )

                .otherPayments(
                        otherPayments
                )

                .recentInvoices(
                        recentInvoices
                )

                .build();
    }


    // =====================================================
    // USER SHOP
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


    // =====================================================
    // RECENT INVOICE MAPPER
    // =====================================================

    private RecentInvoice_Dto mapRecentInvoice(
            Invoice_entity invoice
    ) {

        return RecentInvoice_Dto.builder()

                .id(
                        invoice.getId()
                )

                .invoiceNumber(
                        invoice.getInvoiceNumber()
                )

                .customerId(
                        invoice.getCustomer().getId()
                )

                .customerName(
                        invoice.getCustomer().getName()
                )

                .totalAmount(
                        invoice.getTotalAmount()
                )

                .status(
                        invoice.getStatus()
                )

                .createdAt(
                        invoice.getCreatedAt()
                )

                .build();
    }
}