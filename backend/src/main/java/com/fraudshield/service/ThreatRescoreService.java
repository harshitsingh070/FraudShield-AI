package com.fraudshield.service;

import com.fraudshield.domain.FraudRing;
import com.fraudshield.dto.ThreatScoreBreakdown;
import com.fraudshield.repository.FraudRingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch threat re-scorer.
 *
 * <p>Iterates every {@link FraudRing} in Neo4j, calls {@link ThreatEngineService}
 * to compute an updated score, writes the new score and derived risk metadata
 * back to the node, and returns a ranked report of all breakdowns.
 *
 * <p>This service is invoked:
 * <ul>
 *   <li>Via {@code POST /api/v1/threat/rescore} (manual trigger from dashboard)</li>
 *   <li>After the {@link com.fraudshield.config.DataSeeder} finishes on startup</li>
 *   <li>Whenever new mule accounts are linked to a ring</li>
 * </ul>
 *
 * <p>Each ring's previous {@code threatScore} is captured before the update and
 * passed to the engine so the breakdown DTO's {@code score_delta} field is
 * populated correctly. This lets the dashboard highlight rings whose threat
 * level has recently escalated.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThreatRescoreService {

    private final FraudRingRepository fraudRingRepository;
    private final ThreatEngineService threatEngine;

    /**
     * Rescores every ring, persists updated scores, and returns ranked results.
     *
     * @return list of {@link ThreatScoreBreakdown} sorted by threat score descending
     */
    public List<ThreatScoreBreakdown> rescoreAll() {
        List<FraudRing> rings = fraudRingRepository.findAll();

        log.info("━━━ Threat re-score started — {} rings ━━━", rings.size());

        List<ThreatScoreBreakdown> results = new ArrayList<>();
        int escalated = 0;

        for (FraudRing ring : rings) {
            double previousScore = ring.getThreatScore();

            // Compute new score with full breakdown
            ThreatScoreBreakdown breakdown = threatEngine.score(ring, previousScore);

            // Write new score and derived status back to the Neo4j node
            ring.setThreatScore(breakdown.getThreatScore());
            ring.setTotalMoneyLaundered(breakdown.getTotalMoneyFlowInr());
            ring.setTotalVictimCount(breakdown.getTotalVictims());

            // Auto-escalate status based on score (DORMANT rings that spike → ACTIVE)
            if (breakdown.getThreatScore() >= 6.0 && "DORMANT".equals(ring.getStatus())) {
                ring.setStatus("ACTIVE");
                log.warn("⚠ Ring {} auto-escalated DORMANT → ACTIVE (score={})",
                        ring.getRingId(), String.format("%.2f", breakdown.getThreatScore()));
            }

            fraudRingRepository.save(ring);
            results.add(breakdown);

            // Count escalations (score went up by more than 1.0)
            Double delta = breakdown.getScoreDelta();
            if (delta != null && delta > 1.0) escalated++;

            log.info("  {} {} → {} ({})  Δ={}",
                    ring.getRingId(),
                    ring.getLocationName(),
                    String.format("%.2f", breakdown.getThreatScore()),
                    breakdown.getRiskLevel(),
                    delta != null ? String.format("%+.2f", delta) : "n/a");
        }

        // Sort by threat score descending — CRITICAL threats first
        results.sort((a, b) -> Double.compare(b.getThreatScore(), a.getThreatScore()));

        log.info("━━━ Re-score complete — {} rings updated, {} escalated ━━━",
                results.size(), escalated);

        return results;
    }

    /**
     * Rescores a single ring by ID and persists the result.
     *
     * @param ringId the ring's business key
     * @return the full breakdown, or null if the ring was not found
     */
    public ThreatScoreBreakdown rescoreSingle(String ringId) {
        return fraudRingRepository.findById(ringId).map(ring -> {
            double previous = ring.getThreatScore();
            ThreatScoreBreakdown breakdown = threatEngine.score(ring, previous);
            ring.setThreatScore(breakdown.getThreatScore());
            fraudRingRepository.save(ring);
            log.info("Rescored {} → {}", ringId, String.format("%.2f", breakdown.getThreatScore()));
            return breakdown;
        }).orElse(null);
    }
}
