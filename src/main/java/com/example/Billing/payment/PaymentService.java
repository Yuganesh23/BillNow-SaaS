package com.example.Billing.payment;

import java.math.BigDecimal;

import com.example.Billing.auth.User_entity;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.invoice.InvoiceStatus;
import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invoice not found in this shop"
                                )
                        );


        // -------------------------------------------------
        // CHECK INVOICE STATUS
        // -------------------------------------------------

        if (invoice.getStatus()
                == InvoiceStatus.CANCELLED) {

            throw new RuntimeException(
                    "Cannot make payment for a cancelled invoice"
            );
        }


        if (invoice.getStatus()
                == InvoiceStatus.REFUNDED) {

            throw new RuntimeException(
                    "Cannot make payment for a refunded invoice"
            );
        }


        if (invoice.getStatus()
                == InvoiceStatus.PAID) {

            throw new RuntimeException(
                    "Invoice is already paid"
            );
        }


        // -------------------------------------------------
        // CHECK EXISTING PAYMENT
        // -------------------------------------------------

        if (paymentRepository
                .existsByInvoiceId(
                        invoice.getId()
                )) {

            throw new RuntimeException(
                    "Payment already exists for this invoice"
            );
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

            throw new RuntimeException(
                    "Payment amount must be greater than zero"
            );
        }


        // -------------------------------------------------
        // GET INVOICE TOTAL
        // -------------------------------------------------

        BigDecimal invoiceTotal =
                invoice.getTotalAmount();


        // -------------------------------------------------
        // CHECK PAYMENT AMOUNT
        // -------------------------------------------------

        if (amount.compareTo(
                invoiceTotal
        ) != 0) {

            throw new RuntimeException(
                    "Payment amount must match invoice total. "
                            + "Invoice total: "
                            + invoiceTotal
            );
        }


        // -------------------------------------------------
        // CREATE PAYMENT
        // -------------------------------------------------

        Payment_entity payment =
                Payment_entity.builder()
                        .invoice(invoice)
                        .paymentMethod(
                                request.getPaymentMethod()
                        )
                        .amount(amount)
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

        invoice.setStatus(
                InvoiceStatus.PAID
        );

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
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invoice not found in this shop"
                                )
                        );


        // -------------------------------------------------
        // GET PAYMENT
        // -------------------------------------------------

        Payment_entity payment =
                paymentRepository
                        .findByInvoiceId(
                                invoice.getId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Payment not found for this invoice"
                                )
                        );


        return mapToResponse(
                payment
        );
    }


    // =====================================================
    // GET USER SHOP
    // =====================================================

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