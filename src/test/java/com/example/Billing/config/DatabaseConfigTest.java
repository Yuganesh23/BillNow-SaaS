package com.example.Billing.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseConfigTest {

    @Test
    void convertsNeonUrlWithEmbeddedCredentialsToJdbc() {
        DatabaseConfig.DatabaseConnection connection = DatabaseConfig.resolve(
                "postgresql://billnow:secret%3Avalue@example-pooler.neon.tech/neondb?sslmode=require",
                null,
                null
        );

        assertEquals(
                "jdbc:postgresql://example-pooler.neon.tech/neondb?sslmode=require",
                connection.jdbcUrl()
        );
        assertEquals("billnow", connection.username());
        assertEquals("secret:value", connection.password());
    }

    @Test
    void preservesJdbcUrlAndSeparateCredentials() {
        DatabaseConfig.DatabaseConnection connection = DatabaseConfig.resolve(
                "jdbc:postgresql://example-pooler.neon.tech/neondb?sslmode=require",
                "billnow",
                "secret"
        );

        assertEquals(
                "jdbc:postgresql://example-pooler.neon.tech/neondb?sslmode=require",
                connection.jdbcUrl()
        );
        assertEquals("billnow", connection.username());
        assertEquals("secret", connection.password());
    }
}
