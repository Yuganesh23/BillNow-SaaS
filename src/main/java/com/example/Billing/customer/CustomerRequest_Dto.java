package com.example.Billing.customer;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class CustomerRequest_Dto {
    @NotBlank(message = "Customer name is required")
    private String name;

    @NotBlank(message = "WhatsApp number is required")
    private String whatsappNumber;

    @Email(message = "Invalid email")
    private String email;

    private String address;
}
