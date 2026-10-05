package com.example.Billing.config;

import com.example.Billing.auth.User_entity;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.shop.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ShopContextResolver {

    private final ShopRepository shopRepository;

    public Shop_entity resolveActiveShop(User_entity user) {
        if ("BILLER".equals(user.getRole())) {
            return user.getShop(); // Billers strictly bound to one shop
        }

        // For SHOP_OWNER
        Long branchId = BranchContextHolder.getBranchId();
        if (branchId != null) {
            Shop_entity shop = shopRepository.findById(branchId)
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
            
            if (!shop.getOwner().getId().equals(user.getId())) {
                throw new RuntimeException("Unauthorized branch access");
            }
            return shop;
        }

        // Fallback to first shop (legacy compatibility)
        List<Shop_entity> shops = shopRepository.findByOwnerId(user.getId());
        if (shops.isEmpty()) {
            return null;
        }
        return shops.get(0);
    }
}
