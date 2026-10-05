package com.example.Billing.auth;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse_Dto {
    private String token;

    private Long userId;

    private String name;

    private String email;

    private String role;

    private Long shopId;
    
    private String shopName;
    private String invoiceName;
    private String shopEmail;
    private String shopMobileNumber;
    private String shopAddress;
    private String logoBase64;
}
