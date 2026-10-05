package com.example.Billing.supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface SupplierLedgerRepository extends JpaRepository<SupplierLedger_entity, Long> {
    List<SupplierLedger_entity> findBySupplierIdOrderByTransactionDateDesc(Long supplierId);
    List<SupplierLedger_entity> findByPurchaseIdOrderByTransactionDateDesc(Long purchaseId);
}
