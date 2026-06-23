package com.fraudshield.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * JPA/PostgreSQL configuration for the Complaint relational store.
 *
 * <p>Because this application uses both Spring Data JPA (PostgreSQL / Neon) and
 * Spring Data Neo4j in the same context, Spring Boot's JPA auto-configuration is
 * blocked from creating the default {@code transactionManager} bean by the
 * {@code neo4jTransactionManager} that Neo4jConfig registers first.
 *
 * <p>We therefore define the JPA transaction manager explicitly here and mark it
 * {@code @Primary} so that any {@code @Transactional} annotation without an explicit
 * qualifier resolves to the JPA manager (the safe default for PostgreSQL-backed code).
 * Neo4j repositories are configured to use {@code neo4jTransactionManager} in
 * {@link Neo4jConfig}.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.fraudshield.repository",
        transactionManagerRef = "transactionManager",
        entityManagerFactoryRef = "entityManagerFactory"
)
public class JpaConfig {

    /**
     * Explicit JPA transaction manager named {@code transactionManager}.
     *
     * <p>Without this bean, Spring Boot's JPA auto-configuration is suppressed by the
     * presence of {@code neo4jTransactionManager}, causing the startup failure:
     * <em>"A component required a bean named 'transactionManager' that could not be found."</em>
     *
     * <p>Marked {@code @Primary} so {@code @Transactional} defaults to JPA in
     * services that touch both databases.
     */
    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
