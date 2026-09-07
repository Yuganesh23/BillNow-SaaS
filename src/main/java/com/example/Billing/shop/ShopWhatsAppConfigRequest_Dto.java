package com.example.Billing.shop;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopWhatsAppConfigRequest_Dto {

    @NotBlank(message = "WhatsApp Phone Number ID is required")
    private String phoneNumberId;

    private String businessAccountId;

    @NotBlank(message = "WhatsApp access token is required")
    private String accessToken;

    private Boolean enabled;
}