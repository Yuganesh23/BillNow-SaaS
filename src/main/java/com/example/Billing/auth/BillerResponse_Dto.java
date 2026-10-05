package com.example.Billing.auth;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillerResponse_Dto {

    private Long id;

    private Long shopId;

    private String name;

    private String email;
    private String mobileNumber;

    private String role;

    private boolean active;

    private long totalInvoices;
    
    private BigDecimal totalSales;
}

