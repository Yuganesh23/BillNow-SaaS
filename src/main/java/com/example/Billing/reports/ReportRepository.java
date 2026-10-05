package com.example.Billing.reports;

import org.springframework.data.domain.Pageable;
import com.example.Billing.invoice.Invoice_entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository
        extends JpaRepository<Invoice_entity, Long> {

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

    @Query("""
            SELECT
                c.id AS customerId,
                c.name AS customerName,
                c.whatsappNumber AS whatsappNumber,
                c.address AS address,
                COUNT(i.id) AS invoiceCount,
                COALESCE(SUM(i.totalAmount), 0) AS totalPurchase
            FROM Invoice_entity i
            JOIN i.customer c
            WHERE i.shop.id = :shopId
            AND i.status <> com.example.Billing.invoice.InvoiceStatus.CANCELLED
            AND i.createdAt >= :from
            AND i.createdAt < :to
            GROUP BY c.id, c.name, c.whatsappNumber, c.address
            ORDER BY SUM(i.totalAmount) DESC
            """)
    List<CustomerSalesProjection> getCustomerSales(
            @Param("shopId") Long shopId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            SELECT
                COALESCE(SUM(CASE WHEN p.paymentMethod = com.example.Billing.payment.PaymentMethod.CASH THEN p.amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN p.paymentMethod = com.example.Billing.payment.PaymentMethod.UPI THEN p.amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN p.paymentMethod = com.example.Billing.payment.PaymentMethod.CARD THEN p.amount ELSE 0 END), 0),
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

    @Query("""
            SELECT 
                ii.quantity,
                p.purchasePrice
            FROM InvoiceItem ii
            JOIN ii.invoice i
            JOIN ii.product p
            WHERE i.shop.id = :shopId
            AND i.status <> com.example.Billing.invoice.InvoiceStatus.CANCELLED
            AND i.createdAt >= :from
            AND i.createdAt < :to
            """)
    List<Object[]> getSalesCostAndItems(
            @Param("shopId") Long shopId,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to
    );
    
    @Query("""
            SELECT 
                FUNCTION('DATE', i.createdAt) AS saleDate,
                SUM(i.totalAmount) AS dailyRevenue
            FROM Invoice_entity i
            WHERE i.shop.id = :shopId
            AND i.status <> com.example.Billing.invoice.InvoiceStatus.CANCELLED
            AND i.createdAt >= :from
            AND i.createdAt < :to
            GROUP BY FUNCTION('DATE', i.createdAt)
            ORDER BY SUM(i.totalAmount) DESC
            """)
    List<Object[]> getHighestRevenueDay(
            @Param("shopId") Long shopId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
    
    @Query("""
            SELECT
                c.id AS customerId,
                c.name AS customerName,
                c.whatsappNumber AS whatsappNumber,
                c.address AS address,
                0L AS invoiceCount,
                0.0 AS totalPurchase
            FROM Customer_entity c
            WHERE c.shop.id = :shopId
            AND (SELECT MIN(i.createdAt) FROM Invoice_entity i WHERE i.customer.id = c.id) >= :from
            AND (SELECT MIN(i.createdAt) FROM Invoice_entity i WHERE i.customer.id = c.id) < :to
            ORDER BY c.id DESC
            """)
    List<CustomerSalesProjection> getNewCustomers(
            @Param("shopId") Long shopId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
