package com.fraudshield.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Full auditable breakdown of a ring's threat score — returned by the
 * {@code GET /api/v1/threat/score/{ringId}} endpoint and embedded in batch
 * re-score results.
 *
 * <p>Every component score, weight, and decision is exposed here so that:
 * <ul>
 *   <li>Law enforcement can justify prioritisation decisions in court</li>
 *   <li>Judges can evaluate the intelligence logic at the demo</li>
 *   <li>The frontend can render a breakdown bar chart</li>
 * </ul>
 *
 * <p><strong>Score components (all normalised 0 → 10):</strong>
 * <ol>
 *   <li>Financial Impact  — weight 30%</li>
 *   <li>Victim Volume     — weight 25%</li>
 *   <li>Network Complexity — weight 20% (mule account count + shared accounts)</li>
 *   <li>Recency          — weight 15% (days since last activity)</li>
 *   <li>Fraud Type Severity — weight 10% (categorical multiplier)</li>
 * </ol>
 * Final score = weighted sum, clamped to [0.0, 10.0].
 */
@Data
@Builder
public class ThreatScoreBreakdown {

    /** The ring this score belongs to */
    @JsonProperty("ring_id")
    private String ringId;

    @JsonProperty("location_name")
    private String locationName;

    // ── Final result ─────────────────────────────────────────────────────────

    /** Composite score on a 0–10 scale */
    @JsonProperty("threat_score")
    private double threatScore;

    /** Human-readable risk band */
    @JsonProperty("risk_level")
    private String riskLevel;

    /** ISO-8601 timestamp of when this score was computed */
    @JsonProperty("scored_at")
    private String scoredAt;

    // ── Component scores (each on 0–10 scale before weighting) ───────────────

    @JsonProperty("financial_impact_score")
    private double financialImpactScore;

    @JsonProperty("victim_volume_score")
    private double victimVolumeScore;

    @JsonProperty("network_complexity_score")
    private double networkComplexityScore;

    @JsonProperty("recency_score")
    private double recencyScore;

    @JsonProperty("fraud_type_severity_score")
    private double fraudTypeSeverityScore;

    // ── Raw inputs used (for auditability) ───────────────────────────────────

    @JsonProperty("total_money_flow_inr")
    private double totalMoneyFlowInr;

    @JsonProperty("total_victims")
    private int totalVictims;

    @JsonProperty("mule_account_count")
    private int muleAccountCount;

    @JsonProperty("confirmed_mule_count")
    private long confirmedMuleCount;

    @JsonProperty("days_since_last_active")
    private long daysSinceLastActive;

    @JsonProperty("fraud_type")
    private String fraudType;

    // ── Score delta from previous computation (null if first score) ───────────

    @JsonProperty("score_delta")
    private Double scoreDelta;

    /** Human-readable list of the top factors that drove the score up */
    @JsonProperty("key_drivers")
    private List<String> keyDrivers;

    /** Recommended action for law enforcement given this score */
    @JsonProperty("recommended_action")
    private String recommendedAction;
}
