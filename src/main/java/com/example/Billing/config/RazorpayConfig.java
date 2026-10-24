package com.example.Billing.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RazorpayConfig {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;


    @Bean
    public RazorpayClient razorpayClient()
            throws RazorpayException {

        String cleanKeyId = keyId != null ? keyId.trim() : "";
        String cleanSecret = keySecret != null ? keySecret.trim() : "";

        log.info("Initializing RazorpayClient with Key ID: [{}]", cleanKeyId);

        return new RazorpayClient(
                cleanKeyId,
                cleanSecret
        );
    }


    @Bean
    public RazorpayProperties razorpayProperties() {

        String cleanKeyId = keyId != null ? keyId.trim() : "";
        return new RazorpayProperties(
                cleanKeyId
        );
    }


    public record RazorpayProperties(
            String keyId
    ) {
    }
}