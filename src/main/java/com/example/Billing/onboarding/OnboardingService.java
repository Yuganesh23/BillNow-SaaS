package com.example.Billing.onboarding;

import com.example.Billing.auth.User_entity;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingService {
    private final ShopContextResolver shopContextResolver;
    private final ProductRepository productRepository;
    private final InvoiceRepository invoiceRepository;

    public OnboardingResponse status(User_entity user) {
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        Long shopId = shop.getId();
        List<OnboardingResponse.Step> steps = List.of(
                step("shop", "Add shop details", "/settings", hasText(shop.getName())
                        && hasText(shop.getMobileNumber()) && hasText(shop.getAddress())),
                step("gst", "Configure GST registration", "/settings", shop.getRegistrationType() != null
                        && hasText(shop.getStateCode())),
                step("logo", "Upload your logo", "/settings", hasText(shop.getLogoBase64())
                        || hasText(shop.getLogoObjectKey())),
                step("invoice-series", "Choose invoice series", "/settings", hasText(shop.getInvoicePrefix())),
                step("product", "Add your first product", "/products", productRepository.countByShopIdAndActiveTrue(shopId) > 0),
                step("invoice", "Create your first invoice", "/invoices/create", invoiceRepository.countByShopId(shopId) > 0)
        );
        int completed = (int) steps.stream().filter(OnboardingResponse.Step::complete).count();
        int percent = Math.round((completed * 100f) / steps.size());
        return new OnboardingResponse(completed, steps.size(), percent, completed == steps.size(), steps);
    }

    private OnboardingResponse.Step step(String key, String label, String route, boolean complete) {
        return new OnboardingResponse.Step(key, label, route, complete);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
