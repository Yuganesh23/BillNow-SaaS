package com.example.Billing.workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReminderLogRepository extends JpaRepository<ReminderLog_entity, Long> {
    List<ReminderLog_entity> findByShopIdOrderBySentAtDesc(Long shopId);
    boolean existsByShopIdAndReminderKeyAndChannel(Long shopId, String reminderKey, String channel);
}
