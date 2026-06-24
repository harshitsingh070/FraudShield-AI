package com.fraudshield.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudshield.domain.FraudRing;
import com.fraudshield.domain.MuleAccount;
import com.fraudshield.dto.RingIntelligenceNarrative;
import com.fraudshield.dto.ThreatScoreBreakdown;
import com.fraudshield.dto.TranscriptAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <h2>Gemini AI Service — Phase 4 (Structured Inference)</h2>
 *
 * <p>Sends three categories of structured inference payloads directly to
 * <strong>Gemini 2.5 Flash</strong> using Spring's {@link RestClient} (sync HTTP,
 * no reactive overhead). No Python sidecars, no external libraries.
 *
 * <h3>Three inference modes</h3>
 * <ol>
 *   <li><b>Transcript Analysis</b> — classifies a call/audio transcript as a scam,
 *       extracts trigger phrases, detects authority impersonation and financial
 *       pressure patterns, and produces citizen-facing recommended actions.</li>
 *   <li><b>Ring Intelligence Narrative</b> — given a {@link FraudRing}'s structured
 *       data (location, fraud type, money flow, victim count), generates a
 *       professional law-enforcement intelligence brief with intervention steps.</li>
 *   <li><b>Score Justification</b> — translates a numeric {@link ThreatScoreBreakdown}
 *       into a plain-English justification paragraph suitable for a court submission
 *       or command-centre report.</li>
 * </ol>
 *
 * <h3>RestClient vs WebClient</h3>
 * <p>Spring 6 introduced {@link RestClient} as the synchronous successor to
 * {@code RestTemplate}. We use it here for simplicity — these calls run on
 * request threads and don't need back-pressure. The reactive {@code WebClient}
 * in {@link GeminiService} handles the high-frequency citizen fraud shield
 * endpoint where non-blocking IO matters more.
 *
 * <h3>Retry strategy</h3>
 * <p>Each method retries once on failure (total = 2 attempts). If both fail,
 * a safe fallback DTO is returned so the REST controller never returns 500.
 *
 * <h3>Prompt engineering principles applied</h3>
 * <ul>
 *   <li>Role priming — agent is told exactly who it is and who it serves</li>
 *   <li>Output contract — schema is embedded in the prompt; Gemini must not deviate</li>
 *   <li>India-specific context — jurisdiction, helpline numbers, agency names</li>
 *   <li>Low temperature (0.1) — deterministic, not creative</li>
 *   <li>{@code responseMimeType: application/json} — forces JSON-only output</li>
 * </ul>
 */
@Service
@Slf4j
public class GeminiAiService {

    @Value("${groq.api.url:https://api.groq.com/openai/v1}")
    private String groqUrl;

