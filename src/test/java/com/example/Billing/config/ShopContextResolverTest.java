package com.example.Billing.config;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.shop.ShopRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopContextResolverTest {
    @AfterEach
    void clearBranch() {
        BranchContextHolder.clear();
    }

    @Test
    void ownerCannotResolveAnotherOwnersBranch() {
        ShopRepository repository = mock(ShopRepository.class);
        ShopContextResolver resolver = new ShopContextResolver(repository);
        User_entity owner = User_entity.builder().id(10L).role("SHOP_OWNER").build();
        BranchContextHolder.setBranchId(99L);
        when(repository.findByIdAndOwnerId(99L, 10L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> resolver.resolveActiveShop(owner));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("BRANCH_NOT_FOUND", exception.getCode());
        verify(repository).findByIdAndOwnerId(99L, 10L);
    }
}
