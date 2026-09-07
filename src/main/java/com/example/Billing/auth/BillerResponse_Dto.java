package com.example.Billing.auth;

import lombok.*;

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

    private String role;

    private boolean active;
}