    @Value("${groq.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper;

    public GeminiAiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // =========================================================================
    // 1. TRANSCRIPT ANALYSIS
    // =========================================================================

    /**
     * Analyses a call transcript (plain text or Whisper output) and returns a
     * fully typed {@link TranscriptAnalysisResult}.
     *
     * <p>The prompt is designed to detect <em>Digital Arrest</em> scam patterns
     * specifically, as these account for the largest financial losses in India.
     * It also covers Investment, Lottery, Job, and Romance fraud.
     *
     * <p>Example usage:
     * <pre>
     * String transcript = "...Aadhaar linked to money laundering case...";
     * TranscriptAnalysisResult result = geminiAiService.analyzeIncidentTranscript(transcript);
     * </pre>
     *
     * @param transcript raw text of the suspicious call transcript
     * @return typed analysis result (never null — falls back on API error)
     */
    public TranscriptAnalysisResult analyzeIncidentTranscript(String transcript) {
        log.info("Gemini transcript analysis requested — length={} chars", transcript.length());

        String prompt = buildTranscriptPrompt(transcript);
        String raw    = callGeminiWithRetry(prompt, "transcript-analysis");

        if (raw == null) return transcriptFallback();

        try {
            return parseTranscriptResult(raw);
        } catch (Exception ex) {
            log.error("Failed to parse transcript analysis response: {}", ex.getMessage());
            log.error("Raw Gemini response was: {}", raw);
            return transcriptFallback();
        }
    }

    // =========================================================================
    // 2. RING INTELLIGENCE NARRATIVE
    // =========================================================================

    /**
     * Generates a professional intelligence brief for a {@link FraudRing}
     * from its structured graph data.
     *
     * <p>The brief is formatted as a law-enforcement report that can be
     * attached to an MHA/I4C escalation package. It includes the ring's MO,
     * victim profile, recommended interdiction steps, and estimated window.
     *
     * @param ring the FraudRing node (must have muleAccounts populated)
     * @return intelligence narrative (never null — falls back on API error)
     */
    public RingIntelligenceNarrative generateRingIntelligenceNarrative(FraudRing ring) {
        log.info("Gemini ring intelligence narrative requested — ringId={}", ring.getRingId());

        String prompt = buildRingNarrativePrompt(ring);
        String raw    = callGeminiWithRetry(prompt, "ring-narrative");

        if (raw == null) return ringNarrativeFallback(ring.getRingId());

        try {
            return parseRingNarrative(raw, ring.getRingId());
        } catch (Exception ex) {
            log.error("Failed to parse ring narrative response: {}", ex.getMessage());
            return ringNarrativeFallback(ring.getRingId());
        }
    }

    // =========================================================================
    // 3. SCORE JUSTIFICATION (AI-AUGMENTED THREAT EXPLANATION)
    // =========================================================================

    /**
     * Converts a {@link ThreatScoreBreakdown} into a plain-English justification
     * paragraph. Intended for inclusion in court-admissible intelligence packages
     * and command-centre reports.
     *
     * @param breakdown the fully computed threat score
     * @return a plain-English paragraph (empty string on failure)
     */
    public String generateScoreJustification(ThreatScoreBreakdown breakdown) {
        log.info("Gemini score justification requested — ringId={} score={}",
                breakdown.getRingId(), breakdown.getThreatScore());

        String prompt = buildScoreJustificationPrompt(breakdown);
        String raw    = callGeminiWithRetry(prompt, "score-justification");

        if (raw == null) return defaultScoreJustification(breakdown);

        try {
            JsonNode root = objectMapper.readTree(raw);
            String text = extractTextFromGeminiResponse(root);

            // The justification prompt asks for plain text wrapped in JSON
            JsonNode parsed = objectMapper.readTree(text);
            return parsed.path("justification").asText(defaultScoreJustification(breakdown));
        } catch (Exception ex) {
            log.warn("Justification parse failed, using default: {}", ex.getMessage());
            return defaultScoreJustification(breakdown);
        }
    }

    // =========================================================================
    // 4. CURRENCY IMAGE ANALYSIS
    // =========================================================================

    /**
     * Analyses an image of an Indian currency note to determine if it is counterfeit.
     *
     * @param imageBytes raw bytes of the image file
     * @param mimeType MIME type of the image (e.g., "image/jpeg")
     * @return a JSON string containing the analysis result
     */
    public String analyzeCurrencyImage(byte[] imageBytes, String mimeType) {
        log.info("Gemini currency image analysis requested — size={} bytes", imageBytes.length);

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String promptText = "You are an RBI currency authentication specialist. Analyze this Indian currency note image. Return ONLY a valid JSON object with these exact fields: denomination (string like Rs.500), authenticityScore (integer 0-100), verdict (string: AUTHENTIC or SUSPECT), securityFeatures (array of objects each with name and status fields where status is PRESENT, ABSENT, or UNCLEAR), flaggedIssues (array of strings describing any problems found). Examine: security thread, watermark, microprint, colour shift strip, serial number format, and paper texture indicators.";

        Map<String, Object> body = Map.of(
                "model", "meta-llama/llama-4-scout-17b-16e-instruct",
                "messages", List.of(
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", promptText),
                                Map.of("type", "image_url", "image_url", Map.of("url", "data:" + mimeType + ";base64," + base64Image))
                        ))
                ),
                "temperature", 0.1
        );

        String raw = callGeminiWithRetry(body, "currency-analysis");

        if (raw == null) return "{}";

