package com.example.Billing.shop;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopWhatsAppConfigResponse_Dto {

    private Long id;

    private Long shopId;

    private String phoneNumberId;

    private String businessAccountId;

    private boolean enabled;
}