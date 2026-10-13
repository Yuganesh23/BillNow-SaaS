package com.example.Billing;

import com.example.Billing.auth.JwtService;
import com.example.Billing.auth.userRepository;
import com.example.Billing.config.BranchContextHolder;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.invoice.CreateInvoiceRequest_Dto;
import com.example.Billing.invoice.InvoiceItemRequest_Dto;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.invoice.InvoiceResponse_Dto;
import com.example.Billing.invoice.InvoiceService;
import com.example.Billing.invoice.InvoiceStatus;
import com.example.Billing.payment.PaymentMethod;
import com.example.Billing.payment.PaymentRequest_Dto;
import com.example.Billing.payment.PaymentService;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.purchase.PurchaseItemRequest_Dto;
import com.example.Billing.purchase.PurchaseRequest_Dto;
import com.example.Billing.purchase.PurchaseService;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.support.TenantTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class BillingFlowIntegrationTest {
    @Autowired InvoiceService invoiceService;
    @Autowired PaymentService paymentService;
    @Autowired PurchaseService purchaseService;
    @Autowired InvoiceRepository invoices;
    @Autowired ProductRepository products;
    @Autowired userRepository users;
    @Autowired ShopRepository shops;
    @Autowired CustomerRepository customers;
    @Autowired SupplierRepository suppliers;
    @Autowired JwtService jwtService;

    TenantTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new TenantTestFixture(users, shops, products, customers, suppliers, jwtService);
    }

    @Test
    void invoiceCalculationUsesBackendTaxTotals() {
        var tenant = fixture.createTenant("Calculation");
        InvoiceResponse_Dto result = inTenant(tenant,
                () -> invoiceService.createInvoice(tenant.owner(), invoiceRequest(tenant, 2.0, "10.00"), "calc-1"));

        assertThat(result.getTaxableAmount()).isEqualByComparingTo("200.00");
        assertThat(result.getCgstTotal()).isEqualByComparingTo("18.00");
        assertThat(result.getSgstTotal()).isEqualByComparingTo("18.00");
        assertThat(result.getIgstTotal()).isEqualByComparingTo("0.00");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("226.00");
    }

    @Test
    void insufficientStockRollsBackInvoiceAndStock() {
        var tenant = fixture.createTenant("Insufficient");
        tenant.product().setStockQuantity(1.0);
        products.save(tenant.product());

        assertThatThrownBy(() -> inTenant(tenant,
                () -> invoiceService.createInvoice(tenant.owner(), invoiceRequest(tenant, 2.0, "0"), "stock-fail")))
                .hasMessageContaining("Insufficient stock");

        assertThat(products.findByIdAndShopId(tenant.product().getId(), tenant.shop().getId())
                .orElseThrow().getStockQuantity()).isEqualTo(1.0);
        assertThat(invoices.findByShopIdOrderByCreatedAtDesc(tenant.shop().getId())).isEmpty();
    }

    @Test
    void concurrentStockReductionNeverOversells() throws Exception {
        var tenant = fixture.createTenant("Concurrent stock");
        tenant.product().setStockQuantity(5.0);
        products.save(tenant.product());

        List<Result> results = runConcurrently(10, index -> () -> {
            try {
                inTenant(tenant, () -> invoiceService.createInvoice(
                        tenant.owner(), invoiceRequest(tenant, 1.0, "0"), "stock-" + index));
                return Result.SUCCESS;
            } catch (RuntimeException exception) {
                return Result.REJECTED;
            }
        });

        assertThat(results).filteredOn(Result.SUCCESS::equals).hasSize(5);
        assertThat(results).filteredOn(Result.REJECTED::equals).hasSize(5);
        assertThat(products.findByIdAndShopId(tenant.product().getId(), tenant.shop().getId())
                .orElseThrow().getStockQuantity()).isEqualTo(0.0);
    }

    @Test
    void invoiceNumbersAreUniqueAndSequentialUnderConcurrency() throws Exception {
        var tenant = fixture.createTenant("Concurrent number");

        List<InvoiceResponse_Dto> created = runConcurrently(8, index -> () -> inTenant(tenant,
                () -> invoiceService.createInvoice(
                        tenant.owner(), invoiceRequest(tenant, 1.0, "0"), "number-" + index)));

        List<String> numbers = created.stream()
                .map(InvoiceResponse_Dto::getInvoiceNumber)
                .sorted(Comparator.naturalOrder())
                .toList();
        assertThat(numbers).containsExactly(
                "INV-00001", "INV-00002", "INV-00003", "INV-00004",
                "INV-00005", "INV-00006", "INV-00007", "INV-00008");
    }

    @Test
    void fullPaymentMarksInvoicePaid() {
        var tenant = fixture.createTenant("Payment");
        InvoiceResponse_Dto invoice = inTenant(tenant,
                () -> invoiceService.createInvoice(tenant.owner(), invoiceRequest(tenant, 1.0, "0"), "payment-1"));
        PaymentRequest_Dto request = new PaymentRequest_Dto(
                invoice.getId(), PaymentMethod.UPI, invoice.getTotalAmount());

        var payment = inTenant(tenant, () -> paymentService.createPayment(tenant.owner(), request));

        assertThat(payment.getAmount()).isEqualByComparingTo(invoice.getTotalAmount());
        assertThat(invoices.findByIdAndShopId(invoice.getId(), tenant.shop().getId())
                .orElseThrow().getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void purchaseAddsStockAndSupplierBalance() {
        var tenant = fixture.createTenant("Purchase");
        PurchaseItemRequest_Dto item = new PurchaseItemRequest_Dto();
        item.setProductId(tenant.product().getId());
        item.setQuantity(new BigDecimal("3.00"));
        item.setPurchasePrice(new BigDecimal("50.00"));
        PurchaseRequest_Dto request = new PurchaseRequest_Dto();
        request.setSupplierId(tenant.supplier().getId());
        request.setItems(List.of(item));
        request.setDiscountAmount(BigDecimal.ZERO);
        request.setPaymentStatus("PENDING");

        var purchase = inTenant(tenant, () -> purchaseService.createPurchase(tenant.owner(), request));

        assertThat(purchase.getGrandTotal()).isEqualByComparingTo("177.00");
        assertThat(products.findByIdAndShopId(tenant.product().getId(), tenant.shop().getId())
                .orElseThrow().getStockQuantity()).isEqualTo(103.0);
        assertThat(suppliers.findByIdAndShopId(tenant.supplier().getId(), tenant.shop().getId())
                .orElseThrow().getOutstandingBalance()).isEqualByComparingTo("177.00");
    }

    private CreateInvoiceRequest_Dto invoiceRequest(
            TenantTestFixture.Tenant tenant, double quantity, String discount
    ) {
        InvoiceItemRequest_Dto item = new InvoiceItemRequest_Dto();
        item.setProductId(tenant.product().getId());
        item.setQuantity(quantity);
        CreateInvoiceRequest_Dto request = new CreateInvoiceRequest_Dto();
        request.setCustomerId(tenant.customer().getId());
        request.setItems(List.of(item));
        request.setDiscountAmount(new BigDecimal(discount));
        request.setIsInterState(false);
        return request;
    }

    private <T> T inTenant(TenantTestFixture.Tenant tenant, Callable<T> operation) {
        BranchContextHolder.setBranchId(tenant.shop().getId());
        try {
            return operation.call();
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AssertionError(exception);
        } finally {
            BranchContextHolder.clear();
        }
    }

    private <T> List<T> runConcurrently(int count, TaskFactory<T> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int index = 0; index < count; index++) futures.add(executor.submit(tasks.create(index)));
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) results.add(future.get());
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private enum Result { SUCCESS, REJECTED }

    @FunctionalInterface
    private interface TaskFactory<T> {
        Callable<T> create(int index);
    }
}
