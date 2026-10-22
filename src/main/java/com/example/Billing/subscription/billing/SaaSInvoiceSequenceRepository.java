package com.example.Billing.subscription.billing;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SaaSInvoiceSequenceRepository extends JpaRepository<SaaSInvoiceSequence_entity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SaaSInvoiceSequence_entity s where s.financialYear = :fy")
    Optional<SaaSInvoiceSequence_entity> findForUpdate(@Param("fy") String financialYear);
}
