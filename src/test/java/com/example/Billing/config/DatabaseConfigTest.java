package com.example.Billing.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConfigTest {

    @Test
    @DisplayName("Should parse standard Neon postgresql:// URL with embedded credentials and query params")
    void testParseStandardNeonPostgresqlUrl() {
        String neonUrl = "postgresql://neondb_owner:npg_SecretPass123@ep-sparkling-paper-aoczto3f.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require";

        DatabaseConfig.ConnectionDetails details = DatabaseConfig.parseDatabaseUrl(neonUrl, null, null);

        assertNotNull(details);
        assertEquals("neondb_owner", details.getUsername());
        assertEquals("npg_SecretPass123", details.getPassword());
        assertEquals("ep-sparkling-paper-aoczto3f.c-2.ap-southeast-1.aws.neon.tech", details.getHost());
        assertEquals("neondb", details.getDatabaseName());
        assertEquals("org.postgresql.Driver", details.getDriverClassName());
        assertTrue(details.isPostgres());
        assertTrue(details.isSsl());
        assertEquals("jdbc:postgresql://ep-sparkling-paper-aoczto3f.c-2.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require", details.getJdbcUrl());
    }

    @Test
    @DisplayName("Should parse shorthand postgres:// URL and enforce sslmode=require if omitted")
    void testParseShorthandNeonPostgresUrlWithoutSsl() {
        String neonUrl = "postgres://neondb_owner:npg_SecretPass123@ep-sparkling-paper-aoczto3f.c-2.ap-southeast-1.aws.neon.tech/neondb";

        DatabaseConfig.ConnectionDetails details = DatabaseConfig.parseDatabaseUrl(neonUrl, null, null);

        assertNotNull(details);
        assertEquals("neondb_owner", details.getUsername());
        assertEquals("npg_SecretPass123", details.getPassword());
        assertTrue(details.isSsl());
        assertTrue(details.getJdbcUrl().contains("sslmode=require"));
        assertTrue(details.getJdbcUrl().startsWith("jdbc:postgresql://"));
    }

    @Test
    @DisplayName("Should correctly URL-decode special characters in password")
    void testUrlDecodedPassword() {
        // Password with URL-encoded '@' (%40) and '#' (%23)
        String neonUrl = "postgresql://neon_user:my%40Pass%23word@ep-test.aws.neon.tech:5432/my_database?sslmode=require";

        DatabaseConfig.ConnectionDetails details = DatabaseConfig.parseDatabaseUrl(neonUrl, null, null);

        assertNotNull(details);
        assertEquals("neon_user", details.getUsername());
        assertEquals("my@Pass#word", details.getPassword());
        assertEquals("my_database", details.getDatabaseName());
    }

    @Test
    @DisplayName("Should support standard JDBC PostgreSQL URL with fallback environment credentials")
    void testParseStandardJdbcPostgresUrl() {
        String jdbcUrl = "jdbc:postgresql://ep-sparkling-paper-aoczto3f.c-2.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require";

        DatabaseConfig.ConnectionDetails details = DatabaseConfig.parseDatabaseUrl(jdbcUrl, "env_user", "env_pass");

        assertNotNull(details);
        assertEquals("env_user", details.getUsername());
        assertEquals("env_pass", details.getPassword());
        assertEquals("org.postgresql.Driver", details.getDriverClassName());
        assertTrue(details.isPostgres());
        assertTrue(details.isSsl());
        assertEquals(jdbcUrl, details.getJdbcUrl());
    }

    @Test
    @DisplayName("Should automatically append sslmode=require to Neon JDBC URL if missing")
    void testNeonJdbcUrlAppendsSsl() {
        String jdbcUrl = "jdbc:postgresql://ep-sparkling-paper-aoczto3f.c-2.ap-southeast-1.aws.neon.tech:5432/neondb";

        DatabaseConfig.ConnectionDetails details = DatabaseConfig.parseDatabaseUrl(jdbcUrl, "env_user", "env_pass");

        assertTrue(details.getJdbcUrl().contains("sslmode=require"));
    }

    @Test
    @DisplayName("Should strip accidental surrounding quotes from environment variables")
    void testStripQuotesFromUrl() {
        String quotedUrl = "\"postgresql://neondb_owner:secret@ep-test.neon.tech/neondb?sslmode=require\"";

        DatabaseConfig.ConnectionDetails details = DatabaseConfig.parseDatabaseUrl(quotedUrl, null, null);

        assertEquals("neondb_owner", details.getUsername());
        assertEquals("secret", details.getPassword());
        assertEquals("jdbc:postgresql://ep-test.neon.tech:5432/neondb?sslmode=require", details.getJdbcUrl());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on null or empty database URL")
    void testEmptyUrlThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.parseDatabaseUrl(null, null, null));
        assertThrows(IllegalArgumentException.class, () -> DatabaseConfig.parseDatabaseUrl("   ", null, null));
    }
}
