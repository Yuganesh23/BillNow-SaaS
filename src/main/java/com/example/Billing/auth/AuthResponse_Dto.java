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
}
