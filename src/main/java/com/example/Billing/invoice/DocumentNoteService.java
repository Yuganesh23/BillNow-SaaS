package com.example.Billing.invoice;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service @RequiredArgsConstructor
public class DocumentNoteService {
    private final DocumentNoteRepository notes;
    private final InvoiceRepository invoices;
    private final ShopRepository shops;
    private final ShopContextResolver shopContext;
    private final DocumentNumberService numbers;
    private final ProductRepository products;

    @Transactional
    public NoteResponse create(User_entity user, Long invoiceId, NoteRequest request) {
        Long shopId = shopContext.resolveActiveShopId(user);
        Shop_entity shop = shops.findByIdForUpdate(shopId).orElseThrow(() -> notFound("SHOP_NOT_FOUND", "Shop not found"));
        Invoice_entity invoice = invoices.findByIdAndShopId(invoiceId, shopId)
                .orElseThrow(() -> notFound("INVOICE_NOT_FOUND", "Invoice not found"));
        if (!"CREDIT_NOTE".equals(request.type()) && !"DEBIT_NOTE".equals(request.type())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_NOTE_TYPE", "Type must be CREDIT_NOTE or DEBIT_NOTE");
        }
        BigDecimal amount = request.amount() == null ? BigDecimal.ZERO : request.amount().setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0 || amount.compareTo(invoice.getTotalAmount()) > 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_NOTE_AMOUNT", "Note amount must be positive and not exceed the invoice total");
        }
        String prefix = "CREDIT_NOTE".equals(request.type()) ? shop.getCreditNotePrefix() : shop.getDebitNotePrefix();
        var number = numbers.next(shop, request.type(), prefix, LocalDate.now());
        if (request.restock() && "CREDIT_NOTE".equals(request.type())) {
            for (InvoiceItem item : invoice.getItems()) {
                int updated = products.increaseStock(item.getProduct().getId(), shopId, item.getQuantity());
                if (updated == 0) throw new AppException(HttpStatus.CONFLICT, "STOCK_UPDATE_FAILED", "Could not return invoice stock");
            }
        }
        DocumentNote_entity saved = notes.save(DocumentNote_entity.builder().shop(shop).originalInvoice(invoice)
                .noteNumber(number.number()).noteType(request.type()).financialYear(number.financialYear())
                .seriesNumber(number.sequence()).amount(amount).reason(request.reason()).restock(request.restock()).build());
        return map(saved);
    }

    @Transactional(readOnly = true)
    public java.util.List<NoteResponse> list(User_entity user, Long invoiceId) {
        Long shopId = shopContext.resolveActiveShopId(user);
        if (invoices.findByIdAndShopId(invoiceId, shopId).isEmpty()) throw notFound("INVOICE_NOT_FOUND", "Invoice not found");
        return notes.findByShopIdAndOriginalInvoiceIdOrderByCreatedAtDesc(shopId, invoiceId).stream().map(this::map).toList();
    }

    private NoteResponse map(DocumentNote_entity note) { return new NoteResponse(note.getId(), note.getOriginalInvoice().getId(),
            note.getNoteNumber(), note.getNoteType(), note.getAmount(), note.getReason(), note.isRestock(), note.getCreatedAt()); }
    private AppException notFound(String code, String message) { return new AppException(HttpStatus.NOT_FOUND, code, message); }
    public record NoteRequest(String type, BigDecimal amount, String reason, boolean restock) {}
    public record NoteResponse(Long id, Long invoiceId, String number, String type, BigDecimal amount, String reason, boolean restock, java.time.LocalDateTime createdAt) {}
}
