package com.example.Billing.config;

import org.flywaydb.core.Flyway;

/** Exposes the package-private bootstrap step to tests in other packages. */
public final class FlywayBootstrapAccess {
    private FlywayBootstrapAccess() {}

    public static void bootstrapIfEmpty(Flyway flyway) {
        FlywayBootstrapConfig.bootstrapIfEmpty(flyway);
    }
}
