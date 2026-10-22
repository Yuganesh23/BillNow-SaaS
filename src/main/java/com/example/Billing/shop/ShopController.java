package com.example.Billing.shop;

import com.example.Billing.auth.User_entity;
import com.example.Billing.auth.userRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import com.example.Billing.common.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.example.Billing.gst.GstinValidator;
import com.example.Billing.gst.IndianState;
import com.example.Billing.gst.RegistrationType;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopRepository shopRepository;
    private final userRepository userRepository;
    private final com.example.Billing.subscription.PlanLimitsService planLimitsService;

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @GetMapping
    public ResponseEntity<List<Shop_entity>> getMyShops(Authentication authentication) {
        User_entity userDetails = (User_entity) authentication.getPrincipal();
        return ResponseEntity.ok(shopRepository.findByOwnerId(userDetails.getId()));
    }

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @PostMapping
    @Transactional
    public ResponseEntity<Shop_entity> createShop(
            Authentication authentication,
            @RequestBody ShopCreateRequest request
    ) {
        User_entity userDetails = (User_entity) authentication.getPrincipal();
        User_entity owner = userRepository.findById(userDetails.getId()).orElseThrow();
        planLimitsService.assertCanCreateBranch(com.example.Billing.config.BranchContextHolder.getBranchId() == null
                ? shopRepository.findByOwnerId(owner.getId()).stream().findFirst()
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SHOP_NOT_FOUND", "Shop not found"))
                : shopRepository.findByIdAndOwnerId(com.example.Billing.config.BranchContextHolder.getBranchId(), owner.getId())
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SHOP_NOT_FOUND", "Shop not found")));

        String stateCode = resolveStateCode(request.getGstin(), request.getStateCode());
        Shop_entity newShop = Shop_entity.builder()
                .name(request.getName())
                .invoiceName(request.getInvoiceName())
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .gstin(normalizeGstin(request.getGstin()))
                .stateCode(stateCode)
                .registrationType(request.getRegistrationType() == null ? RegistrationType.UNREGISTERED : request.getRegistrationType())
                .hsnCodeLength(request.getHsnCodeLength() == null ? 4 : validHsnLength(request.getHsnCodeLength()))
                .legalName(request.getLegalName())
                .state(request.getState())
                .address(request.getAddress())
                .locality(request.getLocality())
                .pincode(validPincode(request.getPincode()))
                .owner(owner)
                .active(true)
                .trialEndsAt(java.time.LocalDateTime.now().plusDays(7))
                .build();

        return ResponseEntity.ok(shopRepository.save(newShop));
    }

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Shop_entity> updateShop(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody ShopUpdateRequest request
    ) {
        User_entity userDetails = (User_entity) authentication.getPrincipal();
        Shop_entity shop = shopRepository.findByIdAndOwnerId(id, userDetails.getId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SHOP_NOT_FOUND", "Shop not found"));

        if (request.getName() != null) shop.setName(request.getName());
        if (request.getInvoiceName() != null) shop.setInvoiceName(request.getInvoiceName());
        if (request.getEmail() != null) shop.setEmail(request.getEmail());
        if (request.getMobileNumber() != null) shop.setMobileNumber(request.getMobileNumber());
        if (request.getGstin() != null) {
            shop.setGstin(normalizeGstin(request.getGstin()));
            if (!request.getGstin().isBlank()) shop.setStateCode(GstinValidator.stateCode(request.getGstin()));
        }
        if (request.getLegalName() != null) shop.setLegalName(request.getLegalName());
        if (request.getState() != null) shop.setState(request.getState());
        if (request.getStateCode() != null && (request.getGstin() == null || request.getGstin().isBlank())) {
            shop.setStateCode(resolveStateCode(null, request.getStateCode()));
        }
        if (request.getRegistrationType() != null) shop.setRegistrationType(request.getRegistrationType());
        if (request.getHsnCodeLength() != null) shop.setHsnCodeLength(validHsnLength(request.getHsnCodeLength()));
        if (request.getInvoicePrefix() != null) shop.setInvoicePrefix(request.getInvoicePrefix());
        if (request.getBankDetails() != null) shop.setBankDetails(request.getBankDetails());
        if (request.getInvoiceTerms() != null) shop.setInvoiceTerms(request.getInvoiceTerms());
        if (request.getUpiId() != null) shop.setUpiId(request.getUpiId());
        if (request.getAddress() != null) shop.setAddress(request.getAddress());
        if (request.getLocality() != null) shop.setLocality(request.getLocality());
        if (request.getPincode() != null) shop.setPincode(validPincode(request.getPincode()));
        if (request.getLogoBase64() != null && !request.getLogoBase64().isBlank()) {
            shop.setLogoBase64(request.getLogoBase64());
            shop.setLogoObjectKey(null);
        }
        if (request.getLogoObjectKey() != null && !request.getLogoObjectKey().isBlank()) {
            shop.setLogoObjectKey(request.getLogoObjectKey());
            shop.setLogoBase64(null);
        }

        return ResponseEntity.ok(shopRepository.save(shop));
    }

    private String normalizeGstin(String gstin) {
        if (gstin == null || gstin.isBlank()) return null;
        String value = gstin.trim().toUpperCase(java.util.Locale.ROOT);
        if (!GstinValidator.isValid(value)) throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_GSTIN", "GSTIN is invalid");
        return value;
    }

    private String resolveStateCode(String gstin, String stateCode) {
        if (gstin != null && !gstin.isBlank()) return GstinValidator.stateCode(normalizeGstin(gstin));
        if (IndianState.fromCode(stateCode).isEmpty()) throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_STATE_CODE", "Select a valid Indian state or UT");
        return stateCode;
    }

    private int validHsnLength(int value) {
        if (value != 4 && value != 6 && value != 8) throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_HSN_LENGTH", "HSN length must be 4, 6, or 8");
        return value;
    }

    private String validPincode(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (!normalized.matches("[1-9][0-9]{5}"))
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_PINCODE", "PIN code must contain six digits and cannot start with zero");
        return normalized;
    }

    @PreAuthorize("hasRole('SHOP_OWNER')")
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteShop(
            Authentication authentication,
            @PathVariable Long id
    ) {
        User_entity userDetails = (User_entity) authentication.getPrincipal();
        Shop_entity shop = shopRepository.findByIdAndOwnerId(id, userDetails.getId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SHOP_NOT_FOUND", "Shop not found"));

        try {
            shopRepository.delete(shop);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Cannot delete branch. It contains existing data (invoices, products, etc)."));
        }
    }

}

class ErrorResponse {
    private String message;
    public ErrorResponse(String message) { this.message = message; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    
}


@lombok.Data
class ShopUpdateRequest {
    private String name;
    private String invoiceName;
    private String email;
    private String mobileNumber;
    private String gstin;
    private String legalName;
    private String state;
    private String address;
    private String locality;
    private String pincode;
    private String logoBase64;
    private String logoObjectKey;
    private String stateCode;
    private RegistrationType registrationType;
    private Integer hsnCodeLength;
    private String invoicePrefix;
    private String bankDetails;
    private String invoiceTerms;
    private String upiId;
}

@lombok.Data
class ShopCreateRequest {
    private String name;
    private String invoiceName;
    private String email;
    private String mobileNumber;
    private String gstin;
    private String legalName;
    private String state;
    private String address;
    private String locality;
    private String pincode;
    private String stateCode;
    private RegistrationType registrationType;
    private Integer hsnCodeLength;
}