        try {
            JsonNode root = objectMapper.readTree(raw);
            String text = extractTextFromGeminiResponse(root);
            return stripMarkdownFences(text);
        } catch (Exception ex) {
            log.error("Failed to parse currency analysis response: {}", ex.getMessage());
            return "{}";
        }
    }

    // =========================================================================
    // 5. AUDIO ANALYSIS
    // =========================================================================

    public String analyzeAudio(byte[] audioBytes, String mimeType) {
        log.info("Groq audio analysis requested — size={} bytes", audioBytes.length);

        try {
            // Step 1: Transcribe using Groq Whisper
            org.springframework.util.MultiValueMap<String, Object> parts = new org.springframework.util.LinkedMultiValueMap<>();
            parts.add("file", new org.springframework.core.io.ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return "audio.m4a"; 
                }
            });
            parts.add("model", "whisper-large-v3-turbo");
            parts.add("response_format", "json");

            String transcriptJson = RestClient.create()
                    .post()
                    .uri(groqUrl + "/audio/transcriptions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(String.class);

            String transcribedText = objectMapper.readTree(transcriptJson).path("text").asText();
            
            // Step 2: Analyze transcribed text
            String promptText = "You are an Indian cybercrime audio analysis specialist. The following is a transcript of an audio call. Analyze if this is a scam call targeting Indian citizens. Respond with ONLY a valid JSON object, no markdown, no explanation, using exactly these fields: transcript (string - the complete word for word transcription), isScam (boolean), scamType (string - one of: DIGITAL_ARREST, INVESTMENT_FRAUD, JOB_FRAUD, LOTTERY_FRAUD, LOAN_FRAUD, LEGITIMATE, UNKNOWN), confidence (integer 0 to 100), triggerPhrases (array of strings - exact phrases from the audio that indicate fraud), voiceAnalysis (string - one of: HUMAN_VOICE, AI_GENERATED_VOICE, UNCLEAR - default to HUMAN_VOICE since this is text), recommendation (string - one sentence action recommendation). TRANSCRIPT: " + transcribedText;

            Map<String, Object> body = Map.of(
                    "model", "llama-3.3-70b-versatile",
                    "messages", List.of(
                            Map.of("role", "user", "content", promptText)
                    ),
                    "temperature", 0.1,
                    "response_format", Map.of("type", "json_object")
            );

            String raw = callGeminiWithRetry(body, "audio-analysis");

            if (raw == null) return "{}";

            JsonNode root = objectMapper.readTree(raw);
            String text = extractTextFromGeminiResponse(root);
            return stripMarkdownFences(text);

        } catch (Exception ex) {
            log.error("Failed to parse audio analysis response: {}", ex.getMessage());
            return "{}";
        }
    }

    // =========================================================================
    // PROMPT BUILDERS
    // =========================================================================

    private String buildTranscriptPrompt(String transcript) {
        return """
                You are a Senior Analyst at India's I4C (Indian Cyber Crime Coordination Centre).
                You are analysing the transcript of a call reported as suspicious by a citizen.
                Your task is to classify this transcript, extract intelligence, and produce
                actionable guidance. You have deep expertise in Indian cybercrime patterns.
                
                SCAM PATTERNS TO DETECT:
                
                DIGITAL_ARREST (highest priority):
                - Caller impersonates CBI, ED, Customs, TRAI, Narcotics Bureau, or police
                - Claims victim's Aadhaar, phone, or bank account is linked to crime
                - Demands victim stay on a video call ("digital arrest")
                - Creates time pressure: "warrant will be issued in 2 hours"
                - Indian phrases: "aapka Aadhaar bandh ho jayega", "CBI notice"
                
                INVESTMENT_FRAUD:
                - Promises guaranteed/extraordinary returns from stocks, crypto, forex
                - References WhatsApp/Telegram "VIP investment groups"
                - Initial small profits to build trust before large theft
                
                LOTTERY_FRAUD:
                - Fake prize notification for iPhone, car, cash
                - Requires "registration fee", "customs clearance", "GST payment"
                
                JOB_FRAUD:
                - Fake IT/BPO/WFH job offer from reputed company
                - Requires "registration fee", "training fee", "equipment deposit"
                
                ROMANCE_FRAUD:
                - Long-term relationship → sudden financial emergency abroad
                
                DETECTION RULES:
                - authorityImpersonationDetected: true if any government agency is named
                - financialPressureDetected: true if payment or transfer is requested/implied
                - Base confidence on: specificity of script, urgency level, financial ask
                
                TRANSCRIPT:
                "%s"
                
                Respond ONLY with this JSON schema (no markdown fences, no extra text):
                {
                  "scam_type": "<DIGITAL_ARREST|INVESTMENT_FRAUD|LOTTERY_FRAUD|JOB_FRAUD|ROMANCE_FRAUD|LEGITIMATE|UNKNOWN>",
                  "confidence": <0-100>,
                  "risk_level": "<CRITICAL|HIGH|MEDIUM|LOW>",
                  "trigger_phrases": ["exact phrase from transcript 1", "exact phrase 2"],
                  "authority_impersonation_detected": <true|false>,
                  "financial_pressure_detected": <true|false>,
                  "suggested_action": "<one-sentence action for the citizen>",
                  "recommended_actions": [
                    "Immediately hang up and block the number",
                    "Do NOT make any payment — no real government agency demands payment over phone",
                    "File a complaint at cybercrime.gov.in within 24 hours",
                    "Call the national cybercrime helpline: 1930"
                  ],
                  "explanation": "<1-2 sentences in simple language explaining why this is/isn't a scam>"
                }
                """.formatted(transcript);
    }

    private String buildRingNarrativePrompt(FraudRing ring) {
        // Build a structured data block from the ring's graph properties
        int totalVictims = ring.getMuleAccounts() == null ? ring.getTotalVictimCount()
                : ring.getMuleAccounts().stream().mapToInt(MuleAccount::getVictimCount).sum();
        double totalFunds = ring.getMuleAccounts() == null ? ring.getTotalMoneyLaundered()
                : ring.getMuleAccounts().stream().mapToDouble(MuleAccount::getTotalMoneyFlow).sum();
        int accountCount = ring.getMuleAccounts() == null ? 0 : ring.getMuleAccounts().size();

        String dataBlock = """
                Ring ID       : %s
                Location      : %s (%.4f, %.4f)
                Fraud Type    : %s
                Status        : %s
                Threat Score  : %.1f / 10.0
                Mule Accounts : %d confirmed bank accounts
                Total Victims : %d
                Money Laundered: ₹%.2f Cr
                First Seen    : %s
                Last Active   : %s
                """.formatted(
                ring.getRingId(), ring.getLocationName(), ring.getLatitude(), ring.getLongitude(),
                ring.getFraudType(), ring.getStatus(), ring.getThreatScore(),
                accountCount, totalVictims, totalFunds / 10_000_000.0,
                ring.getFirstSeenDate(), ring.getLastActiveDate()
        );

        return """
                You are a Senior Intelligence Analyst at the National Cyber Forensic Laboratory (NCFL), India.
                Based on the structured threat data below, generate a professional intelligence brief
                for this fraud ring. The brief will be used by the Special Task Force and MHA for
                operational decision-making. Use professional law-enforcement language.
                
                FRAUD RING DATA:
                %s
                
                Respond ONLY with this JSON schema (no markdown, no extra text):
                {
                  "intelligence_brief": "<2-paragraph professional intelligence brief>",
                  "probable_hub": "<city/district most likely operating centre>",
                  "primary_tactic": "<2-5 word description of main method>",
                  "victim_profile": "<demographic likely targeted by this ring>",
                  "intervention_steps": [
                    "<step 1: immediate action>",
                    "<step 2>",
                    "<step 3>",
                    "<step 4: long-term>",
                    "<step 5: preventive>"
                  ],
                  "estimated_interdiction_window": "<e.g. 48-72 hours if action taken now>",
                  "threat_assessment": "<1 sentence for the alert card>"
                }
                """.formatted(dataBlock);
    }

    private String buildScoreJustificationPrompt(ThreatScoreBreakdown b) {
        return """
                You are an expert witness preparing a technical justification for an AI-computed
                threat score that will be included in a court-admissible cybercrime intelligence
                package in India.
                
                SCORE DATA:
                Ring ID              : %s
                Location             : %s
                Final Score          : %.2f / 10.0
                Risk Level           : %s
                Financial Impact (30%%): %.1f / 10
                Victim Volume (25%%):   %.1f / 10
                Network Complexity (20%%): %.1f / 10
                Recency (15%%):          %.1f / 10
                Fraud Type Severity (10%%): %.1f / 10
                Total Money Flow     : ₹%.2f Cr
                Total Victims        : %d
                Days Since Active    : %d
                Fraud Type           : %s
                Key Drivers          : %s
                
                Write a single concise paragraph (80-120 words) in formal English that:
                1. States the overall risk classification
                2. Explains the two highest-scoring dimensions in plain language
                3. Recommends the appropriate law-enforcement response
                4. Does NOT use technical jargon or formula notation
                
                Respond ONLY with this JSON:
                {"justification": "<paragraph here>"}
                """.formatted(
                b.getRingId(), b.getLocationName(),
                b.getThreatScore(), b.getRiskLevel(),
                b.getFinancialImpactScore(), b.getVictimVolumeScore(),
                b.getNetworkComplexityScore(), b.getRecencyScore(),
                b.getFraudTypeSeverityScore(),
                b.getTotalMoneyFlowInr() / 10_000_000.0, b.getTotalVictims(),
                b.getDaysSinceLastActive(), b.getFraudType(),
                String.join("; ", b.getKeyDrivers())
        );
    }

    // =========================================================================
    // RESPONSE PARSERS
    // =========================================================================

    private TranscriptAnalysisResult parseTranscriptResult(String rawGeminiResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawGeminiResponse);
        String   text = extractTextFromGeminiResponse(root);

        // Strip markdown code fences if Gemini ignored the instruction
        text = stripMarkdownFences(text);
        log.debug("Gemini transcript response text: {}", text);

        JsonNode node = objectMapper.readTree(text);

        List<String> triggers = objectMapper.convertValue(
                node.path("trigger_phrases"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

        List<String> actions = objectMapper.convertValue(
                node.path("recommended_actions"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

        return TranscriptAnalysisResult.builder()
                .scamType(node.path("scam_type").asText("UNKNOWN"))
                .confidence(node.path("confidence").asInt(0))
                .riskLevel(node.path("risk_level").asText("LOW"))
                .triggerPhrases(triggers)
                .authorityImpersonationDetected(node.path("authority_impersonation_detected").asBoolean(false))
                .financialPressureDetected(node.path("financial_pressure_detected").asBoolean(false))
                .suggestedAction(node.path("suggested_action").asText())
                .recommendedActions(actions)
                .explanation(node.path("explanation").asText())
                .analyzedAt(Instant.now().toString())
                .isFallback(false)
                .build();
    }

    private RingIntelligenceNarrative parseRingNarrative(String rawGeminiResponse, String ringId)
            throws Exception {
        JsonNode root = objectMapper.readTree(rawGeminiResponse);
        String   text = extractTextFromGeminiResponse(root);
        text = stripMarkdownFences(text);

        log.debug("Gemini ring narrative response text: {}", text);
        JsonNode node = objectMapper.readTree(text);

        List<String> steps = objectMapper.convertValue(
                node.path("intervention_steps"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

        return RingIntelligenceNarrative.builder()
                .ringId(ringId)
                .intelligenceBrief(node.path("intelligence_brief").asText())
                .probableHub(node.path("probable_hub").asText())
                .primaryTactic(node.path("primary_tactic").asText())
                .victimProfile(node.path("victim_profile").asText())
                .interventionSteps(steps)
                .estimatedInterdictionWindow(node.path("estimated_interdiction_window").asText())
                .threatAssessment(node.path("threat_assessment").asText())
                .generatedAt(Instant.now().toString())
                .isFallback(false)
                .build();
    }

    // =========================================================================
    // CORE HTTP CALL  (RestClient — Spring 6 synchronous client)
    // =========================================================================

    /**
     * Sends a prompt to Gemini 2.5 Flash and returns the raw JSON response string.
     *
     * <p>Uses {@link RestClient} — the Spring 6 synchronous HTTP client.
     * One retry is attempted on any exception before giving up.
     *
     * @param prompt    the fully assembled prompt string
     * @param callLabel human-readable label used in log messages
     * @return raw Gemini JSON response, or null if both attempts fail
     */
    private String callGeminiWithRetry(String prompt, String callLabel) {
        Map<String, Object> body = buildRequestBody(prompt);
        return callGeminiWithRetry(body, callLabel);
    }

    private String callGeminiWithRetry(Map<String, Object> body, String callLabel) {
        int maxAttempts = 4;
        long delayMs = 1500;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.debug("Gemini {} — attempt {}/{}", callLabel, attempt, maxAttempts);

                String response = RestClient.create()
                        .post()
                        .uri(groqUrl + "/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                log.debug("Gemini {} — success on attempt {}", callLabel, attempt);
                return response;

            } catch (RestClientException ex) {
                log.warn("Gemini {} — attempt {} failed: {}", callLabel, attempt, ex.getMessage());
                if (attempt == maxAttempts) {
                    log.error("Gemini {} — all {} attempts failed, returning fallback", callLabel, maxAttempts);
                } else {
                    try { 
                        log.info("Gemini {} — backing off for {}ms before next attempt...", callLabel, delayMs);
                        Thread.sleep(delayMs); 
                        delayMs *= 2; // Exponential backoff: 1500ms, 3000ms, 6000ms
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Builds the standard Gemini API request body.
     *
     * <p>Key config choices:
     * <ul>
     *   <li>{@code temperature=0.1} — near-deterministic; we want consistent
     *       classification, not creative prose (except the narrative method)</li>
     *   <li>{@code maxOutputTokens=2048} — enough for a full narrative brief</li>
     *   <li>{@code responseMimeType="application/json"} — instructs Gemini to
     *       return only JSON; eliminates markdown fence stripping in most cases</li>
     * </ul>
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        return Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object")
        );
    }

    // =========================================================================
    // UTILITIES
    // =========================================================================

    /**
     * Extracts the model-generated text from Gemini's response envelope.
     *
     * <p>Gemini wraps the actual content in:
     * {@code candidates[0].content.parts[0].text}
     */
    private String extractTextFromGeminiResponse(JsonNode root) {
        return root
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();
    }

    /**
     * Removes markdown code fences (```json ... ```) that Gemini sometimes
     * inserts despite the {@code responseMimeType} instruction.
     */
    private String stripMarkdownFences(String text) {
        if (text == null) return "{}";
        String stripped = text.trim();
        if (stripped.startsWith("```")) {
            stripped = stripped.replaceFirst("```(?:json)?\\s*", "");
            int lastFence = stripped.lastIndexOf("```");
            if (lastFence > 0) stripped = stripped.substring(0, lastFence).trim();
        }
        return stripped;
    }

    private String defaultScoreJustification(ThreatScoreBreakdown b) {
        return String.format(Locale.US,
                "This fraud ring has been assigned a threat score of %.1f/10 (%s risk level) " +
                "based on automated analysis of its financial impact (₹%.2f Cr laundered), " +
                "victim volume (%d victims), and operational recency (%d days since last activity). " +
                "Recommended response: %s.",
                b.getThreatScore(), b.getRiskLevel(),
                b.getTotalMoneyFlowInr() / 10_000_000.0,
                b.getTotalVictims(), b.getDaysSinceLastActive(),
                b.getRecommendedAction() != null ? b.getRecommendedAction() : "escalate to cybercrime cell");
    }

    // ── Fallbacks ─────────────────────────────────────────────────────────────

    private TranscriptAnalysisResult transcriptFallback() {
        log.warn("Returning transcript analysis fallback due to Gemini API failure");
        return TranscriptAnalysisResult.builder()
                .scamType("UNKNOWN")
                .confidence(0)
                .riskLevel("LOW")
                .triggerPhrases(List.of())
                .authorityImpersonationDetected(false)
                .financialPressureDetected(false)
                .suggestedAction("Service temporarily unavailable. Call 1930 for immediate assistance.")
                .recommendedActions(List.of(
                        "Call the national cybercrime helpline: 1930",
                        "File a complaint at cybercrime.gov.in"))
                .explanation("Analysis service is temporarily unavailable. Please proceed with caution.")
                .analyzedAt(Instant.now().toString())
                .isFallback(true)
                .build();
    }

    private RingIntelligenceNarrative ringNarrativeFallback(String ringId) {
        log.warn("Returning ring narrative fallback for ringId={}", ringId);
        return RingIntelligenceNarrative.builder()
                .ringId(ringId)
                .intelligenceBrief("Intelligence brief generation temporarily unavailable. Please review the structured data manually.")
                .probableHub("Unknown")
                .primaryTactic("Unknown")
                .victimProfile("Unknown")
                .interventionSteps(List.of(
                        "Review mule account transaction records",
                        "Cross-reference with NCRP complaint database",
                        "Coordinate with state cybercrime cell"))
                .estimatedInterdictionWindow("Unknown — manual assessment required")
                .threatAssessment("AI narrative generation unavailable — refer to threat score.")
                .generatedAt(Instant.now().toString())
                .isFallback(true)
                .build();
    }
}
