package com.example.Billing.subscription;

import com.example.Billing.auth.User_entity;
import com.example.Billing.auth.userRepository;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.shop.SubscriptionTier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Centralized service enforcing plan limits and feature entitlements
 * across all XenBill subscription tiers (BASE, PRO, ENTERPRISE, TRIAL).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEntitlementService {

    private final userRepository userRepository;
    private final ShopRepository shopRepository;

    /**
     * Asserts whether a shop can add an additional staff member (Biller).
     * - BASE: 0 staff accounts (1 admin/owner only).
     * - PRO / TRIAL: Up to 3 staff accounts.
     * - ENTERPRISE: Unlimited staff accounts.
     */
    public void assertCanCreateBiller(Shop_entity shop) {
        if (shop == null) {
            throw new IllegalArgumentException("Shop context is required");
        }

        SubscriptionTier tier = shop.getSubscriptionTier() != null 
                ? shop.getSubscriptionTier() 
                : SubscriptionTier.TRIAL;

        if (tier == SubscriptionTier.BASE) {
            log.warn("Staff account creation blocked for shop {} on BASE tier", shop.getId());
            throw new SubscriptionEntitlementException(
                    "Staff accounts are not included in the BASE plan (1 Administrator only). Please upgrade to PRO to add up to 3 staff members.",
                    "PRO"
            );
        }

        if (tier == SubscriptionTier.PRO || tier == SubscriptionTier.TRIAL) {
            long existingBillers = userRepository.countByShopIdAndRole(shop.getId(), "BILLER");
            if (existingBillers >= 3) {
                log.warn("Staff account creation blocked for shop {}: limit of 3 reached (tier: {})", shop.getId(), tier);
                throw new SubscriptionEntitlementException(
                        "You have reached the limit of 3 staff accounts on your current plan. Please upgrade to ENTERPRISE for unlimited staff accounts.",
                        "ENTERPRISE"
                );
            }
        }

        // ENTERPRISE has unlimited staff accounts
    }

    /**
     * Asserts whether an owner can create an additional shop / branch.
     * - BASE & PRO: 1 primary shop/branch only.
     * - ENTERPRISE: Multi-branch allowed.
     */
    public void assertCanCreateBranch(User_entity owner) {
        if (owner == null) {
            throw new IllegalArgumentException("Owner context is required");
        }

        List<Shop_entity> existingShops = shopRepository.findByOwnerId(owner.getId());
        if (!existingShops.isEmpty()) {
            boolean hasEnterprise = existingShops.stream()
                    .anyMatch(s -> s.getSubscriptionTier() == SubscriptionTier.ENTERPRISE);

            if (!hasEnterprise) {
                log.warn("Branch creation blocked for owner {}: requires ENTERPRISE plan", owner.getId());
                throw new SubscriptionEntitlementException(
                        "Multi-branch support is an ENTERPRISE plan feature. Please upgrade to ENTERPRISE to create additional branches.",
                        "ENTERPRISE"
                );
            }
        }
    }

    /**
     * Asserts whether a shop can send invoices via WhatsApp.
     * - BASE: Blocked.
     * - PRO, ENTERPRISE, TRIAL: Allowed.
     */
    public void assertCanUseWhatsApp(Shop_entity shop) {
        if (shop == null) {
            throw new IllegalArgumentException("Shop context is required");
        }

        SubscriptionTier tier = shop.getSubscriptionTier() != null 
                ? shop.getSubscriptionTier() 
                : SubscriptionTier.TRIAL;

        if (tier == SubscriptionTier.BASE) {
            log.warn("WhatsApp invoice sending blocked for shop {} on BASE tier", shop.getId());
            throw new SubscriptionEntitlementException(
                    "WhatsApp invoice delivery is available on PRO and ENTERPRISE plans. Please upgrade to unlock WhatsApp integration.",
                    "PRO"
            );
        }
    }

    /**
     * Checks whether low-stock alert emails are permitted for a shop.
     * - BASE: Disabled.
     * - PRO, ENTERPRISE, TRIAL: Enabled.
     */
    public boolean canSendLowStockAlert(Shop_entity shop) {
        if (shop == null) {
            return false;
        }

        SubscriptionTier tier = shop.getSubscriptionTier() != null 
                ? shop.getSubscriptionTier() 
                : SubscriptionTier.TRIAL;

        return tier != SubscriptionTier.BASE;
    }

    /**
     * Asserts whether a shop can export GST tax reports in Excel / CSV format.
     * - BASE & PRO: Blocked.
     * - ENTERPRISE & TRIAL: Allowed.
     */
    public void assertCanExportGstExcel(Shop_entity shop) {
        if (shop == null) {
            throw new IllegalArgumentException("Shop context is required");
        }

        SubscriptionTier tier = shop.getSubscriptionTier() != null 
                ? shop.getSubscriptionTier() 
                : SubscriptionTier.TRIAL;

        if (tier != SubscriptionTier.ENTERPRISE && tier != SubscriptionTier.TRIAL) {
            log.warn("GST Excel export blocked for shop {} on tier {}", shop.getId(), tier);
            throw new SubscriptionEntitlementException(
                    "GST Excel export is an ENTERPRISE plan feature. Please upgrade to ENTERPRISE to export detailed GST tax reports.",
                    "ENTERPRISE"
            );
        }
    }
}
