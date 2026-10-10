package com.example.Billing.audit;

import com.example.Billing.auth.User_entity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditInterceptor implements HandlerInterceptor {
    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final AuditService auditService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!MUTATING.contains(request.getMethod()) || request.getRequestURI().startsWith("/api/auth/")) return;
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof User_entity user)) return;
        try {
            Long shopId = user.getShop() == null ? null : user.getShop().getId();
            auditService.record(shopId, user.getId(), request.getMethod(), request.getRequestURI(), response.getStatus());
        } catch (Exception exception) {
            log.error("Unable to persist audit event", exception);
        }
    }
}
