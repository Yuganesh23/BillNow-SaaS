package com.example.Billing.workflow;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReminderLogRepository extends JpaRepository<ReminderLog_entity, Long> {
    boolean existsByShopIdAndReminderKeyAndChannel(Long shopId, String reminderKey, String channel);
}
