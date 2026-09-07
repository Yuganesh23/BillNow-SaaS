package com.example.Billing.invoice;

import com.example.Billing.auth.User_entity;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.customer.Customer_entity;
import com.example.Billing.invoice.whatsapp.WhatsAppStatus;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.product.Product_entity;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;


    // =====================================================
    // CREATE INVOICE
    // =====================================================

    public InvoiceResponse_Dto createInvoice(
            User_entity user,
            CreateInvoiceRequest_Dto request
    ) {

        // -------------------------------------------------
        // GET SHOP
        // -------------------------------------------------

        Shop_entity shop =
                getUserShop(user);

        Long shopId =
                shop.getId();


        // -------------------------------------------------
        // GET CUSTOMER
        // -------------------------------------------------

        Customer_entity customer =
                customerRepository
                        .findByIdAndShopId(
                                request.getCustomerId(),
                                shopId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found in this shop"
                                )
                        );


        // -------------------------------------------------
        // DISCOUNT
        // -------------------------------------------------

        BigDecimal discount =
                request.getDiscountAmount();

        if (discount == null) {
            discount = BigDecimal.ZERO;
        }

        if (discount.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new RuntimeException(
                    "Discount cannot be negative"
            );
        }


        // -------------------------------------------------
        // VALIDATE ITEMS
        // -------------------------------------------------

        if (request.getItems() == null ||
                request.getItems().isEmpty()) {

            throw new RuntimeException(
                    "Invoice must contain at least one item"
            );
        }


        // -------------------------------------------------
        // CREATE INVOICE
        // -------------------------------------------------

        Invoice_entity invoice =
                Invoice_entity.builder()

                        .shop(shop)

                        .customer(customer)

                        .biller(user)

                        .invoiceNumber(
                                generateInvoiceNumber(
                                        shopId
                                )
                        )

                        .subtotal(
                                BigDecimal.ZERO
                        )

                        .discountAmount(
                                discount
                        )

                        .totalAmount(
                                BigDecimal.ZERO
                        )

                        .status(
                                InvoiceStatus.PENDING
                        )

                        .whatsappStatus(
                                WhatsAppStatus.NOT_SENT
                        )

                        .items(
                                new ArrayList<>()
                        )

                        .build();


        BigDecimal subtotal =
                BigDecimal.ZERO;


        // =================================================
        // PROCESS ITEMS
        // =================================================

        for (InvoiceItemRequest_Dto itemRequest :
                request.getItems()) {


            // ---------------------------------------------
            // VALIDATE ITEM
            // ---------------------------------------------

            if (itemRequest == null) {

                throw new RuntimeException(
                        "Invoice item cannot be null"
                );
            }


            if (itemRequest.getProductId() == null) {

                throw new RuntimeException(
                        "Product ID is required"
                );
            }


            Integer requestedQuantity =
                    itemRequest.getQuantity();


            if (requestedQuantity == null ||
                    requestedQuantity <= 0) {

                throw new RuntimeException(
                        "Quantity must be greater than zero"
                );
            }


            // ---------------------------------------------
            // GET PRODUCT
            // ---------------------------------------------

            Product_entity product =
                    productRepository
                            .findByIdAndShopId(
                                    itemRequest.getProductId(),
                                    shopId
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Product not found: "
                                                    + itemRequest
                                                    .getProductId()
                                    )
                            );


            // ---------------------------------------------
            // CHECK ACTIVE
            // ---------------------------------------------

            if (!product.isActive()) {

                throw new RuntimeException(
                        "Product is inactive: "
                                + product.getName()
                );
            }


            // ---------------------------------------------
            // CHECK STOCK
            // ---------------------------------------------

            Integer availableStock =
                    product.getStockQuantity();


            if (availableStock == null) {

                throw new RuntimeException(
                        "Stock quantity is missing for product: "
                                + product.getName()
                );
            }


            if (availableStock <
                    requestedQuantity) {

                throw new RuntimeException(
                        "Insufficient stock for product: "
                                + product.getName()
                                + ". Available: "
                                + availableStock
                );
            }


            // ---------------------------------------------
            // UNIT PRICE
            // ---------------------------------------------

            BigDecimal unitPrice =
                    product.getSellingPrice();


            if (unitPrice == null) {

                throw new RuntimeException(
                        "Selling price is missing for product: "
                                + product.getName()
                );
            }


            // ---------------------------------------------
            // TOTAL PRICE
            // ---------------------------------------------

            BigDecimal totalPrice =
                    unitPrice.multiply(
                            BigDecimal.valueOf(
                                    requestedQuantity
                            )
                    );


            // ---------------------------------------------
            // CREATE INVOICE ITEM
            // ---------------------------------------------

            InvoiceItem invoiceItem =
                    InvoiceItem.builder()

                            .invoice(invoice)

                            .product(product)

                            .quantity(
                                    requestedQuantity
                            )

                            .unitPrice(
                                    unitPrice
                            )

                            .totalPrice(
                                    totalPrice
                            )

                            .build();


            invoice.getItems()
                    .add(invoiceItem);


            // ---------------------------------------------
            // UPDATE SUBTOTAL
            // ---------------------------------------------

            subtotal =
                    subtotal.add(
                            totalPrice
                    );


            // ---------------------------------------------
            // REDUCE STOCK
            // ---------------------------------------------

            product.setStockQuantity(
                    availableStock
                            - requestedQuantity
            );

            productRepository.save(
                    product
            );
        }


        // =================================================
        // VALIDATE DISCOUNT
        // =================================================

        if (discount.compareTo(
                subtotal
        ) > 0) {

            throw new RuntimeException(
                    "Discount cannot be greater than subtotal"
            );
        }


        // =================================================
        // CALCULATE TOTAL
        // =================================================

        BigDecimal totalAmount =
                subtotal.subtract(
                        discount
                );


        invoice.setSubtotal(
                subtotal
        );

        invoice.setTotalAmount(
                totalAmount
        );


        // =================================================
        // SAVE INVOICE
        // =================================================

        Invoice_entity savedInvoice =
                invoiceRepository.save(
                        invoice
                );


        return mapToResponse(
                savedInvoice
        );
    }


    // =====================================================
    // GET ALL INVOICES
    // =====================================================

    @Transactional(readOnly = true)
    public List<InvoiceResponse_Dto> getAllInvoices(
            User_entity user
    ) {

        Shop_entity shop =
                getUserShop(user);

        Long shopId =
                shop.getId();


        return invoiceRepository
                .findByShopIdOrderByCreatedAtDesc(
                        shopId
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =====================================================
    // GET INVOICE BY ID
    // =====================================================

    @Transactional(readOnly = true)
    public InvoiceResponse_Dto getInvoice(
            User_entity user,
            Long invoiceId
    ) {

        Shop_entity shop =
                getUserShop(user);

        Long shopId =
                shop.getId();


        Invoice_entity invoice =
                invoiceRepository
                        .findByIdAndShopId(
                                invoiceId,
                                shopId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invoice not found"
                                )
                        );


        return mapToResponse(
                invoice
        );
    }


    // =====================================================
    // CANCEL INVOICE
    // =====================================================

    public InvoiceResponse_Dto cancelInvoice(
            User_entity user,
            Long invoiceId
    ) {

        Shop_entity shop =
                getUserShop(user);

        Long shopId =
                shop.getId();


        Invoice_entity invoice =
                invoiceRepository
                        .findByIdAndShopId(
                                invoiceId,
                                shopId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invoice not found"
                                )
                        );


        // -------------------------------------------------
        // CHECK STATUS
        // -------------------------------------------------

        if (invoice.getStatus()
                == InvoiceStatus.CANCELLED) {

            throw new RuntimeException(
                    "Invoice is already cancelled"
            );
        }


        // -------------------------------------------------
        // RESTORE STOCK
        // -------------------------------------------------

        for (InvoiceItem item :
                invoice.getItems()) {

            Product_entity product =
                    item.getProduct();


            Integer currentStock =
                    product.getStockQuantity();


            if (currentStock == null) {

                currentStock =
                        0;
            }


            product.setStockQuantity(
                    currentStock
                            + item.getQuantity()
            );


            productRepository.save(
                    product
            );
        }


        // -------------------------------------------------
        // UPDATE STATUS
        // -------------------------------------------------

        invoice.setStatus(
                InvoiceStatus.CANCELLED
        );


        return mapToResponse(
                invoiceRepository.save(
                        invoice
                )
        );
    }


    // =====================================================
    // GENERATE INVOICE NUMBER
    // =====================================================

    private String generateInvoiceNumber(
            Long shopId
    ) {

        long count =
                invoiceRepository
                        .findByShopIdOrderByCreatedAtDesc(
                                shopId
                        )
                        .size();


        return String.format(
                "INV-%05d",
                count + 1
        );
    }


    // =====================================================
    // GET USER SHOP
    // =====================================================

    private Shop_entity getUserShop(
            User_entity user
    ) {

        if (user == null) {

            throw new RuntimeException(
                    "User not authenticated"
            );
        }


        if (user.getShop() == null) {

            throw new RuntimeException(
                    "User is not assigned to a shop"
            );
        }


        if (user.getShop().getId() == null) {

            throw new RuntimeException(
                    "Shop ID is missing"
            );
        }


        if (!user.getShop().isActive()) {

            throw new RuntimeException(
                    "Shop is not active"
            );
        }


        return user.getShop();
    }


    // =====================================================
    // MAPPER
    // =====================================================

    private InvoiceResponse_Dto mapToResponse(
            Invoice_entity invoice
    ) {

        List<InvoiceItemResponse_Dto> items =
                invoice.getItems()
                        .stream()
                        .map(item ->

                                InvoiceItemResponse_Dto
                                        .builder()

                                        .id(
                                                item.getId()
                                        )

                                        .productId(
                                                item.getProduct()
                                                        .getId()
                                        )

                                        .productName(
                                                item.getProduct()
                                                        .getName()
                                        )

                                        .sku(
                                                item.getProduct()
                                                        .getSku()
                                        )

                                        .quantity(
                                                item.getQuantity()
                                        )

                                        .unitPrice(
                                                item.getUnitPrice()
                                        )

                                        .totalPrice(
                                                item.getTotalPrice()
                                        )

                                        .build()

                        )
                        .toList();


        return InvoiceResponse_Dto.builder()

                .id(
                        invoice.getId()
                )

                .shopId(
                        invoice.getShop()
                                .getId()
                )

                .customerId(
                        invoice.getCustomer()
                                .getId()
                )

                .customerName(
                        invoice.getCustomer()
                                .getName()
                )

                .invoiceNumber(
                        invoice.getInvoiceNumber()
                )

                .subtotal(
                        invoice.getSubtotal()
                )

                .discountAmount(
                        invoice.getDiscountAmount()
                )

                .totalAmount(
                        invoice.getTotalAmount()
                )

                .status(
                        invoice.getStatus()
                )

                .createdAt(
                        invoice.getCreatedAt()
                )

                .items(items)

                .billerId(
                        invoice.getBiller()
                                .getId()
                )

                .billerName(
                        invoice.getBiller()
                                .getName()
                )

                // -----------------------------------------
                // WHATSAPP STATUS
                // -----------------------------------------

                .whatsappStatus(
                        invoice.getWhatsappStatus()
                )

                .whatsappSentAt(
                        invoice.getWhatsappSentAt()
                )

                .whatsappError(
                        invoice.getWhatsappError()
                )

                .build();
    }
}