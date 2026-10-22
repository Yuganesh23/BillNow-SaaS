package com.example.Billing.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The versioned migrations only upgrade a schema that Hibernate originally
 * created, so they cannot run on an empty database. When the database has no
 * tables, this creates the full schema from db/bootstrap/schema.sql and records
 * it as baseline {@value #BOOTSTRAP_VERSION}; later migrations then apply normally.
 * Databases that already have tables keep the existing behaviour.
 */
@Slf4j
@org.springframework.context.annotation.Configuration
public class FlywayBootstrapConfig {
    static final String BOOTSTRAP_VERSION = "11";
    static final String BOOTSTRAP_MARKER = "billnow-bootstrap-v11";
    private static final String SCHEMA_SCRIPT = "db/bootstrap/schema.sql";
    // Serialises bootstrap when several instances start against the same empty database.
    private static final long LOCK_KEY = 4_820_119_011L;

    @Bean
    FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            bootstrapIfEmpty(flyway);
            flyway.migrate();
        };
    }

    static void bootstrapIfEmpty(Flyway flyway) {
        Configuration configuration = flyway.getConfiguration();
        DataSource dataSource = configuration.getDataSource();
        String historyTable = configuration.getTable();
        try (Connection lock = dataSource.getConnection()) {
            lock.setAutoCommit(false);
            try (Statement statement = lock.createStatement()) {
                statement.execute("SELECT pg_advisory_xact_lock(" + LOCK_KEY + ")");
            }
            try {
                boolean hasHistory = tableExists(lock, historyTable);
                if (!hasHistory && userTableCount(lock, historyTable) == 0) {
                    log.info("Empty database: creating schema from {} and baselining at V{}", SCHEMA_SCRIPT, BOOTSTRAP_VERSION);
                    try (Connection schema = dataSource.getConnection()) {
                        schema.setAutoCommit(false);
                        ScriptUtils.executeSqlScript(schema, new ClassPathResource(SCHEMA_SCRIPT));
                        schema.commit();
                    }
                    baseline(configuration);
                } else if (!hasHistory && isBootstrapSchema(lock)) {
                    // A previous start created the schema but stopped before baselining.
                    log.warn("Bootstrap schema found without Flyway history; baselining at V{}", BOOTSTRAP_VERSION);
                    baseline(configuration);
                }
            } finally {
                lock.commit();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to prepare the database for migrations", exception);
        }
    }

    private static void baseline(Configuration configuration) {
        Flyway.configure()
                .configuration(configuration)
                .baselineVersion(BOOTSTRAP_VERSION)
                .baselineDescription("Bootstrap schema")
                .load()
                .baseline();
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?")) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1) > 0;
            }
        }
    }

    private static long userTableCount(Connection connection, String historyTable) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = current_schema() "
                        + "AND table_type = 'BASE TABLE' AND table_name <> ?")) {
            statement.setString(1, historyTable);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private static boolean isBootstrapSchema(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT obj_description(to_regclass('shops'), 'pg_class')");
             ResultSet result = statement.executeQuery()) {
            return result.next() && BOOTSTRAP_MARKER.equals(result.getString(1));
        }
    }
}
