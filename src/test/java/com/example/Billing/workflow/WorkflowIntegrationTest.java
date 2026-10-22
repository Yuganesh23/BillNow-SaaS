package com.example.Billing.workflow;

import com.example.Billing.auth.*;
import com.example.Billing.config.BranchContextHolder;
import com.example.Billing.customer.*;
import com.example.Billing.invoice.*;
import com.example.Billing.payment.*;
import com.example.Billing.product.*;
import com.example.Billing.shop.*;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.support.TenantTestFixture;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class WorkflowIntegrationTest {
    @Autowired userRepository users; @Autowired ShopRepository shops; @Autowired ProductRepository products;
    @Autowired CustomerRepository customers; @Autowired SupplierRepository suppliers; @Autowired JwtService jwt;
    @Autowired QuotationService quotations; @Autowired InvoiceService invoices; @Autowired PaymentService payments;
    @Autowired InvoiceRepository invoiceRepository; @Autowired CustomerAgingService aging;
    @Autowired RecurringInvoiceService recurring; @Autowired CustomerRepository customerRepository;
    TenantTestFixture fixture;

    @BeforeEach void setup() { fixture = new TenantTestFixture(users, shops, products, customers, suppliers, jwt); }

    @Test void quotationConvertsOnceAndRejectsCrossTenantCustomer() {
        var a = fixture.createTenant("Quote A"); var b = fixture.createTenant("Quote B");
        var bad = new QuotationService.Request(b.customer().getId(), WorkflowDocumentKind.QUOTATION, LocalDate.now().plusDays(7), BigDecimal.ZERO,
                List.of(new QuotationService.LineRequest(a.product().getId(), BigDecimal.ONE, BigDecimal.ZERO)));
        assertThatThrownBy(() -> inTenant(a, () -> quotations.create(a.owner(), bad))).isInstanceOf(com.example.Billing.common.AppException.class);

        var request = new QuotationService.Request(a.customer().getId(), WorkflowDocumentKind.QUOTATION, LocalDate.now().plusDays(7), BigDecimal.ZERO,
                List.of(new QuotationService.LineRequest(a.product().getId(), BigDecimal.ONE, BigDecimal.ZERO)));
        var quote = inTenant(a, () -> quotations.create(a.owner(), request));
        var first = inTenant(a, () -> quotations.convert(a.owner(), quote.id()));
        var second = inTenant(a, () -> quotations.convert(a.owner(), quote.id()));
        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test void partialPaymentsSettleInvoiceAndPutOverpaymentIntoCredit() {
        var tenant = fixture.createTenant("Partial");
        var invoice = inTenant(tenant, () -> invoices.createInvoice(tenant.owner(), invoiceRequest(tenant), "partial-1"));
        inTenant(tenant, () -> payments.createPayment(tenant.owner(), new PaymentRequest_Dto(invoice.getId(), PaymentMethod.UPI, new BigDecimal("40"))));
        assertThat(invoiceRepository.findByIdAndShopId(invoice.getId(), tenant.shop().getId()).orElseThrow().getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
        inTenant(tenant, () -> payments.createPayment(tenant.owner(), new PaymentRequest_Dto(invoice.getId(), PaymentMethod.CASH, new BigDecimal("100"))));
        assertThat(invoiceRepository.findByIdAndShopId(invoice.getId(), tenant.shop().getId()).orElseThrow().getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(customerRepository.findByIdAndShopId(tenant.customer().getId(), tenant.shop().getId()).orElseThrow().getCreditBalance())
                .isEqualByComparingTo("22.00");
    }

    @Test void agingAndRecurringRunAreTenantSafeAndIdempotent() {
        var tenant = fixture.createTenant("Recurring");
        var schedule = new RecurringInvoiceService.Request(tenant.customer().getId(), RecurringFrequency.MONTHLY, LocalDate.now(), false,
                BigDecimal.ZERO, List.of(new RecurringInvoiceService.LineRequest(tenant.product().getId(), BigDecimal.ONE, BigDecimal.ZERO)));
        Long id = inTenant(tenant, () -> recurring.create(tenant.owner(), schedule));
        var first = inTenant(tenant, () -> recurring.run(tenant.owner(), id, LocalDate.now()));
        var second = inTenant(tenant, () -> recurring.run(tenant.owner(), id, LocalDate.now()));
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(inTenant(tenant, () -> aging.calculate(tenant.owner(), LocalDate.now()))).singleElement()
                .satisfies(row -> assertThat(row.days0To30()).isEqualByComparingTo(first.getTotalAmount()));
    }

    private CreateInvoiceRequest_Dto invoiceRequest(TenantTestFixture.Tenant tenant) {
        InvoiceItemRequest_Dto item = new InvoiceItemRequest_Dto(); item.setProductId(tenant.product().getId()); item.setQuantity(1.0);
        CreateInvoiceRequest_Dto request = new CreateInvoiceRequest_Dto(); request.setCustomerId(tenant.customer().getId());
        request.setItems(List.of(item)); request.setDiscountAmount(BigDecimal.ZERO); return request;
    }
    private <T> T inTenant(TenantTestFixture.Tenant tenant, java.util.concurrent.Callable<T> task) {
        BranchContextHolder.setBranchId(tenant.shop().getId()); try { return task.call(); } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException(e); } finally { BranchContextHolder.clear(); }
    }
}
