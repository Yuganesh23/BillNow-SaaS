package com.example.Billing.compliance;

import com.example.Billing.auth.JwtService;
import com.example.Billing.auth.userRepository;
import com.example.Billing.config.BranchContextHolder;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.invoice.*;
import com.example.Billing.invoice.whatsapp.WhatsAppStatus;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.SubscriptionTier;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.support.TenantTestFixture;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EInvoiceIntegrationTest {
    @Autowired EInvoiceService service; @Autowired EWayBillService eWayBillService;
    @Autowired EInvoiceRecordRepository records; @Autowired EWayBillRecordRepository eWayBillRecords; @Autowired InvoiceRepository invoices;
    @Autowired userRepository users; @Autowired ShopRepository shops; @Autowired ProductRepository products;
    @Autowired CustomerRepository customers; @Autowired SupplierRepository suppliers; @Autowired JwtService jwt;
    @Autowired MockMvc mvc;
    @Autowired MockEInvoiceProvider mockProvider;
    TenantTestFixture fixture;
    @BeforeEach void setup(){fixture=new TenantTestFixture(users,shops,products,customers,suppliers,jwt);}

    @Test void generationIsIdempotentLocksInvoiceAndStaysInsideTenant() throws Exception {
        var a=fixture.createTenant("IRN A"); var b=fixture.createTenant("IRN B");
        prepare(a); Invoice_entity invoice=invoice(a);
        EInvoiceService.Response first=inTenant(a,()->{service.saveSettings(a.owner(),new EInvoiceService.Settings(true,false,24));return service.generate(a.owner(),invoice.getId());});
        EInvoiceService.Response second=inTenant(a,()->service.generate(a.owner(),invoice.getId()));
        assertThat(first.status()).isEqualTo(ComplianceStatus.GENERATED);
        assertThat(second.irn()).isEqualTo(first.irn());
        assertThat(records.findByInvoiceIdAndShopId(invoice.getId(),a.shop().getId())).isPresent();
        assertThat(invoices.findByIdAndShopId(invoice.getId(),a.shop().getId()).orElseThrow().isComplianceLocked()).isTrue();
        inTenant(b,()->service.saveSettings(b.owner(),new EInvoiceService.Settings(true,false,24)));
        mvc.perform(b.authenticate(post("/api/compliance/invoices/{id}/generate",invoice.getId())))
                .andExpect(status().isNotFound());
    }

    @Test void eWayBillGenerationIsIdempotentAndTenantScoped() throws Exception {
        var a=fixture.createTenant("EWB A"); var b=fixture.createTenant("EWB B");
        prepare(a); Invoice_entity invoice=invoice(a);
        EWayBillService.Request request=new EWayBillService.Request("27AAAAA0000A1Z5","Fast Transport",null,null,"ROAD","MH12AB1234","REGULAR",125);
        EWayBillService.Response first=inTenant(a,()->{service.saveSettings(a.owner(),new EInvoiceService.Settings(false,true,24));return eWayBillService.generate(a.owner(),invoice.getId(),request);});
        EWayBillService.Response second=inTenant(a,()->eWayBillService.generate(a.owner(),invoice.getId(),request));
        assertThat(first.status()).isEqualTo(ComplianceStatus.GENERATED);
        assertThat(second.eWayBillNumber()).isEqualTo(first.eWayBillNumber());
        assertThat(eWayBillRecords.findByInvoiceIdAndShopId(invoice.getId(),a.shop().getId())).isPresent();
        mvc.perform(b.authenticate(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/compliance/e-way-bills/invoices/{id}",invoice.getId())))
                .andExpect(status().isNotFound());
    }

    @Test void complianceFeaturesAreDisabledUntilOwnerOptsIn() {
        var tenant=fixture.createTenant("Disabled"); prepare(tenant); Invoice_entity invoice=invoice(tenant);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> inTenant(tenant,()->service.generate(tenant.owner(),invoice.getId())))
                .isInstanceOf(com.example.Billing.common.AppException.class)
                .hasMessageContaining("disabled");
    }

    @Test void failedProviderAttemptIsStoredAndCanBeRetried() {
        var tenant=fixture.createTenant("Retry"); prepare(tenant); Invoice_entity invoice=invoice(tenant);
        EInvoiceService.Response failed=inTenant(tenant,()->{
            service.saveSettings(tenant.owner(),new EInvoiceService.Settings(true,false,24));
            mockProvider.failNextEInvoice("simulated timeout");
            return service.generate(tenant.owner(),invoice.getId());
        });
        assertThat(failed.status()).isEqualTo(ComplianceStatus.FAILED);
        assertThat(failed.lastError()).contains("retry");
        EInvoiceService.Response retried=inTenant(tenant,()->service.generate(tenant.owner(),invoice.getId()));
        assertThat(retried.status()).isEqualTo(ComplianceStatus.GENERATED);
        assertThat(retried.attemptCount()).isEqualTo(2);
    }

    @Test void validationExplainsMissingComplianceFields() {
        var tenant=fixture.createTenant("Validation"); Invoice_entity invoice=invoice(tenant);
        tenant.shop().setSubscriptionTier(SubscriptionTier.PRO); shops.save(tenant.shop());
        inTenant(tenant,()->service.saveSettings(tenant.owner(),new EInvoiceService.Settings(true,false,24)));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> inTenant(tenant,()->service.generate(tenant.owner(),invoice.getId())))
                .isInstanceOf(com.example.Billing.common.AppException.class)
                .hasMessageContaining("Seller GSTIN").hasMessageContaining("Buyer GSTIN");
    }

    private void prepare(TenantTestFixture.Tenant t){
        t.shop().setSubscriptionTier(SubscriptionTier.PRO);t.shop().setGstin("27AAPFU0939F1ZV");t.shop().setLegalName("IRN Seller Pvt Ltd");t.shop().setLocality("Mumbai");t.shop().setPincode("400001");shops.save(t.shop());
        t.customer().setGstin("27AAPFU0939F1ZV");t.customer().setAddress("Buyer address");t.customer().setLocality("Mumbai");t.customer().setPincode("400002");t.customer().setStateCode("27");customers.save(t.customer());
        t.product().setHsnCode("1001");products.save(t.product());
    }
    private Invoice_entity invoice(TenantTestFixture.Tenant t){
        Invoice_entity i=Invoice_entity.builder().shop(t.shop()).customer(t.customer()).biller(t.owner()).invoiceNumber("INV-IRN-1")
                .subtotal(new BigDecimal("100.00")).taxableAmount(new BigDecimal("100.00")).discountAmount(BigDecimal.ZERO)
                .cgstTotal(new BigDecimal("9.00")).sgstTotal(new BigDecimal("9.00")).igstTotal(BigDecimal.ZERO).cessTotal(BigDecimal.ZERO)
                .roundOff(BigDecimal.ZERO).totalAmount(new BigDecimal("118.00")).status(InvoiceStatus.PENDING).whatsappStatus(WhatsAppStatus.NOT_SENT).build();
        i.getItems().add(InvoiceItem.builder().invoice(i).product(t.product()).quantity(1.0).unitPrice(new BigDecimal("100.00"))
                .taxableAmount(new BigDecimal("100.00")).gstRate(new BigDecimal("18.00")).cgst(new BigDecimal("9.00"))
                .sgst(new BigDecimal("9.00")).igst(BigDecimal.ZERO).cess(BigDecimal.ZERO).lineDiscount(BigDecimal.ZERO).totalPrice(new BigDecimal("118.00")).build());
        return invoices.save(i);
    }
    private <T>T inTenant(TenantTestFixture.Tenant t,java.util.concurrent.Callable<T> task){BranchContextHolder.setBranchId(t.shop().getId());try{return task.call();}catch(RuntimeException e){throw e;}catch(Exception e){throw new RuntimeException(e);}finally{BranchContextHolder.clear();}}
}
