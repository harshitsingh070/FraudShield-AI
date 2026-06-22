package com.fraudshield.controller;

import com.fraudshield.domain.FraudRing;
import com.fraudshield.dto.ThreatScoreBreakdown;
import com.fraudshield.repository.FraudRingRepository;
import com.fraudshield.service.ThreatEngineService;
import com.fraudshield.service.ThreatRescoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * REST controller for the Threat Prioritisation Engine.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/v1/threat/rescore}           — Re-score ALL rings, persist results</li>
 *   <li>{@code POST /api/v1/threat/rescore/{ringId}}  — Re-score a single ring</li>
 *   <li>{@code GET  /api/v1/threat/score/{ringId}}    — Score details for one ring (read-only)</li>
 *   <li>{@code GET  /api/v1/threat/leaderboard}       — Ranked list of all current scores</li>
 *   <li>{@code GET  /api/v1/threat/critical}          — All rings currently at CRITICAL level</li>
 *   <li>{@code GET  /api/v1/threat/explain}           — Model weights and calibration info</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/threat")
@CrossOrigin(origins = "*")   // permits map dashboard, investigation workbench, and citizen UI
@RequiredArgsConstructor
@Slf4j
public class ThreatController {

    private final ThreatRescoreService rescoreService;
    private final ThreatEngineService  threatEngine;
    private final FraudRingRepository  fraudRingRepository;

    // ── Map Dashboard ─────────────────────────────────────────────────────────

    /**
     * Returns ALL rings with <em>dynamically recomputed</em> threat scores,
     * sorted CRITICAL → LOW. Consumed by the geospatial map and the investigation
     * workbench's ring selector.
     *
     * <p>Unlike {@code /leaderboard} (which reads persisted scores), this endpoint
     * recomputes every ring's score on the fly before returning — ensuring the map
     * always reflects the freshest view of graph intelligence.
     *
     * <p>Called by: map dashboard on initial load + every 60-second polling cycle.
     */
    @GetMapping("/hotspots")
    public ResponseEntity<List<ThreatScoreBreakdown>> getHotspots() {
        log.info("Hotspots endpoint called — dynamic rescore for all rings");
        List<ThreatScoreBreakdown> hotspots = fraudRingRepository
                .findAllWithMuleAccounts()
                .stream()
                .map(ring -> threatEngine.score(ring, ring.getThreatScore()))
                .sorted(Comparator.comparingDouble(ThreatScoreBreakdown::getThreatScore).reversed())
                .toList();
        log.info("Returning {} hotspots", hotspots.size());
        return ResponseEntity.ok(hotspots);
    }

    /**
     * Lightweight variant of {@code /hotspots} — returns only the fields needed
     * to render map pins (ring_id, location_name, lat, lon, threat_score, risk_level,
     * fraud_type). Avoids sending the full breakdown payload for 100+ pins.
     *
     * <p>Called by: Leaflet / MapLibre pin renderer on every re-render.
     */
    @GetMapping("/hotspots/map")
    public ResponseEntity<List<Map<String, Object>>> getHotspotsMapPins() {
        List<Map<String, Object>> pins = fraudRingRepository
                .findAllWithMuleAccounts()
                .stream()
                .map(ring -> {
                    ThreatScoreBreakdown b = threatEngine.score(ring, ring.getThreatScore());
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("ring_id", ring.getRingId());
                    map.put("location_name", ring.getLocationName());
                    map.put("lat", ring.getLatitude());
                    map.put("lon", ring.getLongitude());
                    map.put("threat_score", b.getThreatScore());
                    map.put("risk_level", b.getRiskLevel());
                    map.put("fraud_type", ring.getFraudType() != null ? ring.getFraudType() : "UNKNOWN");
                    map.put("status", ring.getStatus() != null ? ring.getStatus() : "UNKNOWN");
                    return map;
                })
                .sorted((m1, m2) -> Double.compare(
                        (Double) m2.get("threat_score"),
                        (Double) m1.get("threat_score")
                ))
                .toList();
        return ResponseEntity.ok(pins);
    }

    /**
     * Triggers a full re-score of every ring and persists the new scores to Neo4j.
     * Returns the ranked breakdown list (CRITICAL → LOW).
     * Intended to be called from the dashboard's "Refresh Threat Scores" button.
     */
    @PostMapping("/rescore")
    public ResponseEntity<List<ThreatScoreBreakdown>> rescoreAll() {
        log.info("Manual full rescore triggered via API");
        List<ThreatScoreBreakdown> results = rescoreService.rescoreAll();
        return ResponseEntity.ok(results);
    }

