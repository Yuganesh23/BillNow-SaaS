package com.example.Billing;

import com.example.Billing.auth.JwtService;
import com.example.Billing.auth.userRepository;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.invoice.InvoiceStatus;
import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.invoice.whatsapp.WhatsAppStatus;
import com.example.Billing.portal.CustomerPortalAccessRepository;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.support.TenantTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.frontend-url=https://app.example.test")
@AutoConfigureMockMvc
@Transactional
class CustomerPortalIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired userRepository users;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired SupplierRepository suppliers;
    @Autowired InvoiceRepository invoices;
    @Autowired CustomerPortalAccessRepository accessRepository;
    @Autowired JwtService jwtService;

    TenantTestFixture.Tenant tenantA;
    TenantTestFixture.Tenant tenantB;

    @BeforeEach
    void setUp() {
        var fixture = new TenantTestFixture(users, shops, products, customers, suppliers, jwtService);
        tenantA = fixture.createTenant("Portal A");
        tenantB = fixture.createTenant("Portal B");
    }

    @Test
    void linkIsHashedAndPortalListsOnlyTheLinkedCustomerInvoices() throws Exception {
        Invoice_entity invoiceA = createInvoice(tenantA, "PORTAL-A");
        createInvoice(tenantB, "PORTAL-B");

        String body = mvc.perform(tenantA.authenticate(post("/api/customers/{id}/portal-access", tenantA.customer().getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String url = responseUrl(body);
        String token = URI.create(url).getQuery().substring("token=".length());

        assertThat(token).hasSizeGreaterThan(40);
        assertThat(accessRepository.findAll()).singleElement()
                .extracting(access -> access.getTokenHash()).isNotEqualTo(token);

        mvc.perform(get("/api/portal").header("X-Portal-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shopName").value(tenantA.shop().getName()))
                .andExpect(jsonPath("$.invoices.length()").value(1))
                .andExpect(jsonPath("$.invoices[0].id").value(invoiceA.getId()));
        mvc.perform(get("/api/portal/invoices/{id}/pdf", invoiceA.getId())
                        .header("X-Portal-Token", token))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType()).isEqualTo("application/pdf"));
    }

    @Test
    void ownerCannotMintAccessForAnotherTenantAndTokenCannotReadForeignInvoice() throws Exception {
        Invoice_entity foreignInvoice = createInvoice(tenantB, "FOREIGN");
        mvc.perform(tenantA.authenticate(post("/api/customers/{id}/portal-access", tenantB.customer().getId())))
                .andExpect(status().isNotFound());

        String token = createToken(tenantA);
        mvc.perform(get("/api/portal/invoices/{id}/pdf", foreignInvoice.getId())
                        .header("X-Portal-Token", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void revocationImmediatelyInvalidatesThePortalToken() throws Exception {
        String token = createToken(tenantA);
        mvc.perform(tenantA.authenticate(delete("/api/customers/{id}/portal-access", tenantA.customer().getId())))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/portal").header("X-Portal-Token", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PORTAL_TOKEN_REVOKED"));
    }

    private String createToken(TenantTestFixture.Tenant tenant) throws Exception {
        String body = mvc.perform(tenant.authenticate(post("/api/customers/{id}/portal-access", tenant.customer().getId())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String url = responseUrl(body);
        return URI.create(url).getQuery().substring("token=".length());
    }

    private String responseUrl(String body) {
        var matcher = Pattern.compile("\\\"url\\\":\\\"([^\\\"]+)\\\"").matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private Invoice_entity createInvoice(TenantTestFixture.Tenant tenant, String number) {
        return invoices.save(Invoice_entity.builder()
                .shop(tenant.shop()).customer(tenant.customer()).biller(tenant.owner())
                .invoiceNumber(number).subtotal(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("118.00"))
                .taxableAmount(new BigDecimal("100.00")).cgstTotal(new BigDecimal("9.00"))
                .sgstTotal(new BigDecimal("9.00")).igstTotal(BigDecimal.ZERO)
                .status(InvoiceStatus.PENDING).whatsappStatus(WhatsAppStatus.NOT_SENT).build());
    }
}
