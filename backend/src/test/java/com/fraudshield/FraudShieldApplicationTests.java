package com.fraudshield;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic context-load test — verifies the Spring application context starts
 * without errors (checks bean wiring, config validation, etc.).
 *
 * <p>Uses {@code test} properties to avoid requiring live DB connections in CI.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.neo4j.uri=bolt://localhost:7687",
        "spring.neo4j.authentication.username=neo4j",
        "spring.neo4j.authentication.password=sentinelpassword",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "groq.api.key=test-key-not-real",
        "groq.api.url=https://api.groq.com/openai/v1"
})
class FraudShieldApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, the Spring context wires up correctly
    }
}
