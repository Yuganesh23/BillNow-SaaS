package com.example.Billing.invoice;

import java.math.BigDecimal;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.customer.Customer_entity;
import com.example.Billing.invoice.whatsapp.WhatsAppStatus;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.product.Product_entity;
import com.example.Billing.gst.GstinValidator;
import com.example.Billing.gst.IndianAmountInWords;
import com.example.Billing.gst.TaxCalculationService;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import com.example.Billing.config.ShopContextResolver;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

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
    private final TaxCalculationService taxCalculationService;
    private final DocumentNumberService documentNumberService;
    private final com.example.Billing.subscription.PlanLimitsService planLimitsService;


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

        Long shopId = shopContextResolver.resolveActiveShopId(user);
        Shop_entity shop = shopRepository.findByIdForUpdate(shopId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND, "SHOP_NOT_FOUND", "Shop not found"));

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existingInvoice = invoiceRepository.findByShopIdAndIdempotencyKey(shopId, idempotencyKey.trim());
            if (existingInvoice.isPresent()) return mapToResponse(existingInvoice.get());
        }

        planLimitsService.assertCanCreateInvoice(shop);



        // -------------------------------------------------
        // GET CUSTOMER
        // -------------------------------------------------

        Customer_entity customer =
                customerRepository
                        .findByIdAndShopId(
                                request.getCustomerId(),
                                shopId
                        )
                        .orElseThrow(() -> new AppException(
                                HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));


        // -------------------------------------------------
        // DISCOUNT
        // -------------------------------------------------

        BigDecimal discount =
                request.getDiscountAmount();

        if (discount == null) {
            discount = BigDecimal.ZERO;
        }

        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_DISCOUNT", "Discount cannot be negative");
        }


        // -------------------------------------------------
        // VALIDATE ITEMS
        // -------------------------------------------------

        if (request.getItems() == null ||
                request.getItems().isEmpty()) {

            throw new AppException(HttpStatus.BAD_REQUEST, "DOCUMENT_ITEMS_REQUIRED", "Invoice must contain at least one item");
        }

        DocumentNumberService.NumberedDocument documentNumber = documentNumberService.next(
                shop, "TAX_INVOICE", shop.getInvoicePrefix(), LocalDate.now());


        // -------------------------------------------------
        // CREATE INVOICE
        // -------------------------------------------------

        Invoice_entity invoice =
                Invoice_entity.builder()

                        .shop(shop)

                        .customer(customer)

                        .biller(user)

                        .invoiceNumber(documentNumber.number())
                        .financialYear(documentNumber.financialYear())
                        .seriesNumber(documentNumber.sequence())

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


        List<Product_entity> products = new ArrayList<>();
        List<TaxCalculationService.LineInput> taxLines = new ArrayList<>();
        for (InvoiceItemRequest_Dto itemRequest : request.getItems()) {
            if (itemRequest == null || itemRequest.getProductId() == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, "PRODUCT_REQUIRED", "Product ID is required");
            }
            Double requestedQuantity = itemRequest.getQuantity();
            if (requestedQuantity == null || requestedQuantity <= 0.0) {
                throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY", "Quantity must be greater than zero");
            }

            Product_entity product = productRepository.findByIdAndShopId(itemRequest.getProductId(), shopId)
                    .orElseThrow(() -> new AppException(
                            HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found"));

            if (!product.isActive()) {
                throw new AppException(HttpStatus.CONFLICT, "PRODUCT_INACTIVE", "Product is inactive: " + product.getName());
            }
            products.add(product);
            taxLines.add(new TaxCalculationService.LineInput(String.valueOf(product.getId()),
                    BigDecimal.valueOf(requestedQuantity), product.getSellingPrice(), product.getTaxType(),
                    product.getGstRate(), product.getCessRate(), itemRequest.getLineDiscount()));
        }

        String destinationState = customer.getGstin() != null && GstinValidator.isValid(customer.getGstin())
                ? GstinValidator.stateCode(customer.getGstin()) : customer.getStateCode();
        TaxCalculationService.Calculation calculation = taxCalculationService.calculate(
                new TaxCalculationService.Request(shop.getStateCode(), destinationState,
                        shop.getRegistrationType(), discount, taxLines));

        for (int index = 0; index < products.size(); index++) {
            Product_entity product = products.get(index);
            InvoiceItemRequest_Dto itemRequest = request.getItems().get(index);
            TaxCalculationService.LineResult result = calculation.lines().get(index);
            BigDecimal quantity = BigDecimal.valueOf(itemRequest.getQuantity());
            invoice.getItems().add(InvoiceItem.builder().invoice(invoice).product(product)
                    .quantity(itemRequest.getQuantity()).unitPrice(product.getSellingPrice())
                    .totalPrice(result.total()).taxableAmount(result.taxableAmount())
                    .cgst(result.cgst()).sgst(result.sgst()).igst(result.igst()).cess(result.cess())
                    .lineDiscount(itemRequest.getLineDiscount() == null ? BigDecimal.ZERO : itemRequest.getLineDiscount())
                    .gstRate(product.getGstRate() == null ? BigDecimal.ZERO : product.getGstRate()).build());
            int updated = productRepository.reduceStock(product.getId(), shopId, quantity.doubleValue());
            if (updated == 0) throw new AppException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                    "Insufficient stock or concurrent update for product: " + product.getName());
        }

        invoice.setSubtotal(calculation.taxableAmount());
        invoice.setTaxableAmount(calculation.taxableAmount());
        invoice.setCgstTotal(calculation.cgst());
        invoice.setSgstTotal(calculation.sgst());
        invoice.setIgstTotal(calculation.igst());
        invoice.setCessTotal(calculation.cess());
        invoice.setRoundOff(calculation.roundOff());
        invoice.setTotalAmount(calculation.total());
        invoice.setIsInterState(calculation.interState());
        invoice.setPlaceOfSupplyCode(calculation.placeOfSupplyCode());
        invoice.setDocumentType(calculation.billOfSupply() ? "BILL_OF_SUPPLY" : "TAX_INVOICE");


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
                        .orElseThrow(this::invoiceNotFound);


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
                        .orElseThrow(this::invoiceNotFound);

        if (invoice.isComplianceLocked()) {
            throw new AppException(HttpStatus.CONFLICT, "INVOICE_COMPLIANCE_LOCKED",
                    "This invoice has an IRN and cannot be changed through the normal invoice cancellation flow");
        }


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

    // =====================================================
    // GET USER SHOP
    // =====================================================

    private AppException invoiceNotFound() {
        return new AppException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found");
    }

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
                                        .cess(item.getCess())
                                        .lineDiscount(item.getLineDiscount())
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
                .cessTotal(invoice.getCessTotal())
                .roundOff(invoice.getRoundOff())
                .placeOfSupplyCode(invoice.getPlaceOfSupplyCode())
                .documentType(invoice.getDocumentType())
                .amountInWords(IndianAmountInWords.rupees(invoice.getTotalAmount()))
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
