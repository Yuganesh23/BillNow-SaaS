package com.example.Billing.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long shopId, Long actorId, String method, String path, int status) {
        repository.save(AuditLog_entity.builder()
                .shopId(shopId).actorUserId(actorId).method(method).path(path)
                .status(status).createdAt(Instant.now()).build());
    }
}
