package com.fraudshield.controller;

import com.fraudshield.dto.FraudAnalysisRequest;
import com.fraudshield.dto.FraudAnalysisResponse;
import com.fraudshield.service.FraudShieldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the Citizen Fraud Shield text analysis endpoint.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/v1/fraud-shield/analyze} — Analyze a text complaint</li>
 *   <li>{@code GET  /api/v1/fraud-shield/health}  — Component health check</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/fraud-shield")
@RequiredArgsConstructor
@Slf4j
public class FraudShieldController {

    private final FraudShieldService fraudShieldService;

    /**
     * Accepts a text description of a suspicious call/message and returns a
     * full AI-powered fraud risk assessment.
     *
     * <p>Example request body:
     * <pre>
     * {
     *   "description": "A man claiming to be from ED called and said my Aadhaar is linked to money laundering",
     *   "phone_number": "+919876543210",
     *   "victim_city": "Delhi"
     * }
     * </pre>
     */
    @PostMapping("/analyze")
    public ResponseEntity<FraudAnalysisResponse> analyze(
            @Valid @RequestBody FraudAnalysisRequest request) {

        log.info("Received fraud analysis request");
        FraudAnalysisResponse response = fraudShieldService.analyze(request);
        return ResponseEntity.ok(response);
    }

    /** Simple liveness check consumed by the GitHub Actions keep-alive workflow. */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"UP\",\"component\":\"fraud-shield\"}");
    }
}
