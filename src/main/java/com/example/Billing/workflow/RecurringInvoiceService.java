package com.example.Billing.workflow;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.invoice.*;
import com.example.Billing.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor
public class RecurringInvoiceService {
    private final RecurringInvoiceRepository recurring;
    private final RecurringInvoiceRunRepository runs;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final InvoiceRepository invoices;
    private final InvoiceService invoiceService;
    private final ShopContextResolver shopContext;

    @Transactional
    public Long create(User_entity user, Request request) {
        Long shopId = shopContext.resolveActiveShopId(user);
        var customer = customers.findByIdAndShopId(request.customerId(), shopId).orElseThrow(() -> missing("CUSTOMER_NOT_FOUND"));
        RecurringInvoice_entity schedule = RecurringInvoice_entity.builder().shop(customer.getShop()).customer(customer)
                .createdBy(user).frequency(request.frequency()).nextRunDate(request.nextRunDate())
                .issueAutomatically(request.issueAutomatically()).discountAmount(request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount()).build();
        for (LineRequest line : request.items()) {
            var product = products.findByIdAndShopId(line.productId(), shopId).orElseThrow(() -> missing("PRODUCT_NOT_FOUND"));
            schedule.getItems().add(RecurringInvoiceItem_entity.builder().recurringInvoice(schedule).product(product)
                    .quantity(line.quantity()).lineDiscount(line.lineDiscount() == null ? BigDecimal.ZERO : line.lineDiscount()).build());
        }
        return recurring.save(schedule).getId();
    }

    @Transactional
    public InvoiceResponse_Dto run(User_entity user, Long id, LocalDate scheduledDate) {
        Long shopId = shopContext.resolveActiveShopId(user);
        RecurringInvoice_entity schedule = recurring.findByIdAndShopIdForUpdate(id, shopId).orElseThrow(() -> missing("RECURRING_NOT_FOUND"));
        var existing = runs.findByRecurringInvoiceIdAndScheduledDate(id, scheduledDate);
        if (existing.isPresent()) return invoiceService.getInvoice(user, existing.get().getInvoice().getId());
        if (!schedule.isActive() || scheduledDate.isBefore(schedule.getNextRunDate())) throw new AppException(HttpStatus.CONFLICT, "RECURRING_NOT_DUE", "Recurring invoice is not due");
        List<InvoiceItemRequest_Dto> lines = schedule.getItems().stream().map(item -> { var line = new InvoiceItemRequest_Dto();
            line.setProductId(item.getProduct().getId()); line.setQuantity(item.getQuantity().doubleValue()); line.setLineDiscount(item.getLineDiscount()); return line; }).toList();
        CreateInvoiceRequest_Dto request = new CreateInvoiceRequest_Dto(); request.setCustomerId(schedule.getCustomer().getId());
        request.setItems(lines); request.setDiscountAmount(schedule.getDiscountAmount());
        InvoiceResponse_Dto result = invoiceService.createInvoice(user, request, "recurring-" + id + "-" + scheduledDate);
        var invoice = invoices.findByIdAndShopId(result.getId(), shopId).orElseThrow(() -> missing("INVOICE_NOT_FOUND"));
        runs.save(RecurringInvoiceRun_entity.builder().recurringInvoice(schedule).scheduledDate(scheduledDate).invoice(invoice).build());
        schedule.setNextRunDate(next(schedule.getFrequency(), scheduledDate)); recurring.save(schedule); return result;
    }

    static LocalDate next(RecurringFrequency frequency, LocalDate date) { return switch (frequency) { case WEEKLY -> date.plusWeeks(1); case MONTHLY -> date.plusMonths(1); case YEARLY -> date.plusYears(1); }; }
    private AppException missing(String code) { return new AppException(HttpStatus.NOT_FOUND, code, "Resource not found"); }
    public record LineRequest(Long productId, BigDecimal quantity, BigDecimal lineDiscount) {}
    public record Request(Long customerId, RecurringFrequency frequency, LocalDate nextRunDate, boolean issueAutomatically, BigDecimal discountAmount, List<LineRequest> items) {}
}
