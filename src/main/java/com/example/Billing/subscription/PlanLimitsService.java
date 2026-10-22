package com.example.Billing.subscription;

import com.example.Billing.auth.userRepository;
import com.example.Billing.common.AppException;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.shop.SubscriptionTier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class PlanLimitsService {
    private final InvoiceRepository invoices;
    private final userRepository users;
    private final ShopRepository shops;
    private final Limits trial;
    private final Limits base;
    private final Limits pro;
    private final Limits enterprise;

    public PlanLimitsService(InvoiceRepository invoices, userRepository users, ShopRepository shops,
            @Value("${billing.plan-limits.trial.invoices-per-month:25}") int trialInvoices,
            @Value("${billing.plan-limits.base.invoices-per-month:250}") int baseInvoices,
            @Value("${billing.plan-limits.pro.invoices-per-month:2000}") int proInvoices,
            @Value("${billing.plan-limits.enterprise.invoices-per-month:100000}") int enterpriseInvoices,
            @Value("${billing.plan-limits.trial.users:1}") int trialUsers,
            @Value("${billing.plan-limits.base.users:2}") int baseUsers,
            @Value("${billing.plan-limits.pro.users:10}") int proUsers,
            @Value("${billing.plan-limits.enterprise.users:100}") int enterpriseUsers,
            @Value("${billing.plan-limits.trial.branches:1}") int trialBranches,
            @Value("${billing.plan-limits.base.branches:1}") int baseBranches,
            @Value("${billing.plan-limits.pro.branches:5}") int proBranches,
            @Value("${billing.plan-limits.enterprise.branches:100}") int enterpriseBranches) {
        this.invoices = invoices; this.users = users; this.shops = shops;
        this.trial = new Limits(trialInvoices, trialUsers, trialBranches, false);
        this.base = new Limits(baseInvoices, baseUsers, baseBranches, false);
        this.pro = new Limits(proInvoices, proUsers, proBranches, true);
        this.enterprise = new Limits(enterpriseInvoices, enterpriseUsers, enterpriseBranches, true);
    }

    public void assertCanCreateInvoice(Shop_entity shop) {
        assertEntitled(shop);
        Usage usage = usage(shop);
        if (usage.invoicesThisMonth() >= usage.limits().invoicesPerMonth())
            throw limit("INVOICE_LIMIT_REACHED", "Monthly invoice limit reached. Upgrade your plan to continue.");
    }

    public void assertCanCreateUser(Shop_entity shop) {
        assertEntitled(shop);
        Usage usage = usage(shop);
        if (usage.activeUsers() >= usage.limits().users())
            throw limit("USER_LIMIT_REACHED", "Active user limit reached. Upgrade your plan to add staff.");
    }

    public void assertCanCreateBranch(Shop_entity currentShop) {
        assertEntitled(currentShop);
        Usage usage = usage(currentShop);
        if (usage.branches() >= usage.limits().branches())
            throw limit("BRANCH_LIMIT_REACHED", "Branch limit reached. Upgrade your plan to add another branch.");
    }

    public void assertEInvoiceAllowed(Shop_entity shop) {
        assertEntitled(shop);
        if (!limits(shop).eInvoice()) throw limit("E_INVOICE_PLAN_REQUIRED", "E-invoicing requires the PRO or ENTERPRISE plan.");
    }

    public Usage usage(Shop_entity shop) {
        Limits limits = limits(shop);
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return new Usage(shop.getSubscriptionTier() == null ? SubscriptionTier.TRIAL : shop.getSubscriptionTier(), limits,
                invoices.countByShopIdAndCreatedAtGreaterThanEqual(shop.getId(), monthStart),
                users.countByShopIdAndActiveTrue(shop.getId()), shops.findByOwnerId(shop.getOwner().getId()).size());
    }

    public void assertEntitled(Shop_entity shop) {
        LocalDateTime now = LocalDateTime.now();
        SubscriptionTier tier = shop.getSubscriptionTier() == null ? SubscriptionTier.TRIAL : shop.getSubscriptionTier();
        if (tier == SubscriptionTier.TRIAL && shop.getTrialEndsAt() != null && now.isAfter(shop.getTrialEndsAt()))
            throw limit("TRIAL_EXPIRED", "Your trial has expired. Choose a paid plan to continue.");
        if (tier != SubscriptionTier.TRIAL && shop.getSubscriptionEndsAt() != null && now.isAfter(shop.getSubscriptionEndsAt()))
            throw limit("SUBSCRIPTION_EXPIRED", "Your subscription has expired. Renew it to continue.");
    }

    private Limits limits(Shop_entity shop) {
        return limitsFor(shop.getSubscriptionTier() == null ? SubscriptionTier.TRIAL : shop.getSubscriptionTier());
    }

    public Limits limitsFor(SubscriptionTier tier) {
        return switch (tier) { case TRIAL -> trial; case BASE -> base; case PRO -> pro; case ENTERPRISE -> enterprise; };
    }

    private AppException limit(String code, String message) { return new AppException(HttpStatus.PAYMENT_REQUIRED, code, message); }
    public record Limits(int invoicesPerMonth, int users, int branches, boolean eInvoice) {}
    public record Usage(SubscriptionTier tier, Limits limits, long invoicesThisMonth, long activeUsers, long branches) {}
}
