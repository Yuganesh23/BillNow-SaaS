package com.example.Billing.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier_entity, Long> {
    List<Supplier_entity> findByShopIdOrderByCreatedAtDesc(Long shopId);
    Optional<Supplier_entity> findByIdAndShopId(Long id, Long shopId);
}
