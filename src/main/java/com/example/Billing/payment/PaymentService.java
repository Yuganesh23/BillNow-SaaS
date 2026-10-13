package com.example.Billing.payment;

import java.math.BigDecimal;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.invoice.InvoiceStatus;
import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import com.example.Billing.config.ShopContextResolver;
import org.springframework.transaction.annotation.Transactional;



@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ShopContextResolver shopContextResolver;
    private final InvoiceRepository invoiceRepository;


    // =====================================================
    // CREATE PAYMENT
    // =====================================================

    public PaymentResponse_Dto createPayment(
            User_entity user,
            PaymentRequest_Dto request
    ) {

        // -------------------------------------------------
        // GET SHOP
        // -------------------------------------------------

        Shop_entity shop =
                getUserShop(user);

        Long shopId =
                shop.getId();


        // -------------------------------------------------
        // GET INVOICE
        // -------------------------------------------------

        Invoice_entity invoice =
                invoiceRepository
                        .findByIdAndShopId(
                                request.getInvoiceId(),
                                shopId
                        )
                        .orElseThrow(this::invoiceNotFound);


        // -------------------------------------------------
        // CHECK INVOICE STATUS
        // -------------------------------------------------

        if (invoice.getStatus()
                == InvoiceStatus.CANCELLED) {

            throw new AppException(HttpStatus.CONFLICT, "INVOICE_CANCELLED", "Cannot make payment for a cancelled invoice");
        }


        if (invoice.getStatus()
                == InvoiceStatus.REFUNDED) {

            throw new AppException(HttpStatus.CONFLICT, "INVOICE_REFUNDED", "Cannot make payment for a refunded invoice");
        }


        if (invoice.getStatus()
                == InvoiceStatus.PAID) {

            throw new AppException(HttpStatus.CONFLICT, "INVOICE_ALREADY_PAID", "Invoice is already paid");
        }


        // -------------------------------------------------
        // VALIDATE PAYMENT AMOUNT
        // -------------------------------------------------

        BigDecimal amount =
                request.getAmount();

        if (amount == null ||
                amount.compareTo(
                        BigDecimal.ZERO
                ) <= 0) {

            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_AMOUNT", "Payment amount must be greater than zero");
        }


        BigDecimal alreadyPaid = paymentRepository.sumByInvoiceAndShop(invoice.getId(), shopId);
        BigDecimal outstanding = invoice.getTotalAmount().subtract(alreadyPaid);
        if (outstanding.signum() <= 0) throw new AppException(HttpStatus.CONFLICT, "INVOICE_ALREADY_PAID", "Invoice is already paid");
        BigDecimal applied = amount.min(outstanding);
        BigDecimal credit = amount.subtract(applied);


        // -------------------------------------------------
        // CREATE PAYMENT
        // -------------------------------------------------

        Payment_entity payment =
                Payment_entity.builder()
                        .invoice(invoice)
                        .paymentMethod(
                                request.getPaymentMethod()
                        )
                        .amount(applied)
                        .build();


        // -------------------------------------------------
        // SAVE PAYMENT
        // -------------------------------------------------

        Payment_entity savedPayment =
                paymentRepository.save(
                        payment
                );


        // =================================================
        // IMPORTANT:
        // MARK INVOICE AS PAID
        // =================================================

        BigDecimal paidAfter = alreadyPaid.add(applied);
        invoice.setStatus(paidAfter.compareTo(invoice.getTotalAmount()) >= 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIAL);

        if (credit.signum() > 0) {
            var customer = invoice.getCustomer();
            customer.setCreditBalance(customer.getCreditBalance().add(credit));
        }

        invoiceRepository.save(
                invoice
        );


        // -------------------------------------------------
        // RETURN PAYMENT RESPONSE
        // -------------------------------------------------

        return mapToResponse(
                savedPayment
        );
    }


    // =====================================================
    // GET PAYMENT BY INVOICE
    // =====================================================

    @Transactional(readOnly = true)
    public PaymentResponse_Dto getPaymentByInvoice(
            User_entity user,
            Long invoiceId
    ) {

        // -------------------------------------------------
        // GET SHOP
        // -------------------------------------------------

        Shop_entity shop =
                getUserShop(user);

        Long shopId =
                shop.getId();


        // -------------------------------------------------
        // VERIFY INVOICE
        // -------------------------------------------------

        Invoice_entity invoice =
                invoiceRepository
                        .findByIdAndShopId(
                                invoiceId,
                                shopId
                        )
                        .orElseThrow(this::invoiceNotFound);


        // -------------------------------------------------
        // GET PAYMENT
        // -------------------------------------------------

        Payment_entity payment =
                paymentRepository
                        .findFirstByInvoiceIdOrderByPaidAtDesc(invoice.getId())
                        .orElseThrow(() -> new AppException(
                                HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found"));


        return mapToResponse(
                payment
        );
    }


    // =====================================================
    // GET USER SHOP
    // =====================================================

    private AppException invoiceNotFound() {
        return new AppException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found");
    }

    private Shop_entity getUserShop(
            User_entity user
    ) {

        // -------------------------------------------------
        // CHECK USER
        // -------------------------------------------------

        if (user == null) {

            throw new RuntimeException(
                    "User not authenticated"
            );
        }


        // -------------------------------------------------
        // CHECK SHOP
        // -------------------------------------------------

        if (shopContextResolver.resolveActiveShop(user) == null) {

            throw new RuntimeException(
                    "User is not assigned to a shop"
            );
        }


        // -------------------------------------------------
        // CHECK SHOP ID
        // -------------------------------------------------

        if (shopContextResolver.resolveActiveShop(user).getId() == null) {

            throw new RuntimeException(
                    "Shop ID is missing"
            );
        }


        return shopContextResolver.resolveActiveShop(user);
    }


    // =====================================================
    // MAP RESPONSE
    // =====================================================

    private PaymentResponse_Dto mapToResponse(
            Payment_entity payment
    ) {

        Invoice_entity invoice =
                payment.getInvoice();


        return PaymentResponse_Dto.builder()

                .id(
                        payment.getId()
                )

                .invoiceId(
                        invoice.getId()
                )

                .invoiceNumber(
                        invoice.getInvoiceNumber()
                )

                .customerId(
                        invoice.getCustomer()
                                .getId()
                )

                .customerName(
                        invoice.getCustomer()
                                .getName()
                )

                .paymentMethod(
                        payment.getPaymentMethod()
                )

                .amount(
                        payment.getAmount()
                )

                .paidAt(
                        payment.getPaidAt()
                )

                .build();
    }
}
