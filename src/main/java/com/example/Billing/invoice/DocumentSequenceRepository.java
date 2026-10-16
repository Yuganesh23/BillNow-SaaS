package com.example.Billing.invoice;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence_entity, Long> {
    List<DocumentSequence_entity> findByShopIdOrderByFinancialYearDescDocumentTypeAsc(Long shopId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DocumentSequence_entity d where d.shop.id = :shopId and d.financialYear = :fy and d.documentType = :type")
    Optional<DocumentSequence_entity> findForUpdate(@Param("shopId") Long shopId,
                                                     @Param("fy") String financialYear,
                                                     @Param("type") String documentType);
}
