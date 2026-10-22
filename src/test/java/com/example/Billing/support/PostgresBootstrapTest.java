package com.example.Billing.support;

import com.example.Billing.config.FlywayBootstrapAccess;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/** An empty database is initialised from the bootstrap schema and then migrates normally. */
@Testcontainers(disabledWithoutDocker = true)
class PostgresBootstrapTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void emptyDatabaseIsBootstrappedOnceAndBaselinedAtTheSnapshotVersion() {
        DataSource dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        assertBootstrap(dataSource);
    }

    static void assertBootstrap(DataSource dataSource) {
        Flyway flyway = Flyway.configure().dataSource(dataSource).baselineOnMigrate(true).baselineVersion("0").load();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        FlywayBootstrapAccess.bootstrapIfEmpty(flyway);
        flyway.migrate();
        // A second start must neither re-run the schema script nor add history rows.
        FlywayBootstrapAccess.bootstrapIfEmpty(flyway);
        flyway.migrate();

        assertThat(jdbc.queryForList("select version from flyway_schema_history where success order by installed_rank", String.class))
                .containsExactly("11");
        for (String table : new String[]{"users", "shops", "invoices", "saas_subscriptions", "saas_billing_charges"}) {
            assertThat(jdbc.queryForObject("select to_regclass(?) is not null", Boolean.class, table)).as(table).isTrue();
        }
        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_name='saas_subscriptions' and column_name='pending_plan'",
                Integer.class)).isEqualTo(1);
    }
}
