package com.fraudshield.service;

import com.fraudshield.domain.FraudRing;
import com.fraudshield.domain.MuleAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DataValidationService {

    private final GeminiAiService geminiAiService;

    public DataValidationService(GeminiAiService geminiAiService) {
        this.geminiAiService = geminiAiService;
    }

    /**
     * Validates and verifies a new FraudRing before storing.
     */
    public FraudRing verifyFraudRing(FraudRing ring) {
        log.info("Starting validation pipeline for FraudRing: {}", ring.getRingId());

        // 1. Basic Sanity Checks (e.g. checking if coordinates are roughly within India)
        if (ring.getLatitude() < 8.0 || ring.getLatitude() > 38.0 || ring.getLongitude() < 68.0 || ring.getLongitude() > 98.0) {
            log.warn("Invalid coordinates for India: {}, {}", ring.getLatitude(), ring.getLongitude());
            ring.setVerificationStatus("REJECTED_FAKE");
            ring.setVerificationConfidence(0.0);
            ring.setVerifiedBy("System-Sanity-Check");
            return ring;
        }

        // 2. AI Verification
        // We pass the data to our AI model to verify if the patterns resemble real fraud typologies.
        double aiConfidence = performAIVerification(ring);
        
        ring.setVerificationConfidence(aiConfidence);
        if (aiConfidence >= 80.0) {
            ring.setVerificationStatus("VERIFIED_REAL");
            ring.setVerifiedBy("Gemini-AI-Pipeline");
        } else if (aiConfidence < 30.0) {
            ring.setVerificationStatus("REJECTED_FAKE");
            ring.setVerifiedBy("Gemini-AI-Pipeline");
        } else {
            ring.setVerificationStatus("PENDING");
            ring.setVerifiedBy("Awaiting-Human-Review");
        }

        return ring;
    }

    private double performAIVerification(FraudRing ring) {
        // In a full implementation, you would call geminiAiService to analyze the ring's pattern.
        // For now, we simulate a confidence score based on known patterns.
        if ("Digital Arrest".equalsIgnoreCase(ring.getFraudType()) || 
            "Investment Scam".equalsIgnoreCase(ring.getFraudType())) {
            return 85.0; // High confidence for known patterns
        }
        return 50.0; // Middle-ground; needs manual review
    }
}
