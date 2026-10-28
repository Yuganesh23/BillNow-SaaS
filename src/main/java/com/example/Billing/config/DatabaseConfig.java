package com.example.Billing.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Production-ready Database Configuration for Neon PostgreSQL on Google Cloud Run.
 *
 * Automatically detects and adapts:
 * 1. Standard RFC 3986 Neon PostgreSQL URLs: postgresql://user:pass@host:port/database?sslmode=require
 * 2. Shorthand URIs: postgres://...
 * 3. Standard JDBC URLs: jdbc:postgresql://host:port/database?sslmode=require
 * 4. Separate credentials via DATABASE_USERNAME and DATABASE_PASSWORD
 *
 * Ensures SSL is enforced, credentials are not exposed in logs, and connection pooling
 * is optimized for Cloud Run serverless execution.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        // Priority 1: DATABASE_URL (Google Cloud Run Secret Manager / Environment)
        String rawUrl = env.getProperty("DATABASE_URL");
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            // Priority 2: SPRING_DATASOURCE_URL (standard Spring fallback)
            rawUrl = env.getProperty("SPRING_DATASOURCE_URL");
        }
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            // Priority 3: spring.datasource.url property
            rawUrl = env.getProperty("spring.datasource.url");
        }

        String defaultUsername = env.getProperty("DATABASE_USERNAME");
        if (defaultUsername == null || defaultUsername.trim().isEmpty()) {
            defaultUsername = env.getProperty("SPRING_DATASOURCE_USERNAME");
        }
        if (defaultUsername == null || defaultUsername.trim().isEmpty()) {
            defaultUsername = env.getProperty("spring.datasource.username");
        }

        String defaultPassword = env.getProperty("DATABASE_PASSWORD");
        if (defaultPassword == null || defaultPassword.trim().isEmpty()) {
            defaultPassword = env.getProperty("SPRING_DATASOURCE_PASSWORD");
        }
        if (defaultPassword == null || defaultPassword.trim().isEmpty()) {
            defaultPassword = env.getProperty("spring.datasource.password");
        }

        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            throw new IllegalStateException(
                "Missing database connection URL. Please configure the 'DATABASE_URL' environment variable " +
                "in Google Cloud Run (from Secret Manager) to connect to Neon PostgreSQL."
            );
        }

        ConnectionDetails details = parseDatabaseUrl(rawUrl, defaultUsername, defaultPassword);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(details.getJdbcUrl());
        if (details.getUsername() != null && !details.getUsername().trim().isEmpty()) {
            config.setUsername(details.getUsername().trim());
        }
        if (details.getPassword() != null && !details.getPassword().trim().isEmpty()) {
            config.setPassword(details.getPassword());
        }
        if (details.getDriverClassName() != null && !details.getDriverClassName().trim().isEmpty()) {
            config.setDriverClassName(details.getDriverClassName());
        }

        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setPoolName("XenBillHikariPool");

        if (details.isPostgres()) {
            config.addDataSourceProperty("reWriteBatchedInserts", "true");
            config.addDataSourceProperty("stringtype", "unspecified");
        }

        // Safe logging: Never log password or full connection string with credentials
        log.info("Initialized XenBill DataSource [{}] -> Host: {}, Database: {}, SSL: {}, Pool: [min={}, max={}]",
                details.getDriverClassName(), details.getHost(), details.getDatabaseName(), details.isSsl(), minIdle, maxPoolSize);

        return new HikariDataSource(config);
    }

    /**
     * Parse and normalize database URLs for Neon PostgreSQL and JDBC drivers.
     */
    public static ConnectionDetails parseDatabaseUrl(String rawUrl, String defaultUsername, String defaultPassword) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Database connection URL cannot be null or empty.");
        }

        String clean = rawUrl.trim();
        // Strip accidental enclosing quotes from environment variables
        if ((clean.startsWith("\"") && clean.endsWith("\"")) || (clean.startsWith("'") && clean.endsWith("'"))) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }

        // Case 1: Standard RFC 3986 Neon URL (postgresql:// or postgres://)
        if (clean.startsWith("postgresql://") || clean.startsWith("postgres://")) {
            return parseNeonUri(clean, defaultUsername, defaultPassword);
        }

        // Case 2: JDBC PostgreSQL URL (jdbc:postgresql://...)
        if (clean.startsWith("jdbc:postgresql://")) {
            return parseJdbcPostgresUrl(clean, defaultUsername, defaultPassword);
        }

        // Case 3: Other JDBC URL (e.g., MySQL during local migration/dev)
        String driver = clean.startsWith("jdbc:mysql:") ? "com.mysql.cj.jdbc.Driver" : null;
        return new ConnectionDetails(clean, defaultUsername, defaultPassword, driver, "other-host", "other-db", false, false);
    }

    private static ConnectionDetails parseNeonUri(String uriString, String defaultUsername, String defaultPassword) {
        try {
            // Replace scheme with http:// so java.net.URI parses userinfo, host, port, path, and query accurately
            URI uri = URI.create(uriString.replaceFirst("^postgres(ql)?://", "http://"));
            String userInfo = uri.getUserInfo();
            String username = defaultUsername;
            String password = defaultPassword;

            if (userInfo != null && !userInfo.isEmpty()) {
                int colonIdx = userInfo.indexOf(':');
                if (colonIdx >= 0) {
                    username = URLDecoder.decode(userInfo.substring(0, colonIdx), StandardCharsets.UTF_8);
                    password = URLDecoder.decode(userInfo.substring(colonIdx + 1), StandardCharsets.UTF_8);
                } else {
                    username = URLDecoder.decode(userInfo, StandardCharsets.UTF_8);
                }
            }

            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("Invalid PostgreSQL URI: Missing host in '" + uriString + "'");
            }

            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            if (path == null || path.isEmpty() || path.equals("/")) {
                path = "/neondb";
            }
            String databaseName = path.startsWith("/") ? path.substring(1) : path;

            // Parse and preserve query parameters, guaranteeing sslmode=require for Neon
            Map<String, String> queryParams = new LinkedHashMap<>();
            String rawQuery = uri.getRawQuery();
            if (rawQuery != null && !rawQuery.isEmpty()) {
                for (String param : rawQuery.split("&")) {
                    int eqIdx = param.indexOf('=');
                    if (eqIdx > 0) {
                        queryParams.put(param.substring(0, eqIdx), param.substring(eqIdx + 1));
                    } else if (!param.isEmpty()) {
                        queryParams.put(param, "");
                    }
                }
            }

            // Remove channel_binding if present: pgBouncer / Neon connection pooler does not support channel binding
            queryParams.remove("channel_binding");

            // Neon requires SSL connection
            if (!queryParams.containsKey("sslmode")) {
                queryParams.put("sslmode", "require");
            }

            StringBuilder queryBuilder = new StringBuilder();
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (queryBuilder.length() > 0) {
                    queryBuilder.append("&");
                }
                queryBuilder.append(entry.getKey()).append("=").append(entry.getValue());
            }

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + databaseName + "?" + queryBuilder;
            boolean ssl = "require".equalsIgnoreCase(queryParams.get("sslmode")) ||
                          "verify-full".equalsIgnoreCase(queryParams.get("sslmode")) ||
                          "verify-ca".equalsIgnoreCase(queryParams.get("sslmode"));

            return new ConnectionDetails(
                jdbcUrl,
                username,
                password,
                "org.postgresql.Driver",
                host,
                databaseName,
                ssl,
                true
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Neon PostgreSQL URL format: " + e.getMessage(), e);
        }
    }

    private static ConnectionDetails parseJdbcPostgresUrl(String jdbcUrl, String defaultUsername, String defaultPassword) {
        String url = jdbcUrl;
        // Ensure sslmode=require if connecting to Neon without explicit parameter
        if (url.contains("neon.tech") && !url.contains("sslmode=")) {
            url += (url.contains("?") ? "&" : "?") + "sslmode=require";
        }

        String host = "postgres-host";
        String dbName = "neondb";
        try {
            String stripped = url.substring("jdbc:postgresql://".length());
            int slashIdx = stripped.indexOf('/');
            if (slashIdx > 0) {
                host = stripped.substring(0, slashIdx);
                int qIdx = stripped.indexOf('?', slashIdx);
                dbName = qIdx > 0 ? stripped.substring(slashIdx + 1, qIdx) : stripped.substring(slashIdx + 1);
            }
        } catch (Exception ignored) {}

        boolean ssl = url.contains("sslmode=");
        return new ConnectionDetails(
            url,
            defaultUsername,
            defaultPassword,
            "org.postgresql.Driver",
            host,
            dbName,
            ssl,
            true
        );
    }

    public static class ConnectionDetails {
        private final String jdbcUrl;
        private final String username;
        private final String password;
        private final String driverClassName;
        private final String host;
        private final String databaseName;
        private final boolean ssl;
        private final boolean postgres;

        public ConnectionDetails(String jdbcUrl, String username, String password, String driverClassName,
                                 String host, String databaseName, boolean ssl, boolean postgres) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
            this.driverClassName = driverClassName;
            this.host = host;
            this.databaseName = databaseName;
            this.ssl = ssl;
            this.postgres = postgres;
        }

        public String getJdbcUrl() { return jdbcUrl; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getDriverClassName() { return driverClassName; }
        public String getHost() { return host; }
        public String getDatabaseName() { return databaseName; }
        public boolean isSsl() { return ssl; }
        public boolean isPostgres() { return postgres; }
    }
}
