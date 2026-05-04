package com.lp.salesdashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * Configures the secondary H2 in-memory datasource used for write operations.
 * The primary MySQL datasource remains read-only (JPA DDL = validate).
 * H2 schema is initialised from h2-schema.sql on startup, IDs start at high
 * values (10001+ for customers, 100001+ for orders) to avoid collisions with
 * MySQL-seeded rows.
 *
 * The H2 DataSource is NOT registered as a Spring bean to avoid breaking
 * Spring Boot JPA auto-configuration (@ConditionalOnSingleCandidate(DataSource.class)).
 * Only the JdbcTemplate and DataSourceInitializer are exposed as beans.
 */
@Configuration
public class H2DataSourceConfig {

    @Value("${app.datasource.h2.url}")
    private String url;

    @Value("${app.datasource.h2.driver-class-name}")
    private String driverClassName;

    @Value("${app.datasource.h2.username}")
    private String username;

    @Value("${app.datasource.h2.password:}")
    private String password;

    private DataSource buildH2DataSource() {
        return DataSourceBuilder.create()
                .url(url)
                .driverClassName(driverClassName)
                .username(username)
                .password(password)
                .build();
    }

    /** Dedicated JdbcTemplate for H2 — injected into H2 DAOs via @Qualifier. */
    @Bean("h2JdbcTemplate")
    JdbcTemplate h2JdbcTemplate() {
        return new JdbcTemplate(buildH2DataSource());
    }

    /** Runs h2-schema.sql against the H2 datasource on application start. */
    @Bean
    DataSourceInitializer h2DataSourceInitializer() {
        var populator = new ResourceDatabasePopulator(new ClassPathResource("h2-schema.sql"));
        var initializer = new DataSourceInitializer();
        initializer.setDataSource(buildH2DataSource());
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
