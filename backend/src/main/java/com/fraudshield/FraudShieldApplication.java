package com.fraudshield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FraudShield AI – Spring Boot entry point.
 *
 * <p>Launches the Digital Public Safety Intelligence platform backend which provides:
 * <ul>
 *   <li>Fraud Shield text classification (via Google Gemini API)</li>
 *   <li>Audio scam detection pipeline (Whisper + Gemini)</li>
 *   <li>Fraud Network Graph Intelligence (Neo4j)</li>
 *   <li>Executive Dashboard API endpoints</li>
 * </ul>
 */
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FraudShieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudShieldApplication.class, args);
    }
}
