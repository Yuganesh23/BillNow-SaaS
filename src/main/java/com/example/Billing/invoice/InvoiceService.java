package com.example.Billing.invoice;

import java.math.BigDecimal;

import com.example.Billing.auth.User_entity;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.customer.Customer_entity;
import com.example.Billing.invoice.whatsapp.WhatsAppStatus;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.product.Product_entity;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.Billing.config.ShopContextResolver;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private final com.example.Billing.shop.ShopRepository shopRepository;

    private final com.example.Billing.notification.EmailService emailService;
    private final com.example.Billing.invoice.whatsapp.WhatsAppService whatsAppService;


    private final InvoiceRepository invoiceRepository;
    private final ShopContextResolver shopContextResolver;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;


    // =====================================================
    // CREATE INVOICE
    // =====================================================

    public InvoiceResponse_Dto createInvoice(
            User_entity user,
            CreateInvoiceRequest_Dto request,
            String idempotencyKey
    ) {

        // -------------------------------------------------
        // GET SHOP
        // -------------------------------------------------

        Shop_entity shop =
                getUserShop(user);

        Long shopId =
                shop.getId();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existingInvoice = invoiceRepository.findByShopIdAndIdempotencyKey(shopId, idempotencyKey.trim());
            if (existingInvoice.isPresent()) return mapToResponse(existingInvoice.get());
        }

        // -------------------------------------------------
        // SUBSCRIPTION CHECK
        // -------------------------------------------------
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        com.example.Billing.shop.SubscriptionTier tier = shop.getSubscriptionTier();
        
        if (tier == null) {
            tier = com.example.Billing.shop.SubscriptionTier.TRIAL;
            shop.setSubscriptionTier(tier);
            shop.setTrialEndsAt(now.plusDays(7));
            // We won't save shop here to avoid circular dep, it's just a fallback in memory 
            // since we added default values it should be TRIAL.
        }
        
        if (tier == com.example.Billing.shop.SubscriptionTier.TRIAL) {
            if (shop.getTrialEndsAt() != null && now.isAfter(shop.getTrialEndsAt())) {
                throw new RuntimeException("Your 7-day trial has expired. Please upgrade to a paid plan (BASE, PRO, or ENTERPRISE) to continue creating invoices.");
            }
        } else {
            if (shop.getSubscriptionEndsAt() != null && now.isAfter(shop.getSubscriptionEndsAt())) {
                throw new RuntimeException("Your subscription has expired. Please renew your plan to continue.");
            }
        }



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

                        .invoiceNumber(generateInvoiceNumber(shopId))

                        .idempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim())

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

        BigDecimal totalTaxableAmount = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        for (InvoiceItemRequest_Dto itemRequest : request.getItems()) {
            if (itemRequest == null || itemRequest.getProductId() == null) {
                throw new RuntimeException("Product ID is required");
            }
            Double requestedQuantity = itemRequest.getQuantity();
            if (requestedQuantity == null || requestedQuantity <= 0.0) {
                throw new RuntimeException("Quantity must be greater than zero");
            }

            Product_entity product = productRepository.findByIdAndShopId(itemRequest.getProductId(), shopId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (!product.isActive()) {
                throw new RuntimeException("Product is inactive: " + product.getName());
            }

            BigDecimal unitPrice = product.getSellingPrice();
            BigDecimal enteredLineTotal = unitPrice.multiply(BigDecimal.valueOf(requestedQuantity));
            
            BigDecimal itemBaseAmount = enteredLineTotal;
            BigDecimal itemTaxAmount = BigDecimal.ZERO;

            if (product.getGstRate() != null && product.getGstRate().compareTo(BigDecimal.ZERO) > 0) {
                if (product.getTaxType() == com.example.Billing.product.TaxType.INCLUSIVE) {
                    BigDecimal divisor = BigDecimal.ONE.add(product.getGstRate().divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));
                    itemBaseAmount = enteredLineTotal.divide(divisor, 2, java.math.RoundingMode.HALF_UP);
                    itemTaxAmount = enteredLineTotal.subtract(itemBaseAmount);
                } else if (product.getTaxType() == com.example.Billing.product.TaxType.EXCLUSIVE) {
                    itemTaxAmount = enteredLineTotal.multiply(product.getGstRate()).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                    itemBaseAmount = enteredLineTotal;
                }
            }

            BigDecimal itemCgst = BigDecimal.ZERO;
            BigDecimal itemSgst = BigDecimal.ZERO;
            BigDecimal itemIgst = BigDecimal.ZERO;

            if (request.getIsInterState() != null && request.getIsInterState()) {
                itemIgst = itemTaxAmount;
            } else {
                itemCgst = itemTaxAmount.divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
                itemSgst = itemTaxAmount.subtract(itemCgst);
            }

            totalTaxableAmount = totalTaxableAmount.add(itemBaseAmount);
            totalCgst = totalCgst.add(itemCgst);
            totalSgst = totalSgst.add(itemSgst);
            totalIgst = totalIgst.add(itemIgst);

            InvoiceItem invoiceItem = InvoiceItem.builder()
                    .invoice(invoice)
                    .product(product)
                    .quantity(requestedQuantity)
                    .unitPrice(unitPrice)
                    .totalPrice(enteredLineTotal)
                    .taxableAmount(itemBaseAmount)
                    .cgst(itemCgst)
                    .sgst(itemSgst)
                    .igst(itemIgst)
                    .gstRate(product.getGstRate() != null ? product.getGstRate() : BigDecimal.ZERO)
                    .build();

            invoice.getItems().add(invoiceItem);
            subtotal = subtotal.add(itemBaseAmount).add(itemTaxAmount);

            int updated = productRepository.reduceStock(product.getId(), shopId, requestedQuantity);
            if (updated == 0) {
                throw new RuntimeException("Insufficient stock or concurrency conflict for product: " + product.getName());
            }
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


        invoice.setSubtotal(totalTaxableAmount);
        invoice.setTaxableAmount(totalTaxableAmount);
        invoice.setCgstTotal(totalCgst);
        invoice.setSgstTotal(totalSgst);
        invoice.setIgstTotal(totalIgst);
        invoice.setTotalAmount(totalTaxableAmount.add(totalCgst).add(totalSgst).add(totalIgst).subtract(discount));
        invoice.setIsInterState(request.getIsInterState() != null ? request.getIsInterState() : false);


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

        // -------------------------------------------------
        // SUBSCRIPTION CHECK
        // -------------------------------------------------
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        com.example.Billing.shop.SubscriptionTier tier = shop.getSubscriptionTier();
        
        if (tier == null) {
            tier = com.example.Billing.shop.SubscriptionTier.TRIAL;
            shop.setSubscriptionTier(tier);
            shop.setTrialEndsAt(now.plusDays(7));
            // We won't save shop here to avoid circular dep, it's just a fallback in memory 
            // since we added default values it should be TRIAL.
        }
        
        if (tier == com.example.Billing.shop.SubscriptionTier.TRIAL) {
            if (shop.getTrialEndsAt() != null && now.isAfter(shop.getTrialEndsAt())) {
                throw new RuntimeException("Your 7-day trial has expired. Please upgrade to a paid plan (BASE, PRO, or ENTERPRISE) to continue creating invoices.");
            }
        } else {
            if (shop.getSubscriptionEndsAt() != null && now.isAfter(shop.getSubscriptionEndsAt())) {
                throw new RuntimeException("Your subscription has expired. Please renew your plan to continue.");
            }
        }



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

        // -------------------------------------------------
        // SUBSCRIPTION CHECK
        // -------------------------------------------------
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        com.example.Billing.shop.SubscriptionTier tier = shop.getSubscriptionTier();
        
        if (tier == null) {
            tier = com.example.Billing.shop.SubscriptionTier.TRIAL;
            shop.setSubscriptionTier(tier);
            shop.setTrialEndsAt(now.plusDays(7));
            // We won't save shop here to avoid circular dep, it's just a fallback in memory 
            // since we added default values it should be TRIAL.
        }
        
        if (tier == com.example.Billing.shop.SubscriptionTier.TRIAL) {
            if (shop.getTrialEndsAt() != null && now.isAfter(shop.getTrialEndsAt())) {
                throw new RuntimeException("Your 7-day trial has expired. Please upgrade to a paid plan (BASE, PRO, or ENTERPRISE) to continue creating invoices.");
            }
        } else {
            if (shop.getSubscriptionEndsAt() != null && now.isAfter(shop.getSubscriptionEndsAt())) {
                throw new RuntimeException("Your subscription has expired. Please renew your plan to continue.");
            }
        }



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

        // -------------------------------------------------
        // SUBSCRIPTION CHECK
        // -------------------------------------------------
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        com.example.Billing.shop.SubscriptionTier tier = shop.getSubscriptionTier();
        
        if (tier == null) {
            tier = com.example.Billing.shop.SubscriptionTier.TRIAL;
            shop.setSubscriptionTier(tier);
            shop.setTrialEndsAt(now.plusDays(7));
            // We won't save shop here to avoid circular dep, it's just a fallback in memory 
            // since we added default values it should be TRIAL.
        }
        
        if (tier == com.example.Billing.shop.SubscriptionTier.TRIAL) {
            if (shop.getTrialEndsAt() != null && now.isAfter(shop.getTrialEndsAt())) {
                throw new RuntimeException("Your 7-day trial has expired. Please upgrade to a paid plan (BASE, PRO, or ENTERPRISE) to continue creating invoices.");
            }
        } else {
            if (shop.getSubscriptionEndsAt() != null && now.isAfter(shop.getSubscriptionEndsAt())) {
                throw new RuntimeException("Your subscription has expired. Please renew your plan to continue.");
            }
        }



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


            Double currentStock =
                    product.getStockQuantity();


            if (currentStock == null) {

                currentStock = 0.0;
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
        Shop_entity lockedShop = shopRepository.findByIdForUpdate(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));
        int next = lockedShop.getLastInvoiceNumber() + 1;
        lockedShop.setLastInvoiceNumber(next);
        shopRepository.save(lockedShop);
        return String.format("INV-%05d", next);
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


        if (shopContextResolver.resolveActiveShop(user) == null) {

            throw new RuntimeException(
                    "User is not assigned to a shop"
            );
        }


        if (shopContextResolver.resolveActiveShop(user).getId() == null) {

            throw new RuntimeException(
                    "Shop ID is missing"
            );
        }


        if (!shopContextResolver.resolveActiveShop(user).isActive()) {

            throw new RuntimeException(
                    "Shop is not active"
            );
        }


        return shopContextResolver.resolveActiveShop(user);
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

                                        .totalPrice(item.getTotalPrice())
                                        .hsnCode(item.getProduct() != null ? item.getProduct().getHsnCode() : null)
                                        .taxableAmount(item.getTaxableAmount())
                                        .cgst(item.getCgst())
                                        .sgst(item.getSgst())
                                        .igst(item.getIgst())
                                        .gstRate(item.getGstRate())

                                        .build()

                        )
                        .toList();


        return InvoiceResponse_Dto.builder()
                .id(invoice.getId())
                .shopId(invoice.getShop().getId())
                .customerId(invoice.getCustomer().getId())
                .customerName(invoice.getCustomer().getName())
                .customerWhatsapp(invoice.getCustomer().getWhatsappNumber())
                .invoiceNumber(invoice.getInvoiceNumber())
                .subtotal(invoice.getSubtotal())
                .discountAmount(invoice.getDiscountAmount())
                .totalAmount(invoice.getTotalAmount())
                .taxableAmount(invoice.getTaxableAmount())
                .cgstTotal(invoice.getCgstTotal())
                .sgstTotal(invoice.getSgstTotal())
                .igstTotal(invoice.getIgstTotal())
                .isInterState(invoice.getIsInterState())
                .status(invoice.getStatus())
                .createdAt(invoice.getCreatedAt())
                .items(items)
                .billerId(invoice.getBiller() != null ? invoice.getBiller().getId() : null)
                .billerName(invoice.getBiller() != null ? invoice.getBiller().getName() : null)
                .whatsappStatus(invoice.getWhatsappStatus())
                .whatsappSentAt(invoice.getWhatsappSentAt())
                .whatsappError(invoice.getWhatsappError())
                .build();
    }
}
