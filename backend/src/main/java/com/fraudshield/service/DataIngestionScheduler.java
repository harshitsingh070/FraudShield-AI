package com.fraudshield.service;

import com.fraudshield.domain.FraudRing;
import com.fraudshield.repository.FraudRingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Scheduled job to ingest "real" data from external sources (e.g., NCRP APIs, OSINT feeds).
 * This ensures the database constantly grows with accurate data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataIngestionScheduler {

    private final DataValidationService validationService;
    private final FraudRingRepository fraudRingRepository;

    /**
     * Runs once a day to pull new records, validate them, and store them.
     * Note: @Scheduled requires @EnableScheduling on the main application class.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Runs at 2 AM every day
    public void ingestRealWorldData() {
        log.info("Starting automated ingestion of real-world intelligence feeds...");

        // In a real scenario, this would loop over an API response from a threat intel source.
        // For demonstration, we simulate fetching one new raw record.
        FraudRing rawIncomingRing = new FraudRing();
        rawIncomingRing.setRingId("RING-DL-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase());
        rawIncomingRing.setLocationName("Delhi Digital Arrest Cluster (New)");
        rawIncomingRing.setLatitude(28.6139);
        rawIncomingRing.setLongitude(77.2090);
        rawIncomingRing.setThreatScore(8.5);
        rawIncomingRing.setFraudType("Digital Arrest");
        rawIncomingRing.setStatus("ACTIVE");
        rawIncomingRing.setFirstSeenDate(Instant.now().toString());
        rawIncomingRing.setLastActiveDate(Instant.now().toString());

        // 1. Pass the raw data through our Validation/Testing Pipeline
        FraudRing verifiedRing = validationService.verifyFraudRing(rawIncomingRing);

        // 2. Only store if the AI or sanity checks didn't outright reject it
        if (!"REJECTED_FAKE".equals(verifiedRing.getVerificationStatus())) {
            fraudRingRepository.save(verifiedRing);
            log.info("Successfully ingested and stored new verified data: {}", verifiedRing.getRingId());
        } else {
            log.warn("Discarded incoming data due to failed verification: {}", rawIncomingRing.getRingId());
        }
    }
}
