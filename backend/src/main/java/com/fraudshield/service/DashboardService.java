package com.fraudshield.service;

import com.fraudshield.domain.FraudRing;
import com.fraudshield.dto.DashboardSummaryResponse;
import com.fraudshield.dto.DashboardSummaryResponse.FraudRingSummary;
import com.fraudshield.dto.DashboardSummaryResponse.MapPin;
import com.fraudshield.repository.ComplaintRepository;
import com.fraudshield.repository.FraudRingRepository;
import com.fraudshield.repository.PhoneNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Service that assembles the executive dashboard summary from Neo4j graph
 * data and PostgreSQL complaint records.
 *
 * <p>All heavy queries are delegated to repositories; this service only
 * handles aggregation, formatting, and DTO assembly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final FraudRingRepository fraudRingRepository;
    private final PhoneNumberRepository phoneNumberRepository;
    private final ComplaintRepository complaintRepository;

    /**
     * Builds the complete dashboard payload in a single call.
     * React fetches this once on mount and populates every card independently.
     */
    public DashboardSummaryResponse buildSummary() {
        log.info("Building dashboard summary");

        // ── Metric cards ────────────────────────────────────────────────────
        long complaintsToday  = getComplaintsToday();
        long highRiskNumbers  = phoneNumberRepository.countHighRiskNumbers();
        long activeFraudRings = fraudRingRepository.countActiveRings();
        double rawImpact      = fraudRingRepository.sumTotalMoneyLaundered();
        String formattedImpact = formatInr(rawImpact);

        // ── Top threat leaderboard ──────────────────────────────────────────
        List<FraudRing> topRings = fraudRingRepository
                .findByStatusOrderByThreatScoreDesc("ACTIVE")
                .stream().limit(5).toList();

        List<FraudRingSummary> leaderboard = topRings.stream()
                .map(this::toRingSummary)
                .toList();

        // ── Geospatial map pins ─────────────────────────────────────────────
        List<FraudRing> allRings = fraudRingRepository.findAll().stream().toList();
        List<MapPin> mapPins = allRings.stream()
                .map(this::toMapPin)
                .toList();

        return DashboardSummaryResponse.builder()
                .complaintsToday(complaintsToday)
                .highRiskNumbers(highRiskNumbers)
                .activeFraudRings(activeFraudRings)
                .estimatedImpactInr(formattedImpact)
                .estimatedImpactRaw(rawImpact)
                .topThreatRings(leaderboard)
                .mapPins(mapPins)
                .build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private long getComplaintsToday() {
        try {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            return complaintRepository.countTodayComplaints(startOfDay);
        } catch (Exception ex) {
            log.warn("Could not count today's complaints: {}", ex.getMessage());
            return 0;
        }
    }

    private FraudRingSummary toRingSummary(FraudRing r) {
        return FraudRingSummary.builder()
                .ringId(r.getRingId())
                .locationName(r.getLocationName())
                .fraudType(r.getFraudType())
                .threatScore(r.getThreatScore())
                .status(r.getStatus())
                .victimCount(r.getTotalVictimCount())
                .moneyFlowInr(formatInr(r.getTotalMoneyLaundered()))
                .lastActive(r.getLastActiveDate())
                .build();
    }

    private MapPin toMapPin(FraudRing r) {
        return MapPin.builder()
                .ringId(r.getRingId())
                .locationName(r.getLocationName())
                .lat(r.getLatitude())
                .lon(r.getLongitude())
                .threatScore(r.getThreatScore())
                .fraudType(r.getFraudType())
                .status(r.getStatus())
                .build();
    }

    /**
     * Converts a raw INR amount to a human-readable crore/lakh string.
     * Examples: 48_900_000 → "₹4.89 Cr", 800_000 → "₹8.00 L"
     */
    private String formatInr(double amount) {
        if (amount >= 10_000_000) {
            return String.format(Locale.US, "₹%.2f Cr", amount / 10_000_000.0);
        } else if (amount >= 100_000) {
            return String.format(Locale.US, "₹%.2f L", amount / 100_000.0);
        } else {
            return String.format(Locale.US, "₹%.0f", amount);
        }
    }
}
