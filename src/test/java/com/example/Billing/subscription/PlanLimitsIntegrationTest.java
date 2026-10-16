package com.example.Billing.subscription;

import com.example.Billing.auth.JwtService;
import com.example.Billing.auth.userRepository;
import com.example.Billing.common.AppException;
import com.example.Billing.config.BranchContextHolder;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.invoice.CreateInvoiceRequest_Dto;
import com.example.Billing.invoice.InvoiceItemRequest_Dto;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.invoice.InvoiceService;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.support.TenantTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "billing.plan-limits.trial.invoices-per-month=1")
class PlanLimitsIntegrationTest {
    @Autowired InvoiceService invoiceService;
    @Autowired InvoiceRepository invoices;
    @Autowired userRepository users;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired SupplierRepository suppliers;
    @Autowired JwtService jwt;
    TenantTestFixture fixture;

    @BeforeEach void setup() { fixture = new TenantTestFixture(users, shops, products, customers, suppliers, jwt); }

    @Test
    void monthlyInvoiceLimitIsTenantScopedAndIdempotentReplayStillWorks() {
        var a = fixture.createTenant("Limit A");
        var b = fixture.createTenant("Limit B");
        var bInvoice = inTenant(b, () -> invoiceService.createInvoice(b.owner(), request(b), "b-one"));
        assertThat(bInvoice.getId()).isNotNull();

        var first = inTenant(a, () -> invoiceService.createInvoice(a.owner(), request(a), "a-one"));
        var replay = inTenant(a, () -> invoiceService.createInvoice(a.owner(), request(a), "a-one"));
        assertThat(replay.getId()).isEqualTo(first.getId());
        assertThatThrownBy(() -> inTenant(a, () -> invoiceService.createInvoice(a.owner(), request(a), "a-two")))
                .isInstanceOf(AppException.class).hasMessageContaining("limit");
        assertThat(invoices.countByShopId(a.shop().getId())).isEqualTo(1);
    }

    private CreateInvoiceRequest_Dto request(TenantTestFixture.Tenant tenant) {
        InvoiceItemRequest_Dto item = new InvoiceItemRequest_Dto(); item.setProductId(tenant.product().getId()); item.setQuantity(1.0);
        CreateInvoiceRequest_Dto request = new CreateInvoiceRequest_Dto(); request.setCustomerId(tenant.customer().getId());
        request.setItems(List.of(item)); request.setDiscountAmount(BigDecimal.ZERO); request.setIsInterState(false); return request;
    }

    private <T> T inTenant(TenantTestFixture.Tenant tenant, Callable<T> callable) {
        BranchContextHolder.setBranchId(tenant.shop().getId());
        try { return callable.call(); } catch (RuntimeException exception) { throw exception; }
        catch (Exception exception) { throw new AssertionError(exception); } finally { BranchContextHolder.clear(); }
    }
}
