package com.example.Billing.supplier;

import com.example.Billing.auth.User_entity;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierLedgerRepository ledgerRepository;
    private final ShopContextResolver shopContextResolver;
    private final com.example.Billing.purchase.PurchaseRepository purchaseRepository;

    @Transactional(readOnly = true)
    public List<SupplierResponse_Dto> getAllSuppliers(User_entity user) {
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        return supplierRepository.findByShopIdOrderByCreatedAtDesc(shop.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplierResponse_Dto getSupplierById(User_entity user, Long id) {
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        Supplier_entity supplier = supplierRepository.findByIdAndShopId(id, shop.getId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        return mapToResponse(supplier);
    }

    @Transactional
    public SupplierResponse_Dto createSupplier(User_entity user, SupplierRequest_Dto request) {
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        
        Supplier_entity supplier = Supplier_entity.builder()
                .shop(shop)
                .businessName(request.getBusinessName())
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .whatsapp(request.getWhatsapp())
                .email(request.getEmail())
                .gstin(request.getGstin())
                .address(request.getAddress())
                .notes(request.getNotes())
                .build();
                
        return mapToResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse_Dto updateSupplier(User_entity user, Long id, SupplierRequest_Dto request) {
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        Supplier_entity supplier = supplierRepository.findByIdAndShopId(id, shop.getId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
                
        if (request.getBusinessName() != null) supplier.setBusinessName(request.getBusinessName());
        if (request.getContactPerson() != null) supplier.setContactPerson(request.getContactPerson());
        if (request.getPhone() != null) supplier.setPhone(request.getPhone());
        if (request.getWhatsapp() != null) supplier.setWhatsapp(request.getWhatsapp());
        if (request.getEmail() != null) supplier.setEmail(request.getEmail());
        if (request.getGstin() != null) supplier.setGstin(request.getGstin());
        if (request.getAddress() != null) supplier.setAddress(request.getAddress());
        if (request.getNotes() != null) supplier.setNotes(request.getNotes());
        
        return mapToResponse(supplierRepository.save(supplier));
    }

    @Transactional(readOnly = true)
    public List<SupplierLedgerResponse_Dto> getLedger(User_entity user, Long id) {
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        Supplier_entity supplier = supplierRepository.findByIdAndShopId(id, shop.getId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
                
        return ledgerRepository.findBySupplierIdOrderByTransactionDateDesc(supplier.getId())
                .stream().map(l -> SupplierLedgerResponse_Dto.builder()
                        .id(l.getId())
                        .transactionDate(l.getTransactionDate())
                        .type(l.getType())
                        .reference(l.getPurchase() != null ? l.getPurchase().getPurchaseNumber() : null)
                        .debitAmount(l.getDebitAmount())
                        .creditAmount(l.getCreditAmount())
                        .runningBalance(l.getRunningBalance())
                        .notes(l.getNotes())
                        .productNames(l.getPurchase() != null && l.getPurchase().getItems() != null ? l.getPurchase().getItems().stream().map(i -> i.getProduct() != null ? i.getProduct().getName() : "Unknown").collect(java.util.stream.Collectors.joining(", ")) : null)
                        .build()
                ).collect(Collectors.toList());
    }

    @Transactional
    public void recordPayment(Shop_entity shop, Supplier_entity supplier, BigDecimal amount, String notes) {
        // Decrease outstanding balance
        BigDecimal newBalance = supplier.getOutstandingBalance().subtract(amount);
        supplier.setAmountPaid(supplier.getAmountPaid().add(amount));
        supplier.setOutstandingBalance(newBalance);
        supplierRepository.save(supplier);
        
        // Ledger entry
        SupplierLedger_entity ledger = SupplierLedger_entity.builder()
                .shop(shop)
                .supplier(supplier)
                .transactionDate(java.time.LocalDateTime.now())
                .type("PAYMENT")
                .creditAmount(amount)
                .runningBalance(newBalance)
                .notes(notes)
                .build();
        ledgerRepository.save(ledger);
    }

    
    @Transactional
    public void recordPayment(User_entity user, Long supplierId, BigDecimal amount, String paymentMethod, String notes) {
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        Supplier_entity supplier = supplierRepository.findByIdAndShopId(supplierId, shop.getId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        BigDecimal remainingAmount = amount;
        
        // Auto-allocate across unpaid purchases
        List<com.example.Billing.purchase.Purchase_entity> unpaidPurchases = purchaseRepository.findBySupplierIdOrderByCreatedAtDesc(supplier.getId())
                .stream()
                .filter(p -> !"PAID".equals(p.getStatus()))
                .sorted(java.util.Comparator.comparing(com.example.Billing.purchase.Purchase_entity::getCreatedAt))
                .collect(java.util.stream.Collectors.toList());
                
        for (com.example.Billing.purchase.Purchase_entity p : unpaidPurchases) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;
            
            BigDecimal balanceDue = p.getGrandTotal().subtract(p.getAmountPaid() != null ? p.getAmountPaid() : BigDecimal.ZERO);
            if (balanceDue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal paymentForThis = remainingAmount.min(balanceDue);
                
                // Update purchase
                p.setAmountPaid((p.getAmountPaid() != null ? p.getAmountPaid() : BigDecimal.ZERO).add(paymentForThis));
                if (p.getAmountPaid().compareTo(p.getGrandTotal()) >= 0) {
                    p.setStatus("PAID");
                } else {
                    p.setStatus("PARTIAL");
                }
                purchaseRepository.save(p);
                
                // Ledger entry for this allocation
                BigDecimal newSupplierBalance = supplier.getOutstandingBalance().subtract(paymentForThis);
                supplier.setAmountPaid(supplier.getAmountPaid().add(paymentForThis));
                supplier.setOutstandingBalance(newSupplierBalance);
                
                SupplierLedger_entity allocLedger = SupplierLedger_entity.builder()
                        .shop(shop)
                        .supplier(supplier)
                        .purchase(p)
                        .transactionDate(java.time.LocalDateTime.now())
                        .type("PAYMENT")
                        .creditAmount(paymentForThis)
                        .runningBalance(newSupplierBalance)
                        .notes("Payment for " + p.getPurchaseNumber() + " | Method: " + paymentMethod)
                        .build();
                ledgerRepository.save(allocLedger);
                
                remainingAmount = remainingAmount.subtract(paymentForThis);
            }
        }
        
        // If there is still money left over (Advance), log it generally
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal newSupplierBalance = supplier.getOutstandingBalance().subtract(remainingAmount);
            supplier.setAmountPaid(supplier.getAmountPaid().add(remainingAmount));
            supplier.setOutstandingBalance(newSupplierBalance);
            
            SupplierLedger_entity advanceLedger = SupplierLedger_entity.builder()
                    .shop(shop)
                    .supplier(supplier)
                    .transactionDate(java.time.LocalDateTime.now())
                    .type("PAYMENT")
                    .creditAmount(remainingAmount)
                    .runningBalance(newSupplierBalance)
                    .notes((notes != null && !notes.trim().isEmpty() ? notes + " | " : "Advance Payment | ") + "Method: " + paymentMethod)
                    .build();
            ledgerRepository.save(advanceLedger);
        }
        
        supplierRepository.save(supplier);
    }

    @Transactional
    public void deleteSupplier(User_entity user, Long id) {
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        Supplier_entity supplier = supplierRepository.findByIdAndShopId(id, shop.getId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        if (supplier.getTotalPurchases().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Cannot delete supplier with existing transaction history.");
        }
        
        supplierRepository.delete(supplier);
    }

    public SupplierResponse_Dto mapToResponse(Supplier_entity supplier) {
        return SupplierResponse_Dto.builder()
                .id(supplier.getId())
                .shopId(supplier.getShop().getId())
                .businessName(supplier.getBusinessName())
                .contactPerson(supplier.getContactPerson())
                .phone(supplier.getPhone())
                .whatsapp(supplier.getWhatsapp())
                .email(supplier.getEmail())
                .gstin(supplier.getGstin())
                .address(supplier.getAddress())
                .notes(supplier.getNotes())
                .totalPurchases(supplier.getTotalPurchases())
                .amountPaid(supplier.getAmountPaid())
                .outstandingBalance(supplier.getOutstandingBalance())
                .createdAt(supplier.getCreatedAt())
                .build();
    }
}
