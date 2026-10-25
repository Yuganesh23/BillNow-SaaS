package com.example.Billing.subscription;

import com.example.Billing.auth.User_entity;
import com.example.Billing.auth.userRepository;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.shop.SubscriptionTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubscriptionEntitlementServiceTest {

    @Mock
    private userRepository userRepository;

    @Mock
    private ShopRepository shopRepository;

    @InjectMocks
    private SubscriptionEntitlementService entitlementService;

    private User_entity owner;
    private Shop_entity baseShop;
    private Shop_entity proShop;
    private Shop_entity enterpriseShop;
    private Shop_entity trialShop;

    @BeforeEach
    void setUp() {
        owner = User_entity.builder().id(1L).email("owner@xenbill.com").role("SHOP_OWNER").build();

        baseShop = Shop_entity.builder().id(100L).name("Base Store").owner(owner).subscriptionTier(SubscriptionTier.BASE).build();
        proShop = Shop_entity.builder().id(200L).name("Pro Store").owner(owner).subscriptionTier(SubscriptionTier.PRO).build();
        enterpriseShop = Shop_entity.builder().id(300L).name("Enterprise Store").owner(owner).subscriptionTier(SubscriptionTier.ENTERPRISE).build();
        trialShop = Shop_entity.builder().id(400L).name("Trial Store").owner(owner).subscriptionTier(SubscriptionTier.TRIAL).build();
    }

    // =========================================================================
    // 1. STAFF LIMITS (BILLERS)
    // =========================================================================

    @Test
    @DisplayName("BASE plan must block staff account creation (0 staff allowed)")
    void testBasePlan_BlocksStaffCreation() {
        SubscriptionEntitlementException ex = assertThrows(SubscriptionEntitlementException.class, () -> {
            entitlementService.assertCanCreateBiller(baseShop);
        });

        assertEquals("PRO", ex.getRequiredTier());
        assertTrue(ex.getMessage().contains("BASE plan"));
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("PRO plan must allow staff account creation when count < 3")
    void testProPlan_AllowsStaffCreation_UnderLimit() {
        when(userRepository.countByShopIdAndRole(proShop.getId(), "BILLER")).thenReturn(2L);

        assertDoesNotThrow(() -> entitlementService.assertCanCreateBiller(proShop));
    }

    @Test
    @DisplayName("PRO plan must block 4th staff account when count reaches 3")
    void testProPlan_BlocksStaffCreation_AtLimit() {
        when(userRepository.countByShopIdAndRole(proShop.getId(), "BILLER")).thenReturn(3L);

        SubscriptionEntitlementException ex = assertThrows(SubscriptionEntitlementException.class, () -> {
            entitlementService.assertCanCreateBiller(proShop);
        });

        assertEquals("ENTERPRISE", ex.getRequiredTier());
        assertTrue(ex.getMessage().contains("limit of 3 staff accounts"));
    }

    @Test
    @DisplayName("ENTERPRISE plan must allow unlimited staff accounts")
    void testEnterprisePlan_AllowsUnlimitedStaffCreation() {
        assertDoesNotThrow(() -> entitlementService.assertCanCreateBiller(enterpriseShop));
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("TRIAL plan must allow up to 3 staff accounts and block at 3")
    void testTrialPlan_AllowsStaffUpToLimit() {
        when(userRepository.countByShopIdAndRole(trialShop.getId(), "BILLER")).thenReturn(2L);
        assertDoesNotThrow(() -> entitlementService.assertCanCreateBiller(trialShop));

        when(userRepository.countByShopIdAndRole(trialShop.getId(), "BILLER")).thenReturn(3L);
        assertThrows(SubscriptionEntitlementException.class, () -> entitlementService.assertCanCreateBiller(trialShop));
    }

    // =========================================================================
    // 2. MULTI-BRANCH LIMITS
    // =========================================================================

    @Test
    @DisplayName("BASE and PRO owners must be blocked from adding a 2nd branch")
    void testBaseAndProOwners_BlockedFromCreatingSecondBranch() {
        when(shopRepository.findByOwnerId(owner.getId())).thenReturn(List.of(proShop));

        SubscriptionEntitlementException ex = assertThrows(SubscriptionEntitlementException.class, () -> {
            entitlementService.assertCanCreateBranch(owner);
        });

        assertEquals("ENTERPRISE", ex.getRequiredTier());
        assertTrue(ex.getMessage().contains("ENTERPRISE plan"));
    }

    @Test
    @DisplayName("ENTERPRISE owners must be allowed to create additional branches")
    void testEnterpriseOwner_AllowsCreatingMultipleBranches() {
        when(shopRepository.findByOwnerId(owner.getId())).thenReturn(List.of(enterpriseShop));

        assertDoesNotThrow(() -> entitlementService.assertCanCreateBranch(owner));
    }

    @Test
    @DisplayName("First shop creation during registration must be allowed")
    void testFirstShopCreation_AllowedForNewOwner() {
        when(shopRepository.findByOwnerId(owner.getId())).thenReturn(List.of());

        assertDoesNotThrow(() -> entitlementService.assertCanCreateBranch(owner));
    }

    // =========================================================================
    // 3. WHATSAPP INTEGRATION
    // =========================================================================

    @Test
    @DisplayName("BASE plan must block WhatsApp invoice delivery")
    void testBasePlan_BlocksWhatsApp() {
        SubscriptionEntitlementException ex = assertThrows(SubscriptionEntitlementException.class, () -> {
            entitlementService.assertCanUseWhatsApp(baseShop);
        });

        assertEquals("PRO", ex.getRequiredTier());
        assertTrue(ex.getMessage().contains("WhatsApp"));
    }

    @Test
    @DisplayName("PRO, ENTERPRISE, and TRIAL plans must allow WhatsApp delivery")
    void testProAndEnterprise_AllowWhatsApp() {
        assertDoesNotThrow(() -> entitlementService.assertCanUseWhatsApp(proShop));
        assertDoesNotThrow(() -> entitlementService.assertCanUseWhatsApp(enterpriseShop));
        assertDoesNotThrow(() -> entitlementService.assertCanUseWhatsApp(trialShop));
    }

    // =========================================================================
    // 4. LOW-STOCK EMAIL ALERTS
    // =========================================================================

    @Test
    @DisplayName("BASE plan must have low-stock alerts disabled; PRO & ENTERPRISE enabled")
    void testLowStockAlertEntitlement() {
        assertFalse(entitlementService.canSendLowStockAlert(baseShop));
        assertTrue(entitlementService.canSendLowStockAlert(proShop));
        assertTrue(entitlementService.canSendLowStockAlert(enterpriseShop));
        assertTrue(entitlementService.canSendLowStockAlert(trialShop));
    }

    // =========================================================================
    // 5. GST EXCEL EXPORTS
    // =========================================================================

    @Test
    @DisplayName("BASE and PRO plans must block GST Excel export")
    void testBaseAndPro_BlockGstExcelExport() {
        SubscriptionEntitlementException exBase = assertThrows(SubscriptionEntitlementException.class, () -> {
            entitlementService.assertCanExportGstExcel(baseShop);
        });
        assertEquals("ENTERPRISE", exBase.getRequiredTier());

        SubscriptionEntitlementException exPro = assertThrows(SubscriptionEntitlementException.class, () -> {
            entitlementService.assertCanExportGstExcel(proShop);
        });
        assertEquals("ENTERPRISE", exPro.getRequiredTier());
    }

    @Test
    @DisplayName("ENTERPRISE plan must allow GST Excel export")
    void testEnterprise_AllowsGstExcelExport() {
        assertDoesNotThrow(() -> entitlementService.assertCanExportGstExcel(enterpriseShop));
    }
}
