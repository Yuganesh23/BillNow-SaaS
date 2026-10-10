package com.example.Billing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final BranchInterceptor branchInterceptor;
    private final com.example.Billing.audit.AuditInterceptor auditInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(branchInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(auditInterceptor)
                .addPathPatterns("/api/**");
    }
}
