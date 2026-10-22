package com.example.Billing.portal;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.invoice.pdf.InvoicePdfService;
import com.example.Billing.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerPortalService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CustomerPortalAccessRepository accessRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final InvoicePdfService invoicePdfService;
    private final ShopContextResolver shopContextResolver;

    @Value("${customer-portal.access-hours:72}")
    private long accessHours;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public PortalLink createLink(User_entity user, Long customerId) {
        Long shopId = shopContextResolver.resolveActiveShopId(user);
        var customer = customerRepository.findByIdAndShopId(customerId, shopId)
                .orElseThrow(() -> notFound("CUSTOMER_NOT_FOUND", "Customer not found"));
        revokeActive(customerId, shopId);

        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(accessHours);
        accessRepository.save(CustomerPortalAccess.builder()
                .shop(customer.getShop()).customer(customer).tokenHash(hash(rawToken))
                .expiresAt(expiresAt).build());

        String root = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return new PortalLink(root + "/portal?token=" + rawToken, expiresAt);
    }

    @Transactional
    public void revoke(User_entity user, Long customerId) {
        Long shopId = shopContextResolver.resolveActiveShopId(user);
        if (customerRepository.findByIdAndShopId(customerId, shopId).isEmpty()) {
            throw notFound("CUSTOMER_NOT_FOUND", "Customer not found");
        }
        revokeActive(customerId, shopId);
    }

    @Transactional
    public PortalSummary summary(String rawToken) {
        CustomerPortalAccess access = validate(rawToken);
        access.setLastUsedAt(LocalDateTime.now());
        List<PortalInvoice> invoices = invoiceRepository
                .findByCustomerIdAndShopIdOrderByCreatedAtDesc(access.getCustomer().getId(), access.getShop().getId())
                .stream().map(invoice -> toPortalInvoice(invoice, access.getShop().getId())).toList();
        return new PortalSummary(access.getShop().getName(), access.getCustomer().getName(), invoices, access.getExpiresAt());
    }

    @Transactional
    public PortalPdf invoicePdf(String rawToken, Long invoiceId) {
        CustomerPortalAccess access = validate(rawToken);
        Invoice_entity invoice = invoiceRepository.findByIdAndShopId(invoiceId, access.getShop().getId())
                .filter(value -> value.getCustomer().getId().equals(access.getCustomer().getId()))
                .orElseThrow(() -> notFound("INVOICE_NOT_FOUND", "Invoice not found"));
        access.setLastUsedAt(LocalDateTime.now());
        byte[] bytes = invoicePdfService.generateInvoicePdfForPortal(
                invoiceId, access.getShop().getId(), access.getCustomer().getId());
        return new PortalPdf(invoice.getInvoiceNumber(), bytes);
    }

    private CustomerPortalAccess validate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "PORTAL_TOKEN_REQUIRED", "Portal access token is required");
        }
        CustomerPortalAccess access = accessRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "PORTAL_TOKEN_INVALID", "Portal link is invalid"));
        if (access.getRevokedAt() != null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "PORTAL_TOKEN_REVOKED", "Portal link has been revoked");
        }
        if (!access.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "PORTAL_TOKEN_EXPIRED", "Portal link has expired");
        }
        return access;
    }

    private PortalInvoice toPortalInvoice(Invoice_entity invoice, Long shopId) {
        BigDecimal paid = paymentRepository.sumByInvoiceAndShop(invoice.getId(), shopId).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = invoice.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal outstanding = total.subtract(paid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new PortalInvoice(invoice.getId(), invoice.getInvoiceNumber(), invoice.getCreatedAt(),
                invoice.getStatus().name(), total, paid, outstanding);
    }

    private void revokeActive(Long customerId, Long shopId) {
        LocalDateTime now = LocalDateTime.now();
        accessRepository.findByCustomerIdAndShopIdAndRevokedAtIsNull(customerId, shopId)
                .forEach(access -> access.setRevokedAt(now));
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "PORTAL_HASH_UNAVAILABLE", "Portal access is unavailable");
        }
    }

    private AppException notFound(String code, String message) {
        return new AppException(HttpStatus.NOT_FOUND, code, message);
    }

    public record PortalLink(String url, LocalDateTime expiresAt) {}
    public record PortalSummary(String shopName, String customerName, List<PortalInvoice> invoices,
                                LocalDateTime expiresAt) {}
    public record PortalInvoice(Long id, String invoiceNumber, LocalDateTime createdAt, String status,
                                BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal outstandingAmount) {}
    public record PortalPdf(String invoiceNumber, byte[] bytes) {}
}
