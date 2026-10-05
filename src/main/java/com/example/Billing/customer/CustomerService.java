package com.example.Billing.customer;

import com.example.Billing.auth.User_entity;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.Billing.config.ShopContextResolver;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        Customer_entity customer =
                Customer_entity.builder()
                        .shop(shop)
                        .name(request.getName())
                        .whatsappNumber(
                                request.getWhatsappNumber()
                        )
                        .email(request.getEmail())
                        .address(request.getAddress())
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
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found in this shop"
                                )
                        );

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
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found in this shop"
                                )
                        );


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
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Customer not found in this shop"
                                )
                        );

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

                .build();
    }
}