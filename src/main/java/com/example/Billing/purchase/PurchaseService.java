package com.example.Billing.purchase;

import com.example.Billing.auth.User_entity;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.common.AppException;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.product.Product_entity;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.supplier.SupplierLedgerRepository;
import com.example.Billing.supplier.SupplierLedger_entity;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.supplier.Supplier_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import com.example.Billing.gst.GstinValidator;
import com.example.Billing.gst.RegistrationType;
import com.example.Billing.gst.TaxCalculationService;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierLedgerRepository ledgerRepository;
    private final ShopContextResolver shopContextResolver;
    private final TaxCalculationService taxCalculationService;

    @Transactional(readOnly = true)
    public List<PurchaseResponse_Dto> getAllPurchases(User_entity user) {
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        return purchaseRepository.findByShopIdOrderByCreatedAtDesc(shop.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseResponse_Dto getPurchaseById(User_entity user, Long id) {
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        Purchase_entity purchase = purchaseRepository.findByIdAndShopId(id, shop.getId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "PURCHASE_NOT_FOUND", "Purchase not found"));
                
        PurchaseResponse_Dto response = mapToResponse(purchase);
        
        List<com.example.Billing.supplier.SupplierLedger_entity> ledgers = ledgerRepository.findByPurchaseIdOrderByTransactionDateDesc(id);
        List<PurchasePaymentHistoryDto> payments = ledgers.stream()
            .filter(l -> "PAYMENT".equals(l.getType()) || "ALLOCATION".equals(l.getType()))
            .map(l -> PurchasePaymentHistoryDto.builder()
                .date(l.getTransactionDate())
                .amount(l.getCreditAmount())
                .type(l.getType())
                .notes(l.getNotes())
                .build())
            .collect(java.util.stream.Collectors.toList());
            
        response.setPayments(payments);
        return response;
    }

    @Transactional
    public PurchaseResponse_Dto createPurchase(User_entity user, PurchaseRequest_Dto request) {
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        
        Supplier_entity supplier = supplierRepository.findByIdAndShopId(request.getSupplierId(), shop.getId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SUPPLIER_NOT_FOUND", "Supplier not found"));
                
        String purchaseNumber = String.format("PUR-%04d", purchaseRepository.countByShopId(shop.getId()) + 1);
        
        Purchase_entity purchase = Purchase_entity.builder()
                .shop(shop)
                .supplier(supplier)
                .purchaseNumber(purchaseNumber)
                .createdBy(user)
                .status(request.getPaymentStatus() != null ? request.getPaymentStatus() : "PENDING")
                .paymentMethod(request.getPaymentMethod())
                .discountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO)
                .build();
                
        purchase = purchaseRepository.save(purchase);
        
        List<Product_entity> products = new ArrayList<>();
        List<TaxCalculationService.LineInput> taxLines = new ArrayList<>();
        for (PurchaseItemRequest_Dto itemReq : request.getItems()) {
            Product_entity product = productRepository.findByIdAndShopId(itemReq.getProductId(), shop.getId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found"));
            products.add(product);
            taxLines.add(new TaxCalculationService.LineInput(String.valueOf(product.getId()), itemReq.getQuantity(),
                    itemReq.getPurchasePrice(), product.getTaxType(), product.getGstRate(), product.getCessRate(), BigDecimal.ZERO));
        }

        String supplierState = supplier.getGstin() != null && GstinValidator.isValid(supplier.getGstin())
                ? GstinValidator.stateCode(supplier.getGstin()) : supplier.getStateCode();
        if (supplierState == null) supplierState = shop.getStateCode();
        TaxCalculationService.Calculation calculation = taxCalculationService.calculate(new TaxCalculationService.Request(
                supplierState, shop.getStateCode(),
                supplier.getGstin() == null ? RegistrationType.UNREGISTERED : RegistrationType.REGULAR,
                purchase.getDiscountAmount(), taxLines));

        for (int index = 0; index < products.size(); index++) {
            Product_entity product = products.get(index);
            PurchaseItemRequest_Dto itemReq = request.getItems().get(index);
            TaxCalculationService.LineResult result = calculation.lines().get(index);
            product.setStockQuantity(product.getStockQuantity() + itemReq.getQuantity().doubleValue());
            productRepository.save(product);
            PurchaseItem_entity item = PurchaseItem_entity.builder()
                    .purchase(purchase)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .purchasePrice(itemReq.getPurchasePrice())
                    .totalPrice(result.total())
                    .build();
            purchase.getItems().add(item);
        }
        
        purchase.setSubtotal(calculation.taxableAmount());
        purchase.setTaxAmount(calculation.cgst().add(calculation.sgst()).add(calculation.igst()).add(calculation.cess()));
        BigDecimal grandTotal = calculation.total();
        purchase.setGrandTotal(grandTotal);
        
        purchaseRepository.save(purchase);
        
        // Update Supplier Ledger for the new Purchase
        BigDecimal newBalance = supplier.getOutstandingBalance().add(grandTotal);
        supplier.setTotalPurchases(supplier.getTotalPurchases().add(grandTotal));
        supplier.setOutstandingBalance(newBalance);
        
        SupplierLedger_entity ledger = SupplierLedger_entity.builder()
                .shop(shop)
                .supplier(supplier)
                .purchase(purchase)
                .transactionDate(java.time.LocalDateTime.now())
                .type("PURCHASE")
                .debitAmount(grandTotal) // Increases debt
                .runningBalance(newBalance)
                .notes("Purchase " + purchaseNumber)
                .build();
        ledgerRepository.save(ledger);
        
        // Handle immediate payment if provided
        if (request.getAmountPaid() != null && request.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal payment = request.getAmountPaid();
            purchase.setAmountPaid(payment);
            if (payment.compareTo(grandTotal) >= 0) {
                purchase.setStatus("PAID");
            }
            purchaseRepository.save(purchase);
            
            BigDecimal balanceAfterPayment = newBalance.subtract(payment);
            supplier.setAmountPaid(supplier.getAmountPaid().add(payment));
            supplier.setOutstandingBalance(balanceAfterPayment);
            
            SupplierLedger_entity paymentLedger = SupplierLedger_entity.builder()
                    .shop(shop)
                    .supplier(supplier)
                    .purchase(purchase)
                    .transactionDate(java.time.LocalDateTime.now())
                    .type("PAYMENT")
                    .creditAmount(payment) // Decreases debt
                    .runningBalance(balanceAfterPayment)
                    .notes("Payment for " + purchaseNumber)
                    .build();
            ledgerRepository.save(paymentLedger);
        }
        
        supplierRepository.save(supplier);
        
        return mapToResponse(purchase);
    }

    
    @Transactional
    public PurchaseResponse_Dto recordPayment(User_entity user, Long id, java.math.BigDecimal amount, String paymentMethod, String notes) {
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        Purchase_entity purchase = purchaseRepository.findByIdAndShopId(id, shop.getId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "PURCHASE_NOT_FOUND", "Purchase not found"));
                
        if ("PAID".equals(purchase.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "PURCHASE_ALREADY_PAID", "Purchase is already fully paid");
        }

        if (purchase.getAmountPaid() == null) {
            purchase.setAmountPaid(java.math.BigDecimal.ZERO);
        }
        
        java.math.BigDecimal newAmountPaid = purchase.getAmountPaid().add(amount);
        purchase.setAmountPaid(newAmountPaid);
        
        if (newAmountPaid.compareTo(purchase.getGrandTotal()) >= 0) {
            purchase.setStatus("PAID");
        } else {
            purchase.setStatus("PARTIAL");
        }
        if (paymentMethod != null && !paymentMethod.trim().isEmpty()) {
            purchase.setPaymentMethod(paymentMethod);
        }
        
        purchase = purchaseRepository.save(purchase);
        
        com.example.Billing.supplier.Supplier_entity supplier = purchase.getSupplier();
        
        if ("ADVANCE_ADJUSTMENT".equals(paymentMethod)) {
            com.example.Billing.supplier.SupplierLedger_entity allocationLedger = com.example.Billing.supplier.SupplierLedger_entity.builder()
                    .shop(shop)
                    .supplier(supplier)
                    .purchase(purchase)
                    .transactionDate(java.time.LocalDateTime.now())
                    .type("ALLOCATION")
                    .creditAmount(java.math.BigDecimal.ZERO)
                    .runningBalance(supplier.getOutstandingBalance())
                    .notes("Allocated Advance to " + purchase.getPurchaseNumber() + " for Rs." + amount)
                    .build();
            ledgerRepository.save(allocationLedger);
        } else {
            java.math.BigDecimal balanceAfterPayment = supplier.getOutstandingBalance().subtract(amount);
            supplier.setAmountPaid(supplier.getAmountPaid().add(amount));
            supplier.setOutstandingBalance(balanceAfterPayment);
            supplierRepository.save(supplier);
            
            com.example.Billing.supplier.SupplierLedger_entity paymentLedger = com.example.Billing.supplier.SupplierLedger_entity.builder()
                    .shop(shop)
                    .supplier(supplier)
                    .purchase(purchase)
                    .transactionDate(java.time.LocalDateTime.now())
                    .type("PAYMENT")
                    .creditAmount(amount)
                    .runningBalance(balanceAfterPayment)
                    .notes((notes != null && !notes.trim().isEmpty() ? notes + " | " : "Payment for " + purchase.getPurchaseNumber() + " | ") + "Method: " + paymentMethod)
                    .build();
                    
            ledgerRepository.save(paymentLedger);
        }
        
        return mapToResponse(purchase);
    }

    private PurchaseResponse_Dto mapToResponse(Purchase_entity purchase) {
        return PurchaseResponse_Dto.builder()
                .id(purchase.getId())
                .shopId(purchase.getShop().getId())
                .supplierId(purchase.getSupplier().getId())
                .supplierName(purchase.getSupplier().getBusinessName())
                .supplierGstin(purchase.getSupplier().getGstin())
                .purchaseNumber(purchase.getPurchaseNumber())
                .subtotal(purchase.getSubtotal())
                .discountAmount(purchase.getDiscountAmount())
                .taxAmount(purchase.getTaxAmount())
                .grandTotal(purchase.getGrandTotal())
                .status(purchase.getStatus())
                .paymentMethod(purchase.getPaymentMethod())
                .amountPaid(purchase.getAmountPaid() != null ? purchase.getAmountPaid() : BigDecimal.ZERO)
                .createdAt(purchase.getCreatedAt())
                .createdBy(purchase.getCreatedBy() != null ? purchase.getCreatedBy().getName() : null)
                .items(purchase.getItems().stream().map(i -> PurchaseItemResponse_Dto.builder()
                        .id(i.getId())
                        .productId(i.getProduct().getId())
                        .productName(i.getProduct().getName())
                        .sku(i.getProduct().getSku())
                          .hsnCode(i.getProduct().getHsnCode())
                        .quantity(i.getQuantity())
                        .purchasePrice(i.getPurchasePrice())
                        .totalPrice(i.getTotalPrice())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
