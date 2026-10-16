package com.example.Billing.reports;

import com.example.Billing.auth.JwtService;
import com.example.Billing.auth.userRepository;
import com.example.Billing.config.BranchContextHolder;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.invoice.*;
import com.example.Billing.invoice.whatsapp.WhatsAppStatus;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.payment.PaymentMethod;
import com.example.Billing.payment.PaymentRepository;
import com.example.Billing.payment.Payment_entity;
import com.example.Billing.purchase.PurchaseRepository;
import com.example.Billing.purchase.Purchase_entity;
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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportIntegrationTest {
    @Autowired AdvancedReportService reports;
    @Autowired InvoiceRepository invoices;
    @Autowired DocumentNoteRepository notes;
    @Autowired PurchaseRepository purchases;
    @Autowired PaymentRepository payments;
    @Autowired MockMvc mvc;
    @Autowired userRepository users; @Autowired ShopRepository shops; @Autowired ProductRepository products;
    @Autowired CustomerRepository customers; @Autowired SupplierRepository suppliers; @Autowired JwtService jwt;
    TenantTestFixture fixture;

    @BeforeEach void setup() { fixture = new TenantTestFixture(users, shops, products, customers, suppliers, jwt); }

    @Test void gstAndBusinessReportsDeriveExactTotalsAndCreditNotesReduceOutput() {
        var tenant = fixture.createTenant("Reports");
        Invoice_entity invoice = invoices.save(Invoice_entity.builder().shop(tenant.shop()).customer(tenant.customer())
                .biller(tenant.owner()).invoiceNumber("INV-RPT-1").subtotal(new BigDecimal("100.00"))
                .taxableAmount(new BigDecimal("100.00")).discountAmount(BigDecimal.ZERO)
                .cgstTotal(new BigDecimal("9.00")).sgstTotal(new BigDecimal("9.00"))
                .igstTotal(BigDecimal.ZERO).cessTotal(BigDecimal.ZERO).roundOff(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("118.00")).financialYear("2026-27").seriesNumber(1)
                .status(InvoiceStatus.PENDING).whatsappStatus(WhatsAppStatus.NOT_SENT).build());
        notes.save(DocumentNote_entity.builder().shop(tenant.shop()).originalInvoice(invoice).noteNumber("CN/1")
                .noteType("CREDIT_NOTE").financialYear("2026-27").seriesNumber(1)
                .amount(new BigDecimal("59.00")).reason("Partial return").restock(false).build());
        purchases.save(Purchase_entity.builder().shop(tenant.shop()).supplier(tenant.supplier()).createdBy(tenant.owner())
                .purchaseNumber("PUR-RPT-1").subtotal(new BigDecimal("50.00")).discountAmount(BigDecimal.ZERO)
                .taxAmount(new BigDecimal("9.00")).grandTotal(new BigDecimal("59.00")).status("PENDING")
                .taxableAmount(new BigDecimal("50.00")).cgstTotal(new BigDecimal("4.50"))
                .sgstTotal(new BigDecimal("4.50")).igstTotal(BigDecimal.ZERO).cessTotal(BigDecimal.ZERO)
                .amountPaid(BigDecimal.ZERO).build());

        ReportDtos.Gstr3bSummary gst = inTenant(tenant, () -> reports.gstr3b(tenant.owner(), LocalDate.now(), LocalDate.now()));
        assertThat(gst.outwardTaxable()).isEqualByComparingTo("50.00");
        assertThat(gst.outwardCgst()).isEqualByComparingTo("4.50");
        assertThat(gst.eligibleItcCgst()).isEqualByComparingTo("4.50");
        assertThat(gst.netTaxPayable()).isEqualByComparingTo("0.00");

        ReportDtos.BusinessSummary business = inTenant(tenant, () -> reports.business(tenant.owner(), LocalDate.now(), LocalDate.now()));
        assertThat(business.profitAndLoss().netSales()).isEqualByComparingTo("59.00");
        assertThat(business.profitAndLoss().grossProfit()).isEqualByComparingTo("0.00");
    }

    @Test void registerEndpointNeverReturnsAnotherTenantsDocuments() throws Exception {
        var a = fixture.createTenant("Report A"); var b = fixture.createTenant("Report B");
        invoices.save(Invoice_entity.builder().shop(a.shop()).customer(a.customer()).biller(a.owner())
                .invoiceNumber("A-PRIVATE").subtotal(BigDecimal.TEN).taxableAmount(BigDecimal.TEN)
                .discountAmount(BigDecimal.ZERO).cgstTotal(BigDecimal.ZERO).sgstTotal(BigDecimal.ZERO)
                .igstTotal(BigDecimal.ZERO).cessTotal(BigDecimal.ZERO).roundOff(BigDecimal.ZERO)
                .totalAmount(BigDecimal.TEN).status(InvoiceStatus.PENDING).whatsappStatus(WhatsAppStatus.NOT_SENT).build());
        mvc.perform(b.authenticate(get("/api/reports/v2/registers/sales")
                        .param("from", LocalDate.now().toString()).param("to", LocalDate.now().toString())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test void paymentRegisterIsPaginatedAndTenantScoped() throws Exception {
        var a = fixture.createTenant("Payment A"); var b = fixture.createTenant("Payment B");
        Invoice_entity invoiceA = invoice(a, "PAY-A");
        Invoice_entity invoiceB = invoice(b, "PAY-B");
        payments.save(Payment_entity.builder().invoice(invoiceA).paymentMethod(PaymentMethod.CASH)
                .amount(new BigDecimal("10.00")).reference("A-ONLY").build());
        payments.save(Payment_entity.builder().invoice(invoiceB).paymentMethod(PaymentMethod.UPI)
                .amount(new BigDecimal("5.00")).reference("B-FIRST").build());
        payments.save(Payment_entity.builder().invoice(invoiceB).paymentMethod(PaymentMethod.CARD)
                .amount(new BigDecimal("5.00")).reference("B-SECOND").build());

        mvc.perform(b.authenticate(get("/api/reports/v2/payments")
                        .param("from", LocalDate.now().toString()).param("to", LocalDate.now().toString())
                        .param("page", "0").param("size", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("A-ONLY"))));
    }

    @Test void reportPartyOptionsAndExportSettingsStayInsideActiveTenant() throws Exception {
        var a = fixture.createTenant("Options A"); var b = fixture.createTenant("Options B");
        String customSettings = """
                {"salesLedger":"A Sales","purchaseLedger":"A Purchases","cgstLedger":"A CGST",
                 "sgstLedger":"A SGST","igstLedger":"A IGST","cessLedger":"A Cess"}
                """;
        mvc.perform(a.authenticate(put("/api/reports/v2/export-settings")
                        .contentType("application/json").content(customSettings)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.salesLedger").value("A Sales"));

        mvc.perform(b.authenticate(get("/api/reports/v2/registers/sales/parties")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Options B Customer"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Options A Customer"))));
        mvc.perform(b.authenticate(get("/api/reports/v2/export-settings")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.salesLedger").value("Sales"));
    }

    private Invoice_entity invoice(TenantTestFixture.Tenant tenant, String number) {
        return invoices.save(Invoice_entity.builder().shop(tenant.shop()).customer(tenant.customer()).biller(tenant.owner())
                .invoiceNumber(number).subtotal(new BigDecimal("10.00")).taxableAmount(new BigDecimal("10.00"))
                .discountAmount(BigDecimal.ZERO).cgstTotal(BigDecimal.ZERO).sgstTotal(BigDecimal.ZERO)
                .igstTotal(BigDecimal.ZERO).cessTotal(BigDecimal.ZERO).roundOff(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("10.00")).status(InvoiceStatus.PENDING)
                .whatsappStatus(WhatsAppStatus.NOT_SENT).build());
    }

    private <T> T inTenant(TenantTestFixture.Tenant tenant, java.util.concurrent.Callable<T> task) {
        BranchContextHolder.setBranchId(tenant.shop().getId());
        try { return task.call(); } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException(e); } finally { BranchContextHolder.clear(); }
    }
}
