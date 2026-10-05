package com.example.Billing.purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface PurchaseRepository extends JpaRepository<Purchase_entity, Long> {
    List<Purchase_entity> findByShopIdOrderByCreatedAtDesc(Long shopId);
    Optional<Purchase_entity> findByIdAndShopId(Long id, Long shopId);
    List<Purchase_entity> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);
    long countByShopId(Long shopId);
}
