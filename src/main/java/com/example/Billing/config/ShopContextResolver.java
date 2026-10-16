package com.example.Billing.config;

import com.example.Billing.auth.User_entity;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.shop.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.example.Billing.common.AppException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ShopContextResolver {

    private final ShopRepository shopRepository;

    public Shop_entity resolveActiveShop(User_entity user) {
        if (user == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Authentication is required");
        }
        if ("BILLER".equals(user.getRole())) {
            if (user.getShop() == null) {
                throw new AppException(HttpStatus.FORBIDDEN, "SHOP_ACCESS_DENIED", "User is not assigned to a shop");
            }
            return user.getShop(); // Billers strictly bound to one shop
        }

        // For SHOP_OWNER
        Long branchId = BranchContextHolder.getBranchId();
        if (branchId != null) {
            return shopRepository.findByIdAndOwnerId(branchId, user.getId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "BRANCH_NOT_FOUND", "Branch not found"));
        }

        // Fallback to first shop (legacy compatibility)
        List<Shop_entity> shops = shopRepository.findByOwnerId(user.getId());
        if (shops.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "SHOP_NOT_FOUND", "Shop not found");
        }
        return shops.get(0);
    }

    /**
     * Resolves and authorizes the tenant without attaching a Shop entity to the
     * current persistence context. Use this before acquiring a pessimistic shop
     * lock so a stale, already-managed version cannot conflict with the lock.
     */
    public Long resolveActiveShopId(User_entity user) {
        if (user == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Authentication is required");
        }
        if ("BILLER".equals(user.getRole())) {
            if (user.getShop() == null || user.getShop().getId() == null) {
                throw new AppException(HttpStatus.FORBIDDEN, "SHOP_ACCESS_DENIED", "User is not assigned to a shop");
            }
            return user.getShop().getId();
        }

        Long branchId = BranchContextHolder.getBranchId();
        if (branchId != null) {
            if (!shopRepository.existsByIdAndOwnerId(branchId, user.getId())) {
                throw new AppException(HttpStatus.NOT_FOUND, "BRANCH_NOT_FOUND", "Branch not found");
            }
            return branchId;
        }

        return shopRepository.findFirstIdByOwnerId(user.getId())
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND, "SHOP_NOT_FOUND", "Shop not found"));
    }
}
