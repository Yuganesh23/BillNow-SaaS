package com.example.Billing.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class BranchInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String branchId = request.getHeader("X-Branch-Id");
        if (branchId != null && !branchId.isEmpty()) {
            try {
                BranchContextHolder.setBranchId(Long.parseLong(branchId));
            } catch (NumberFormatException e) {
                // Ignore invalid
            }
        } else {
            BranchContextHolder.clear();
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BranchContextHolder.clear();
    }
}
