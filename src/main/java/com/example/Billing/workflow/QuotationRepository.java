package com.example.Billing.workflow;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface QuotationRepository extends JpaRepository<Quotation_entity, Long> {
    Optional<Quotation_entity> findByIdAndShopId(Long id, Long shopId);
    List<Quotation_entity> findByShopIdOrderByCreatedAtDesc(Long shopId);
}
