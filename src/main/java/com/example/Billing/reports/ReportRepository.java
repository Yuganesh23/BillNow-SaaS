package com.example.Billing.reports;

import com.example.Billing.invoice.Invoice_entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository
        extends JpaRepository<Invoice_entity, Long> {


    // =====================================================
    // SALES REPORT
    // =====================================================

    @Query("""
            SELECT COUNT(i),
                   COALESCE(SUM(i.subtotal), 0),
                   COALESCE(SUM(i.discountAmount), 0),
                   COALESCE(SUM(i.totalAmount), 0)
            FROM Invoice_entity i
            WHERE i.shop.id = :shopId
            AND i.status <> com.example.Billing.invoice.InvoiceStatus.CANCELLED
            AND i.createdAt >= :from
            AND i.createdAt < :to
            """)
    List<Object[]> getSalesReport(
            @Param("shopId") Long shopId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );


    // =====================================================
    // PRODUCT SALES REPORT
    // =====================================================

    @Query("""
            SELECT
                p.id AS productId,
                p.name AS productName,
                p.sku AS sku,
                SUM(ii.quantity) AS quantitySold,
                SUM(ii.totalPrice) AS revenue
            FROM InvoiceItem ii
            JOIN ii.invoice i
            JOIN ii.product p
            WHERE i.shop.id = :shopId
            AND i.status <> com.example.Billing.invoice.InvoiceStatus.CANCELLED
            AND i.createdAt >= :from
            AND i.createdAt < :to
            GROUP BY p.id, p.name, p.sku
            ORDER BY SUM(ii.totalPrice) DESC
            """)
    List<ProductSalesProjection> getProductSales(
            @Param("shopId") Long shopId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );


    // =====================================================
    // CUSTOMER SALES REPORT
    // =====================================================

    @Query("""
            SELECT
                c.id AS customerId,
                c.name AS customerName,
                c.whatsappNumber AS whatsappNumber,
                COUNT(i.id) AS invoiceCount,
                COALESCE(SUM(i.totalAmount), 0) AS totalPurchase
            FROM Invoice_entity i
            JOIN i.customer c
            WHERE i.shop.id = :shopId
            AND i.status <> com.example.Billing.invoice.InvoiceStatus.CANCELLED
            AND i.createdAt >= :from
            AND i.createdAt < :to
            GROUP BY c.id, c.name, c.whatsappNumber
            ORDER BY SUM(i.totalAmount) DESC
            """)
    List<CustomerSalesProjection> getCustomerSales(
            @Param("shopId") Long shopId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );


    // =====================================================
    // PAYMENT REPORT
    // =====================================================

    @Query("""
            SELECT
                COALESCE(SUM(
                    CASE
                        WHEN p.paymentMethod =
                        com.example.Billing.payment.PaymentMethod.CASH
                        THEN p.amount
                        ELSE 0
                    END
                ), 0),

                COALESCE(SUM(
                    CASE
                        WHEN p.paymentMethod =
                        com.example.Billing.payment.PaymentMethod.UPI
                        THEN p.amount
                        ELSE 0
                    END
                ), 0),

                COALESCE(SUM(
                    CASE
                        WHEN p.paymentMethod =
                        com.example.Billing.payment.PaymentMethod.CARD
                        THEN p.amount
                        ELSE 0
                    END
                ), 0),

                COALESCE(SUM(
                    CASE
                        WHEN p.paymentMethod NOT IN (
                            com.example.Billing.payment.PaymentMethod.CASH,
                            com.example.Billing.payment.PaymentMethod.UPI,
                            com.example.Billing.payment.PaymentMethod.CARD
                        )
                        THEN p.amount
                        ELSE 0
                    END
                ), 0),

                COALESCE(SUM(p.amount), 0)

            FROM Payment_entity p
            JOIN p.invoice i
            WHERE i.shop.id = :shopId
            AND i.status <> com.example.Billing.invoice.InvoiceStatus.CANCELLED
            AND p.paidAt >= :from
            AND p.paidAt < :to
            """)
    List<Object[]> getPaymentReport(
            @Param("shopId") Long shopId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}