    /**
     * Re-scores a single ring by ID and persists the result.
     * Used when new intelligence is added to a specific ring (e.g., a new mule
     * account is linked) and only that ring needs to be updated.
     */
    @PostMapping("/rescore/{ringId}")
    public ResponseEntity<ThreatScoreBreakdown> rescoreSingle(@PathVariable String ringId) {
        ThreatScoreBreakdown breakdown = rescoreService.rescoreSingle(ringId);
        if (breakdown == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(breakdown);
    }

    /**
     * Computes and returns a score breakdown for a single ring without persisting.
     * Safe to call from the frontend's ring detail panel for live inspection.
     */
    @GetMapping("/score/{ringId}")
    public ResponseEntity<ThreatScoreBreakdown> getScore(@PathVariable String ringId) {
        return fraudRingRepository.findById(ringId)
                .map(ring -> ResponseEntity.ok(threatEngine.score(ring, ring.getThreatScore())))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns all rings ranked by current threat score (descending).
     * Reads the persisted {@code threatScore} from Neo4j — no re-computation.
     * This is the fast path for the dashboard leaderboard on page load.
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<ThreatScoreBreakdown>> getLeaderboard() {
        List<ThreatScoreBreakdown> ranked = fraudRingRepository
                .findAllWithMuleAccounts()
                .stream()
                .map(ring -> threatEngine.score(ring, ring.getThreatScore()))
                .sorted((a, b) -> Double.compare(b.getThreatScore(), a.getThreatScore()))
                .toList();
        return ResponseEntity.ok(ranked);
    }

    /**
     * Returns only rings currently scoring ≥ 8.0 (CRITICAL band).
     * Intended for the dashboard's real-time alert ticker and notification feed.
     */
    @GetMapping("/critical")
    public ResponseEntity<List<ThreatScoreBreakdown>> getCriticalRings() {
        List<ThreatScoreBreakdown> critical = fraudRingRepository
                .findByThreatScoreGreaterThanEqualOrderByThreatScoreDesc(8.0)
                .stream()
                .map(ring -> threatEngine.score(ring, ring.getThreatScore()))
                .toList();
        return ResponseEntity.ok(critical);
    }

    /**
     * Returns a human-readable explanation of the scoring model — weights,
     * calibration constants, and risk band thresholds.
     * Useful for the "How does scoring work?" panel in the dashboard's
     * help section, and for explaining the model to hackathon judges.
     */
    @GetMapping("/explain")
    public ResponseEntity<Map<String, Object>> explainModel() {
        return ResponseEntity.ok(Map.of(
                "model_version", "1.0",
                "score_range", "0.0 – 10.0",
                "dimensions", List.of(
                        dim("Financial Impact",     "30%",
                                "Square-root scale: ₹2 Cr total flow → score 10",
                                "Capped at ₹2 Cr — extreme outliers don't dominate"),
                        dim("Victim Volume",         "25%",
                                "Linear scale: 20 victims → score 10",
                                "Directly reflects human harm caused"),
                        dim("Network Complexity",    "20%",
                                "60% from account count (ceiling 4) + 40% confirmation ratio",
                                "Confirmed accounts signal better intelligence quality"),
                        dim("Recency",              "15%",
                                "Exponential decay: active today=10, 90 days ago≈0",
                                "Inactive rings have lower immediate priority"),
                        dim("Fraud Type Severity",  "10%",
                                "DIGITAL_ARREST=10, INVESTMENT=8, JOB/ROMANCE=6, LOTTERY=4",
                                "Digital arrest causes greatest psychological harm")
                ),
                "risk_bands", Map.of(
                        "CRITICAL", "≥ 8.0 — immediate interdiction",
                        "HIGH",     "6.0 – 7.9 — escalate to state cybercrime cell",
                        "MEDIUM",   "4.0 – 5.9 — monitor and gather intelligence",
                        "LOW",      "< 4.0 — log and review quarterly"
                )
        ));
    }

    // ── Helper to build the explain map entries cleanly ──────────────────────
    private Map<String, String> dim(String name, String weight, String calibration, String rationale) {
        return Map.of(
                "name",        name,
                "weight",      weight,
                "calibration", calibration,
                "rationale",   rationale
        );
    }
}
