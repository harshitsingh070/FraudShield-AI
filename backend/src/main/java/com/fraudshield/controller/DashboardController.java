package com.fraudshield.controller;

import com.fraudshield.domain.FraudRing;
import com.fraudshield.dto.DashboardSummaryResponse;
import com.fraudshield.repository.FraudRingRepository;
import com.fraudshield.repository.MuleAccountRepository;
import com.fraudshield.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Executive Command Centre dashboard.
 *
 * <p>All endpoints are consumed by the React dashboard page and the
 * geospatial threat map.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /api/v1/dashboard/summary}          — Full dashboard payload</li>
 *   <li>{@code GET /api/v1/dashboard/rings}            — All fraud rings list</li>
 *   <li>{@code GET /api/v1/dashboard/rings/{ringId}}   — Single ring detail</li>
 *   <li>{@code GET /api/v1/dashboard/rings/top-threat} — Leaderboard (top N)</li>
 *   <li>{@code GET /api/v1/dashboard/mules/top}        — Top mule accounts</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final FraudRingRepository fraudRingRepository;
    private final MuleAccountRepository muleAccountRepository;

    /**
     * Single endpoint that powers the entire dashboard on page load.
     * Returns metric cards + leaderboard + map pins in one request.
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        log.info("Dashboard summary requested");
        return ResponseEntity.ok(dashboardService.buildSummary());
    }

    /**
     * All fraud rings with full detail — used by the rings management table
     * and the react-force-graph full visualisation.
     */
    @GetMapping("/rings")
    public ResponseEntity<List<FraudRing>> getAllRings() {
        return ResponseEntity.ok(fraudRingRepository.findAllWithMuleAccounts());
    }

    /**
     * Single ring detail page — shown when clicking a map pin or leaderboard row.
     */
    @GetMapping("/rings/{ringId}")
    public ResponseEntity<FraudRing> getRingById(@PathVariable String ringId) {
        return fraudRingRepository.findById(ringId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Top N rings by threat score — the dashboard's alert leaderboard table.
     * Default limit = 5.
     */
    @GetMapping("/rings/top-threat")
    public ResponseEntity<List<FraudRing>> getTopThreatRings(
            @RequestParam(defaultValue = "5") int limit) {
        List<FraudRing> rings = fraudRingRepository
                .findByStatusOrderByThreatScoreDesc("ACTIVE")
                .stream().limit(limit).toList();
        return ResponseEntity.ok(rings);
    }

    /**
     * Rings filtered by fraud type — used by the "Filter by Type" dropdown.
     */
    @GetMapping("/rings/by-type/{fraudType}")
    public ResponseEntity<List<FraudRing>> getRingsByType(@PathVariable String fraudType) {
        return ResponseEntity.ok(fraudRingRepository.findByFraudType(fraudType));
    }

    /**
     * Top mule accounts by money flow — powers the "Financial Intelligence" table.
     */
    @GetMapping("/mules/top")
    public ResponseEntity<?> getTopMuleAccounts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(muleAccountRepository.findTopByMoneyFlow(limit));
    }

    /**
     * Mule accounts shared across multiple rings — the key graph intelligence insight.
     * Returns accounts that appear as nodes in more than one FraudRing's OPERATES_THROUGH edge.
     */
    @GetMapping("/mules/shared")
    public ResponseEntity<?> getSharedMuleAccounts() {
        return ResponseEntity.ok(muleAccountRepository.findSharedAcrossRings());
    }
}
