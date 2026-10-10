package com.example.Billing.shop;

import com.example.Billing.auth.User_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.Billing.config.ShopContextResolver;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ShopWhatsAppConfigService {

    private final ShopWhatsAppConfigRepository configRepository;
    private final ShopContextResolver shopContextResolver;
    private final com.example.Billing.security.SecretStringCipher secretStringCipher;


    // =====================================================
    // SAVE / UPDATE CONFIG
    // =====================================================

    public ShopWhatsAppConfigResponse_Dto saveConfig(
            User_entity owner,
            ShopWhatsAppConfigRequest_Dto request
    ) {

        // -------------------------------------------------
        // CHECK OWNER
        // -------------------------------------------------

        if (!"SHOP_OWNER".equals(owner.getRole())) {

            throw new RuntimeException(
                    "Only shop owner can configure WhatsApp"
            );
        }


        // -------------------------------------------------
        // GET SHOP
        // -------------------------------------------------

        Shop_entity shop =
                shopContextResolver.resolveActiveShop(owner);

        if (shop == null) {

            throw new RuntimeException(
                    "Owner is not associated with a shop"
            );
        }


        // -------------------------------------------------
        // FIND EXISTING CONFIG
        // -------------------------------------------------

        ShopWhatsAppConfig config =
                configRepository
                        .findByShopId(shop.getId())
                        .orElse(null);


        // -------------------------------------------------
        // CREATE IF NOT EXISTS
        // -------------------------------------------------

        if (config == null) {

            config =
                    ShopWhatsAppConfig.builder()
                            .shop(shop)
                            .phoneNumberId(
                                    request.getPhoneNumberId().trim()
                            )
                            .businessAccountId(
                                    request.getBusinessAccountId()
                            )
                            .accessToken(
                                    secretStringCipher.encrypt(request.getAccessToken().trim())
                            )
                            .enabled(
                                    request.getEnabled() == null
                                            || request.getEnabled()
                            )
                            .build();

        } else {

            // ---------------------------------------------
            // UPDATE
            // ---------------------------------------------

            config.setPhoneNumberId(
                    request.getPhoneNumberId().trim()
            );

            config.setBusinessAccountId(
                    request.getBusinessAccountId()
            );

            config.setAccessToken(
                    secretStringCipher.encrypt(request.getAccessToken().trim())
            );

            if (request.getEnabled() != null) {

                config.setEnabled(
                        request.getEnabled()
                );
            }
        }


        // -------------------------------------------------
        // SAVE
        // -------------------------------------------------

        ShopWhatsAppConfig saved =
                configRepository.save(config);


        return mapToResponse(saved);
    }


    // =====================================================
    // GET CONFIG
    // =====================================================

    @Transactional(readOnly = true)
    public ShopWhatsAppConfigResponse_Dto getConfig(
            User_entity owner
    ) {

        if (!"SHOP_OWNER".equals(owner.getRole())) {

            throw new RuntimeException(
                    "Only shop owner can view WhatsApp configuration"
            );
        }


        Shop_entity shop =
                shopContextResolver.resolveActiveShop(owner);

        if (shop == null) {

            throw new RuntimeException(
                    "Owner is not associated with a shop"
            );
        }


        ShopWhatsAppConfig config =
                configRepository
                        .findByShopId(shop.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "WhatsApp configuration not found"
                                )
                        );


        return mapToResponse(config);
    }


    // =====================================================
    // ENABLE / DISABLE
    // =====================================================

    public ShopWhatsAppConfigResponse_Dto setEnabled(
            User_entity owner,
            boolean enabled
    ) {

        if (!"SHOP_OWNER".equals(owner.getRole())) {

            throw new RuntimeException(
                    "Only shop owner can change WhatsApp status"
            );
        }


        Shop_entity shop =
                shopContextResolver.resolveActiveShop(owner);

        if (shop == null) {

            throw new RuntimeException(
                    "Owner is not associated with a shop"
            );
        }


        ShopWhatsAppConfig config =
                configRepository
                        .findByShopId(shop.getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "WhatsApp configuration not found"
                                )
                        );


        config.setEnabled(enabled);


        return mapToResponse(
                configRepository.save(config)
        );
    }


    // =====================================================
    // MAPPER
    // =====================================================

    private ShopWhatsAppConfigResponse_Dto mapToResponse(
            ShopWhatsAppConfig config
    ) {

        return ShopWhatsAppConfigResponse_Dto.builder()
                .id(config.getId())
                .shopId(
                        config.getShop().getId()
                )
                .phoneNumberId(
                        config.getPhoneNumberId()
                )
                .businessAccountId(
                        config.getBusinessAccountId()
                )
                .enabled(
                        config.isEnabled()
                )
                .build();
    }
}
