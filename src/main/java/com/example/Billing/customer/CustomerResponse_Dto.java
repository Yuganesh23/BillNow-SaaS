package com.example.Billing.customer;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse_Dto {
    private Long id;

    private Long shopId;

    private String name;

    private String whatsappNumber;

    private String email;

    private String address;
    private String gstin;
    private String stateCode;
    private String billingAddress;
    private String shippingAddress;
}
