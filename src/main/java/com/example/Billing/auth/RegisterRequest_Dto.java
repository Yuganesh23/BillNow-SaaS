package com.example.Billing.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest_Dto {

    // =====================================================
    // OWNER NAME
    // =====================================================

    @NotBlank(message = "Owner name is required")
    private String ownerName;


    // =====================================================
    // SHOP NAME
    // =====================================================

    @NotBlank(message = "Shop name is required")
    @Size(
            max = 150,
            message = "Shop name cannot exceed 150 characters"
    )
    private String shopName;


    // =====================================================
    // EMAIL
    // =====================================================

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String email;


    // =====================================================
    // PASSWORD
    // =====================================================

    @NotBlank(message = "Password is required")
    @Size(
            min = 6,
            message = "Password must contain at least 6 characters"
    )
    private String password;

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    @NotBlank(message = "Address is required")
    private String address;

    private String logoBase64;
}