package com.example.Billing.support;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

/**
 * Runs in CI against PostgreSQL. Local machines without Docker use the H2
 * PostgreSQL-mode suite instead; Testcontainers marks this class skipped.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create"
})
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired DataSource dataSource;

    @Test
    void additiveMigrationsApplyToTheBaselineSchema() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isGreaterThanOrEqualTo(1);
        Integer applied = new JdbcTemplate(dataSource).queryForObject(
                "select count(*) from flyway_schema_history where success", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(2);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Long userId = jdbc.queryForObject("insert into users(name,email,password,role,active) values ('Perf Owner','perf@example.test','x','SHOP_OWNER',true) returning id", Long.class);
        Long shopId = jdbc.queryForObject("""
                insert into shops(name,email,state_code,registration_type,hsn_code_length,invoice_prefix,
                  credit_note_prefix,debit_note_prefix,subscription_tier,last_invoice_number,active,owner_id,version)
                values ('Perf Shop','perf-shop@example.test','27','REGULAR',4,'INV','CN','DN','TRIAL',0,true,?,0) returning id
                """, Long.class, userId);
        Long customerId = jdbc.queryForObject("insert into customers(shop_id,name,whatsapp_number,credit_balance) values (?,'Perf Customer','9000000000',0) returning id", Long.class, shopId);
        jdbc.update("update users set shop_id=? where id=?", shopId, userId);
        jdbc.update("""
                insert into invoices(shop_id,customer_id,biller_id,invoice_number,subtotal,discount_amount,total_amount,
                  taxable_amount,cgst_total,sgst_total,igst_total,cess_total,round_off,document_type,reverse_charge,
                  is_inter_state,status,whatsapp_status,created_at,financial_year,series_number)
                select ?,?,?, 'PERF-'||n,100,0,118,100,9,9,0,0,0,'TAX_INVOICE',false,false,'PAID','NOT_SENT',
                       current_timestamp - ((n % 365)||' days')::interval,'2026-27',n
                from generate_series(1,10000) n
                """, shopId, customerId, userId);
        jdbc.execute("analyze invoices");
        String plan = String.join(System.lineSeparator(), jdbc.queryForList("""
                explain (analyze, buffers, format text)
                select id,invoice_number,total_amount from invoices
                where shop_id=? and status<>'CANCELLED' and created_at>=current_timestamp-interval '365 days'
                order by created_at desc limit 25
                """, String.class, shopId));
        Instant start = Instant.now();
        jdbc.queryForList("""
                select id,invoice_number,total_amount from invoices
                where shop_id=? and status<>'CANCELLED' and created_at>=current_timestamp-interval '365 days'
                order by created_at desc limit 25
                """, shopId);
        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        System.out.println("REPORT_PERFORMANCE_10K_MS=" + elapsedMs);
        System.out.println(plan);
        assertThat(elapsedMs).isLessThan(1000);
    }
}
