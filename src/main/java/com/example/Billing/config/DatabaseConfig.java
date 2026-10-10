package com.example.Billing.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource(Environment environment) {
        String databaseUrl = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("spring.datasource.url")
        );
        String username = firstNonBlank(
                environment.getProperty("DATABASE_USERNAME"),
                environment.getProperty("spring.datasource.username")
        );
        String password = firstNonBlank(
                environment.getProperty("DATABASE_PASSWORD"),
                environment.getProperty("spring.datasource.password")
        );

        DatabaseConnection connection = resolve(databaseUrl, username, password);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(connection.jdbcUrl());
        dataSource.setUsername(connection.username());
        dataSource.setPassword(connection.password());
        dataSource.setDriverClassName(connection.jdbcUrl().startsWith("jdbc:postgresql:")
                ? "org.postgresql.Driver"
                : environment.getProperty("spring.datasource.driver-class-name"));
        dataSource.setMaximumPoolSize(environment.getProperty("DB_POOL_SIZE", Integer.class, 5));
        dataSource.setMinimumIdle(environment.getProperty("DB_POOL_MIN_IDLE", Integer.class, 0));
        dataSource.setConnectionTimeout(environment.getProperty("DB_CONNECTION_TIMEOUT_MS", Long.class, 30000L));
        dataSource.setIdleTimeout(environment.getProperty("DB_IDLE_TIMEOUT_MS", Long.class, 300000L));
        dataSource.setMaxLifetime(environment.getProperty("DB_MAX_LIFETIME_MS", Long.class, 600000L));
        return dataSource;
    }

    static DatabaseConnection resolve(String databaseUrl, String configuredUsername, String configuredPassword) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "DATABASE_URL is required. Use the pooled Neon connection string or a JDBC PostgreSQL URL."
            );
        }

        if (databaseUrl.startsWith("jdbc:")) {
            return new DatabaseConnection(databaseUrl, nullToEmpty(configuredUsername), nullToEmpty(configuredPassword));
        }

        if (!databaseUrl.startsWith("postgresql://") && !databaseUrl.startsWith("postgres://")) {
            throw new IllegalStateException(
                    "DATABASE_URL must start with postgresql://, postgres://, or jdbc:postgresql://."
            );
        }

        URI uri;
        try {
            uri = URI.create(databaseUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("DATABASE_URL is not a valid PostgreSQL connection string.", exception);
        }

        if (uri.getHost() == null || uri.getRawPath() == null || uri.getRawPath().length() < 2) {
            throw new IllegalStateException("DATABASE_URL must include a database host and name.");
        }

        String embeddedUsername = null;
        String embeddedPassword = null;
        String rawUserInfo = uri.getRawUserInfo();
        if (rawUserInfo != null) {
            String[] credentials = rawUserInfo.split(":", 2);
            embeddedUsername = decode(credentials[0]);
            if (credentials.length == 2) {
                embeddedPassword = decode(credentials[1]);
            }
        }

        String username = firstNonBlank(configuredUsername, embeddedUsername);
        String password = firstNonBlank(configuredPassword, embeddedPassword);
        if (username == null || password == null) {
            throw new IllegalStateException(
                    "Database credentials are missing. Include them in DATABASE_URL or set DATABASE_USERNAME and DATABASE_PASSWORD."
            );
        }

        String host = uri.getHost();
        if (host.contains(":")) {
            host = "[" + host + "]";
        }
        String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        String jdbcUrl = "jdbc:postgresql://" + host + port + uri.getRawPath() + query;

        return new DatabaseConnection(jdbcUrl, username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second != null && !second.isBlank() ? second : null;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    record DatabaseConnection(String jdbcUrl, String username, String password) {
    }
}
