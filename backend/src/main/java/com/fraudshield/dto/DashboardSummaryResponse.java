package com.fraudshield.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Dashboard summary DTO — a single response object that the React executive
 * dashboard fetches once on load. Contains all four metric card values plus
 * the data for the alerts table and geospatial map.
 */
@Data
@Builder
public class DashboardSummaryResponse {

    // ── Metric cards (top row) ────────────────────────────────────────────────

    /** Total complaints analysed today (from PostgreSQL). */
    @JsonProperty("complaints_today")
    private long complaintsToday;

    /** Count of PhoneNumber/FraudRing nodes with risk_level HIGH or CRITICAL. */
    @JsonProperty("high_risk_numbers")
    private long highRiskNumbers;

    /** Count of ACTIVE FraudRing nodes. */
    @JsonProperty("active_fraud_rings")
    private long activeFraudRings;

    /**
     * Estimated total financial impact in INR (sum of totalMoneyLaundered
     * across all FraudRings), formatted as a human-readable string.
     */
    @JsonProperty("estimated_impact_inr")
    private String estimatedImpactInr;

    /** Raw impact figure in INR for chart calculations. */
    @JsonProperty("estimated_impact_raw")
    private double estimatedImpactRaw;

    // ── Alert leaderboard (top 5 highest-threat rings) ───────────────────────

    @JsonProperty("top_threat_rings")
    private List<FraudRingSummary> topThreatRings;

    // ── Geospatial map pin data ───────────────────────────────────────────────

    @JsonProperty("map_pins")
    private List<MapPin> mapPins;

    // ── Nested summary types ──────────────────────────────────────────────────

    @Data
    @Builder
    public static class FraudRingSummary {
        @JsonProperty("ring_id")       private String ringId;
        @JsonProperty("location_name") private String locationName;
        @JsonProperty("fraud_type")    private String fraudType;
        @JsonProperty("threat_score")  private double threatScore;
        @JsonProperty("status")        private String status;
        @JsonProperty("victim_count")  private int victimCount;
        @JsonProperty("money_flow_inr")private String moneyFlowInr;
        @JsonProperty("last_active")   private String lastActive;
    }

    @Data
    @Builder
    public static class MapPin {
        @JsonProperty("ring_id")       private String ringId;
        @JsonProperty("location_name") private String locationName;
        @JsonProperty("lat")           private double lat;
        @JsonProperty("lon")           private double lon;
        @JsonProperty("threat_score")  private double threatScore;
        @JsonProperty("fraud_type")    private String fraudType;
        @JsonProperty("status")        private String status;
    }
}
