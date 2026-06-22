package com.fraudshield.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO that carries the structured fraud analysis result from Gemini back to
 * the REST controller and ultimately to the React frontend.
 *
 * <p>All fields mirror the JSON schema we instruct Gemini to produce.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAnalysisResponse {

    /** Scam category determined by Gemini. */
    @JsonProperty("scam_type")
    private String scamType;

    /** Confidence score 0–100 as a percentage. */
    @JsonProperty("confidence")
    private Double confidence;

    /** Human-readable risk label: LOW | MEDIUM | HIGH | CRITICAL */
    @JsonProperty("risk_level")
    private String riskLevel;

    /** Exact phrases from the description that triggered the classification. */
    @JsonProperty("trigger_phrases")
    private List<String> triggerPhrases;

    /** Ordered list of recommended actions for the citizen. */
    @JsonProperty("recommended_actions")
    private List<String> recommendedActions;

    /** Brief explanation suitable for the citizen-facing UI. */
    @JsonProperty("explanation")
    private String explanation;

    /** Whether this phone number was found in the Neo4j fraud network. */
    @JsonProperty("in_fraud_network")
    private Boolean inFraudNetwork;

    /** Count of previous victims linked to the same network (if found). */
    @JsonProperty("network_victim_count")
    private Integer networkVictimCount;

    /** Database ID of the persisted complaint record. */
    @JsonProperty("complaint_id")
    private Long complaintId;
}
