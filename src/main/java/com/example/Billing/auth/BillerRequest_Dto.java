package com.example.Billing.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillerRequest_Dto {

    @NotBlank(message = "Biller name is required")
    private String name;

    @NotBlank(message = "Biller email is required")
    @Email(message = "Invalid email")
    private String email;

    
    
    private String password;
    private String mobileNumber;
}
