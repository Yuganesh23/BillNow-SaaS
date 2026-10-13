package com.example.Billing.auth;
import com.example.Billing.invoice.InvoiceController;
import com.example.Billing.product.ProductController;
import com.example.Billing.reports.ReportController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.assertj.core.api.Assertions.assertThat;

class RolePermissionMatrixTest {
    @Test void sensitiveEndpointsDeclareExpectedRoleMatrix() throws Exception {
        String cancel = InvoiceController.class.getMethod("cancelInvoice", org.springframework.security.core.Authentication.class, Long.class)
                .getAnnotation(PreAuthorize.class).value();
        assertThat(cancel).contains("SHOP_OWNER", "MANAGER").doesNotContain("BILLER", "VIEWER");
        String productUpdate = ProductController.class.getMethod("updateProduct", org.springframework.security.core.Authentication.class, Long.class,
                com.example.Billing.product.ProductRequest_Dto.class).getAnnotation(PreAuthorize.class).value();
        assertThat(productUpdate).contains("SHOP_OWNER", "MANAGER").doesNotContain("BILLER", "VIEWER");
        assertThat(ReportController.class.getDeclaredMethods()).filteredOn(m -> m.isAnnotationPresent(PreAuthorize.class))
                .allSatisfy(method -> assertThat(method.getAnnotation(PreAuthorize.class).value()).doesNotContain("BILLER"));
    }
}
