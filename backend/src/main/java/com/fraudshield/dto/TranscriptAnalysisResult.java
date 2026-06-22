package com.fraudshield.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Fully typed result of a Gemini call-transcript analysis.
 *
 * <p>Produced by {@link com.fraudshield.service.GeminiAiService#analyzeIncidentTranscript}
 * and returned directly to the React audio pipeline result panel.
 *
 * <p>Schema mirrors the JSON contract we instruct Gemini to follow.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptAnalysisResult {

    /**
     * Scam category: DIGITAL_ARREST | INVESTMENT_FRAUD | LOTTERY_FRAUD |
     * JOB_FRAUD | ROMANCE_FRAUD | LEGITIMATE | UNKNOWN
     */
    @JsonProperty("scam_type")
    private String scamType;

    /** Classification confidence 0–100 */
    @JsonProperty("confidence")
    private int confidence;

    /** CRITICAL | HIGH | MEDIUM | LOW */
    @JsonProperty("risk_level")
    private String riskLevel;

    /** Exact verbatim phrases from the transcript that triggered classification */
    @JsonProperty("trigger_phrases")
    private List<String> triggerPhrases;

    /** Step-by-step recommended actions for the citizen */
    @JsonProperty("suggested_action")
    private String suggestedAction;

    /** Ordered citizen-facing recommended actions list */
    @JsonProperty("recommended_actions")
    private List<String> recommendedActions;

    /** Whether classic authority-impersonation language was detected */
    @JsonProperty("authority_impersonation_detected")
    private boolean authorityImpersonationDetected;

    /** Whether financial pressure / urgency language was present */
    @JsonProperty("financial_pressure_detected")
    private boolean financialPressureDetected;

    /** 1–2 sentence plain-language explanation for the citizen UI */
    @JsonProperty("explanation")
    private String explanation;

    /** ISO-8601 timestamp at which this analysis was run */
    @JsonProperty("analyzed_at")
    private String analyzedAt;

    /**
     * True if Gemini's raw response was unparseable and this object
     * was constructed from a safe fallback.
     */
    @JsonProperty("is_fallback")
    private boolean isFallback;
}
