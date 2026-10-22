package com.example.Billing.workflow;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.customer.*;
import com.example.Billing.gst.*;
import com.example.Billing.invoice.*;
import com.example.Billing.product.*;
import com.example.Billing.shop.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor
public class QuotationService {
    private final QuotationRepository quotations;
    private final ShopRepository shops;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final ShopContextResolver shopContext;
    private final DocumentNumberService numbers;
    private final TaxCalculationService taxes;
    private final InvoiceService invoices;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public Response create(User_entity user, Request request) {
        Long shopId = shopContext.resolveActiveShopId(user);
        Shop_entity shop = shops.findByIdForUpdate(shopId).orElseThrow(() -> missing("SHOP_NOT_FOUND", "Shop not found"));
        Customer_entity customer = customers.findByIdAndShopId(request.customerId(), shopId)
                .orElseThrow(() -> missing("CUSTOMER_NOT_FOUND", "Customer not found"));
        List<Product_entity> selected = new ArrayList<>();
        List<TaxCalculationService.LineInput> lines = new ArrayList<>();
        for (LineRequest line : request.items()) {
            Product_entity product = products.findByIdAndShopId(line.productId(), shopId)
                    .orElseThrow(() -> missing("PRODUCT_NOT_FOUND", "Product not found"));
            selected.add(product);
            lines.add(new TaxCalculationService.LineInput(String.valueOf(product.getId()), line.quantity(),
                    product.getSellingPrice(), product.getTaxType(), product.getGstRate(), product.getCessRate(), line.lineDiscount()));
        }
        String destination = customer.getGstin() != null && GstinValidator.isValid(customer.getGstin())
                ? GstinValidator.stateCode(customer.getGstin()) : customer.getStateCode();
        var calculation = taxes.calculate(new TaxCalculationService.Request(shop.getStateCode(), destination,
                shop.getRegistrationType(), request.discountAmount(), lines));
        WorkflowDocumentKind kind = request.documentKind() == null ? WorkflowDocumentKind.QUOTATION : request.documentKind();
        String prefix = switch (kind) { case QUOTATION -> "QT"; case ESTIMATE -> "EST"; case SALES_ORDER -> "SO"; case DELIVERY_CHALLAN -> "DC"; };
        var number = numbers.next(shop, kind.name(), prefix, LocalDate.now());
        Quotation_entity quote = Quotation_entity.builder().shop(shop).customer(customer).createdBy(user)
                .quoteNumber(number.number()).documentKind(kind).status(QuotationStatus.DRAFT).validUntil(request.validUntil())
                .taxableAmount(calculation.taxableAmount()).discountAmount(calculation.invoiceDiscount())
                .cgstTotal(calculation.cgst()).sgstTotal(calculation.sgst()).igstTotal(calculation.igst())
                .cessTotal(calculation.cess()).roundOff(calculation.roundOff()).totalAmount(calculation.total()).build();
        for (int index = 0; index < selected.size(); index++) {
            Product_entity product = selected.get(index); LineRequest input = request.items().get(index);
            var tax = calculation.lines().get(index);
            quote.getItems().add(QuotationItem_entity.builder().quotation(quote).product(product).quantity(input.quantity())
                    .unitPrice(product.getSellingPrice()).lineDiscount(input.lineDiscount() == null ? BigDecimal.ZERO : input.lineDiscount())
                    .taxableAmount(tax.taxableAmount()).gstRate(product.getGstRate()).cgst(tax.cgst()).sgst(tax.sgst())
                    .igst(tax.igst()).cess(tax.cess()).totalAmount(tax.total()).build());
        }
        return map(quotations.save(quote));
    }

    @Transactional
    public InvoiceResponse_Dto convert(User_entity user, Long id) {
        Long shopId = shopContext.resolveActiveShopId(user);
        Quotation_entity quote = quotations.findByIdAndShopId(id, shopId).orElseThrow(() -> missing("QUOTATION_NOT_FOUND", "Quotation not found"));
        if (quote.getConvertedInvoice() != null) return invoices.getInvoice(user, quote.getConvertedInvoice().getId());
        List<InvoiceItemRequest_Dto> items = quote.getItems().stream().map(line -> {
            InvoiceItemRequest_Dto item = new InvoiceItemRequest_Dto(); item.setProductId(line.getProduct().getId());
            item.setQuantity(line.getQuantity().doubleValue()); item.setLineDiscount(line.getLineDiscount()); return item;
        }).toList();
        CreateInvoiceRequest_Dto request = new CreateInvoiceRequest_Dto(); request.setCustomerId(quote.getCustomer().getId());
        request.setItems(items); request.setDiscountAmount(quote.getDiscountAmount());
        InvoiceResponse_Dto created = invoices.createInvoice(user, request, "quotation-" + quote.getId());
        quote.setConvertedInvoice(invoiceRepository.findByIdAndShopId(created.getId(), shopId)
                .orElseThrow(() -> missing("INVOICE_NOT_FOUND", "Converted invoice not found")));
        quote.setStatus(QuotationStatus.CONVERTED); quotations.save(quote); return created;
    }

    @Transactional(readOnly = true)
    public List<Response> list(User_entity user) { return quotations.findByShopIdOrderByCreatedAtDesc(shopContext.resolveActiveShopId(user)).stream().map(this::map).toList(); }
    private Response map(Quotation_entity q) { return new Response(q.getId(), q.getQuoteNumber(), q.getDocumentKind(), q.getCustomer().getId(), q.getCustomer().getName(), q.getStatus(), q.getValidUntil(), q.getTotalAmount(), q.getConvertedInvoice() == null ? null : q.getConvertedInvoice().getId()); }
    private AppException missing(String code, String message) { return new AppException(HttpStatus.NOT_FOUND, code, message); }
    public record LineRequest(Long productId, BigDecimal quantity, BigDecimal lineDiscount) {}
    public record Request(Long customerId, WorkflowDocumentKind documentKind, LocalDate validUntil, BigDecimal discountAmount, List<LineRequest> items) {}
    public record Response(Long id, String number, WorkflowDocumentKind documentKind, Long customerId, String customerName, QuotationStatus status, LocalDate validUntil, BigDecimal totalAmount, Long convertedInvoiceId) {}
}
