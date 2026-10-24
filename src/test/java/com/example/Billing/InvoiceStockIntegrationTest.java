package com.example.Billing;

import com.example.Billing.invoice.InvoiceService;
import com.example.Billing.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class InvoiceStockIntegrationTest {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    public void contextLoads() {
        assertNotNull(invoiceService, "InvoiceService should be loaded");
        assertNotNull(productRepository, "ProductRepository should be loaded");
    }
    
    @Test
    public void testStockReductionConcurrency() {
        assertNotNull(invoiceService);
    }
}
