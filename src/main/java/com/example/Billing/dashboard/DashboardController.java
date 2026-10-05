package com.example.Billing.dashboard;

import com.example.Billing.auth.User_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;


    // =====================================================
    // GET DASHBOARD
    // =====================================================

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'BILLER')")
    @GetMapping
    public ResponseEntity<DashboardResponse_Dto> getDashboard(
            Authentication authentication
    ) {

        User_entity user =
                getAuthenticatedUser(authentication);


        DashboardResponse_Dto response =
                dashboardService.getDashboard(user);


        return ResponseEntity.ok(response);
    }


    // =====================================================
    // AUTHENTICATED USER
    // =====================================================

    private User_entity getAuthenticatedUser(
            Authentication authentication
    ) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "User is not authenticated"
            );
        }


        Object principal =
                authentication.getPrincipal();


        if (!(principal instanceof User_entity)) {

            throw new RuntimeException(
                    "Invalid authenticated user"
            );
        }


        return (User_entity) principal;
    }
}
