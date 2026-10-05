package com.example.Billing.reports;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/reports")
public class DebugController6 {

    private final com.example.Billing.customer.CustomerRepository customerRepository;

    public DebugController6(com.example.Billing.customer.CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/debug6")
    public ResponseEntity<String> debug() {
        try {
            long count = customerRepository.count();
            return ResponseEntity.ok("Success! Customers count: " + count);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok("Error: " + e.getMessage());
        }
    }
}
