package com.example.Billing.portal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerPortalAccessRepository extends JpaRepository<CustomerPortalAccess, Long> {
    Optional<CustomerPortalAccess> findByTokenHash(String tokenHash);
    List<CustomerPortalAccess> findByCustomerIdAndShopIdAndRevokedAtIsNull(Long customerId, Long shopId);
}
