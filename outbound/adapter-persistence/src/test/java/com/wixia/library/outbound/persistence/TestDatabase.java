package com.wixia.library.outbound.persistence;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;

import org.flywaydb.core.Flyway;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A PostgreSQL in a container, shared by all tests of the module, with the schema created by the Flyway migrations
 * the application uses. The container is removed by Testcontainers when the JVM exits.
 */
final class TestDatabase {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    private static EntityManagerFactory entityManagerFactory;

    private TestDatabase() {
    }

    static synchronized EntityManagerFactory entityManagerFactory() {
        if (entityManagerFactory == null) {
            POSTGRES.start();
            Flyway.configure()
                    .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                    .load()
                    .migrate();
            entityManagerFactory = new PersistenceConfiguration("library")
                    .managedClass(MemberEntity.class)
                    .managedClass(BookCopyEntity.class)
                    .managedClass(LoanEntity.class)
                    .property(PersistenceConfiguration.JDBC_URL, POSTGRES.getJdbcUrl())
                    .property(PersistenceConfiguration.JDBC_USER, POSTGRES.getUsername())
                    .property(PersistenceConfiguration.JDBC_PASSWORD, POSTGRES.getPassword())
                    // Fails if the entities don't match the tables the migrations created.
                    .property("hibernate.hbm2ddl.auto", "validate")
                    .createEntityManagerFactory();
        }
        return entityManagerFactory;
    }
}
