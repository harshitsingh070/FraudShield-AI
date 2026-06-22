package com.fraudshield.controller;

import com.fraudshield.dto.RingIntelligenceNarrative;
import com.fraudshield.dto.ThreatScoreBreakdown;
import com.fraudshield.dto.TranscriptAnalysisRequest;
import com.fraudshield.dto.TranscriptAnalysisResult;
import com.fraudshield.repository.FraudRingRepository;
import com.fraudshield.service.GeminiAiService;
import com.fraudshield.service.ThreatEngineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller wiring the Phase 4 Gemini AI Service to HTTP endpoints.
 *
 * <p>All three inference modes are exposed as dedicated endpoints so the
 * React frontend can call them independently:
 *
 * <ul>
 *   <li>{@code POST /api/v1/gemini/analyze-transcript}       — transcript → scam classification</li>
 *   <li>{@code GET  /api/v1/gemini/ring-intelligence/{id}}   — ring data → intelligence brief</li>
 *   <li>{@code GET  /api/v1/gemini/score-justification/{id}} — score → court-ready paragraph</li>
 *   <li>{@code GET  /api/v1/gemini/health}                   — liveness / API key sanity check</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/gemini")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class GeminiAiController {

    private final GeminiAiService    geminiAiService;
    private final FraudRingRepository fraudRingRepository;
    private final ThreatEngineService threatEngine;

    // ── 1. Transcript Analysis ────────────────────────────────────────────────

    /**
     * Analyses a call transcript and returns a typed fraud classification.
     *
     * <p>Primary consumer: the React audio pipeline result panel (Day 2 frontend).
     * Also accepts plain-text descriptions from the Fraud Shield text input.
     *
     * <p>Example request body:
     * <pre>
     * {
     *   "transcript": "Namaste, main CBI se bol raha hoon. Aapka Aadhaar
     *                  money laundering case mein use hua hai. Aapko abhi
     *                  digital arrest kiya ja raha hai..."
     * }
     * </pre>
     */
    @PostMapping("/analyze-transcript")
    public ResponseEntity<TranscriptAnalysisResult> analyzeTranscript(
            @Valid @RequestBody TranscriptAnalysisRequest request) {

        log.info("Transcript analysis endpoint called — length={}", request.getTranscript().length());
        TranscriptAnalysisResult result = geminiAiService.analyzeIncidentTranscript(request.getTranscript());
        return ResponseEntity.ok(result);
    }

    // ── 2. Ring Intelligence Narrative ────────────────────────────────────────

    /**
     * Generates a professional law-enforcement intelligence brief for a ring.
     *
     * <p>Called when a user clicks a fraud ring node in the graph visualisation
     * or a map pin in the geospatial view. The narrative is displayed in the
     * ring detail side-panel.
     */
    @GetMapping("/ring-intelligence/{ringId}")
    public ResponseEntity<RingIntelligenceNarrative> getRingIntelligence(
            @PathVariable String ringId) {

        log.info("Ring intelligence narrative requested — ringId={}", ringId);

        return fraudRingRepository.findById(ringId)
                .map(ring -> {
                    RingIntelligenceNarrative narrative =
                            geminiAiService.generateRingIntelligenceNarrative(ring);
                    return ResponseEntity.ok(narrative);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── 3. Score Justification ────────────────────────────────────────────────

    /**
     * Generates a plain-English justification paragraph for a ring's threat score.
     *
     * <p>Used in the "Export Intelligence Package" feature — the paragraph is
     * included in the downloadable PDF/text report that law enforcement can
     * attach to an FIR or court submission.
     */
    @GetMapping("/score-justification/{ringId}")
    public ResponseEntity<Map<String, String>> getScoreJustification(
            @PathVariable String ringId) {

        log.info("Score justification requested — ringId={}", ringId);

        return fraudRingRepository.findById(ringId)
                .map(ring -> {
                    ThreatScoreBreakdown breakdown = threatEngine.score(ring, ring.getThreatScore());
                    String justification = geminiAiService.generateScoreJustification(breakdown);

                    return ResponseEntity.ok(Map.of(
                            "ring_id",       ringId,
                            "threat_score",  String.valueOf(breakdown.getThreatScore()),
                            "risk_level",    breakdown.getRiskLevel(),
                            "justification", justification
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── 4. Health / Sanity Check ──────────────────────────────────────────────

    /**
     * Verifies that the Gemini API is reachable and the API key is configured.
     * Sends a minimal "hello" prompt and confirms a response arrives.
     * Used by the dashboard's "AI Status" indicator.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        log.info("Gemini health check requested");

        // Minimal probe — asks Gemini to confirm it's online
        TranscriptAnalysisResult probe = geminiAiService.analyzeIncidentTranscript(
                "Test message. Respond with LEGITIMATE classification.");

        boolean apiReachable = !probe.isFallback();

        return ResponseEntity.ok(Map.of(
                "status",        apiReachable ? "UP" : "DEGRADED",
                "gemini_reachable", apiReachable,
                "fallback_mode", probe.isFallback(),
                "component",     "gemini-ai-service",
                "model",         "gemini-2.0-flash"
        ));
    }
}
