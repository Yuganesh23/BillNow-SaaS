package com.example.Billing.customer;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import com.example.Billing.config.ShopContextResolver;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.example.Billing.gst.GstinValidator;
import com.example.Billing.gst.IndianState;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ShopContextResolver shopContextResolver;


    // =========================================================
    // CREATE CUSTOMER
    // =========================================================

    public CustomerResponse_Dto createCustomer(
            User_entity user,
            CustomerRequest_Dto request
    ) {

        Shop_entity shop = getUserShop(user);

        // Check duplicate WhatsApp number inside this shop
        if (customerRepository
                .findByShopIdAndWhatsappNumber(
                        shop.getId(),
                        request.getWhatsappNumber()
                )
                .isPresent()) {

            throw new RuntimeException(
                    "Customer with this WhatsApp number already exists"
            );
        }

        String stateCode = resolveStateCode(request.getGstin(), request.getStateCode(), shop.getStateCode());
        Customer_entity customer =
                Customer_entity.builder()
                        .shop(shop)
                        .name(request.getName())
                        .whatsappNumber(
                                request.getWhatsappNumber()
                        )
                        .email(request.getEmail())
                        .address(request.getAddress())
                        .gstin(normalizeGstin(request.getGstin()))
                        .stateCode(stateCode)
                        .billingAddress(request.getBillingAddress() != null ? request.getBillingAddress() : request.getAddress())
                        .shippingAddress(request.getShippingAddress())
                        .build();

        Customer_entity savedCustomer =
                customerRepository.save(customer);

        return mapToResponse(savedCustomer);
    }


    // =========================================================
    // GET ALL CUSTOMERS
    // =========================================================

    @Transactional(readOnly = true)
    public List<CustomerResponse_Dto> getAllCustomers(
            User_entity user
    ) {

        Shop_entity shop = getUserShop(user);

        return customerRepository
                .findByShopId(shop.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =========================================================
    // GET CUSTOMER BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public CustomerResponse_Dto getCustomer(
            User_entity user,
            Long customerId
    ) {

        Shop_entity shop = getUserShop(user);

        Customer_entity customer =
                customerRepository
                        .findByIdAndShopId(
                                customerId,
                                shop.getId()
                        )
                        .orElseThrow(() -> customerNotFound());

        return mapToResponse(customer);
    }


    // =========================================================
    // UPDATE CUSTOMER
    // =========================================================

    public CustomerResponse_Dto updateCustomer(
            User_entity user,
            Long customerId,
            CustomerRequest_Dto request
    ) {

        Shop_entity shop = getUserShop(user);

        Customer_entity customer =
                customerRepository
                        .findByIdAndShopId(
                                customerId,
                                shop.getId()
                        )
                        .orElseThrow(() -> customerNotFound());


        // Check whether WhatsApp number belongs
        // to another customer in the same shop
        customerRepository
                .findByShopIdAndWhatsappNumber(
                        shop.getId(),
                        request.getWhatsappNumber()
                )
                .ifPresent(existingCustomer -> {

                    if (!existingCustomer
                            .getId()
                            .equals(customerId)) {

                        throw new RuntimeException(
                                "Another customer already uses this WhatsApp number"
                        );
                    }
                });


        customer.setName(
                request.getName()
        );

        customer.setWhatsappNumber(
                request.getWhatsappNumber()
        );

        customer.setEmail(
                request.getEmail()
        );

        customer.setAddress(
                request.getAddress()
        );
        customer.setGstin(normalizeGstin(request.getGstin()));
        customer.setStateCode(resolveStateCode(request.getGstin(), request.getStateCode(), shop.getStateCode()));
        customer.setBillingAddress(request.getBillingAddress() != null ? request.getBillingAddress() : request.getAddress());
        customer.setShippingAddress(request.getShippingAddress());

        Customer_entity updatedCustomer =
                customerRepository.save(customer);

        return mapToResponse(updatedCustomer);
    }


    // =========================================================
    // DELETE CUSTOMER
    // =========================================================

    public void deleteCustomer(
            User_entity user,
            Long customerId
    ) {

        Shop_entity shop = getUserShop(user);

        Customer_entity customer =
                customerRepository
                        .findByIdAndShopId(
                                customerId,
                                shop.getId()
                        )
                        .orElseThrow(() -> customerNotFound());

        customerRepository.delete(customer);
    }


    // =========================================================
    // GET USER SHOP
    // =========================================================

    private Shop_entity getUserShop(
            User_entity user
    ) {

        if (user == null) {

            throw new RuntimeException(
                    "User is not authenticated"
            );
        }

        if (shopContextResolver.resolveActiveShop(user) == null) {

            throw new RuntimeException(
                    "User is not associated with a shop"
            );
        }

        if (!shopContextResolver.resolveActiveShop(user).isActive()) {

            throw new RuntimeException(
                    "Shop is not active"
            );
        }

        return shopContextResolver.resolveActiveShop(user);
    }

    private AppException customerNotFound() {
        return new AppException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found");
    }


    // =========================================================
    // ENTITY → RESPONSE DTO
    // =========================================================

    private CustomerResponse_Dto mapToResponse(
            Customer_entity customer
    ) {

        return CustomerResponse_Dto.builder()

                .id(
                        customer.getId()
                )

                .shopId(
                        customer
                                .getShop()
                                .getId()
                )

                .name(
                        customer.getName()
                )

                .whatsappNumber(
                        customer.getWhatsappNumber()
                )

                .email(
                        customer.getEmail()
                )

                .address(
                        customer.getAddress()
                )
                .gstin(customer.getGstin())
                .stateCode(customer.getStateCode())
                .billingAddress(customer.getBillingAddress())
                .shippingAddress(customer.getShippingAddress())

                .build();
    }

    private String normalizeGstin(String gstin) {
        if (gstin == null || gstin.isBlank()) return null;
        String normalized = gstin.trim().toUpperCase(java.util.Locale.ROOT);
        if (!GstinValidator.isValid(normalized)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_GSTIN", "GSTIN is invalid");
        }
        return normalized;
    }

    private String resolveStateCode(String gstin, String manual, String fallback) {
        if (gstin != null && !gstin.isBlank()) return GstinValidator.stateCode(normalizeGstin(gstin));
        String code = manual != null && !manual.isBlank() ? manual : fallback;
        if (IndianState.fromCode(code).isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_STATE_CODE", "Select a valid Indian state or UT");
        }
        return code;
    }
}
