package com.fraudshield.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI-generated intelligence narrative for a single {@link com.fraudshield.domain.FraudRing}.
 *
 * <p>Produced by {@link com.fraudshield.service.GeminiAiService#generateRingIntelligenceNarrative}
 * and displayed in the ring detail panel as a human-readable intelligence brief.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RingIntelligenceNarrative {

    @JsonProperty("ring_id")
    private String ringId;

    /**
     * Two-paragraph law-enforcement-style intelligence brief summarising
     * the ring's MO, scale, and recommended response.
     */
    @JsonProperty("intelligence_brief")
    private String intelligenceBrief;

    /** Probable geographic hub of the operation */
    @JsonProperty("probable_hub")
    private String probableHub;

    /** Primary tactic used by this ring (e.g., "video-call coercion") */
    @JsonProperty("primary_tactic")
    private String primaryTactic;

    /** Likely victim profile (e.g., "urban professionals aged 35–55") */
    @JsonProperty("victim_profile")
    private String victimProfile;

    /** 3–5 recommended law-enforcement intervention steps */
    @JsonProperty("intervention_steps")
    private List<String> interventionSteps;

    /** Estimated time-to-interdiction if action is taken now */
    @JsonProperty("estimated_interdiction_window")
    private String estimatedInterdictionWindow;

    /** Overall threat assessment sentence (for the dashboard alert card) */
    @JsonProperty("threat_assessment")
    private String threatAssessment;

    @JsonProperty("generated_at")
    private String generatedAt;

    @JsonProperty("is_fallback")
    private boolean isFallback;
}
