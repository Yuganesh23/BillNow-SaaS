package com.example.Billing.reports;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReportExportSettingsRepository extends JpaRepository<ReportExportSettings_entity, Long> {
    Optional<ReportExportSettings_entity> findByShopId(Long shopId);
}
