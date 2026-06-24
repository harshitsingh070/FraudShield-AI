package com.fraudshield.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudshield.dto.FraudAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Core Gemini AI integration service.
 *
 * <p>Sends user descriptions to the Gemini API with a carefully engineered
 * fraud-classification prompt and parses the structured JSON response into a
 * {@link FraudAnalysisResponse}. This service is the intellectual core of the
 * Fraud Shield component.
 *
 * <p><b>Prompt engineering goals:</b>
 * <ul>
 *   <li>Understand Indian cybercrime context specifically</li>
 *   <li>Identify digital arrest, investment, lottery, job, and romance fraud</li>
 *   <li>Extract exact trigger phrases</li>
 *   <li>Produce a confidence percentage reflecting actual ambiguity</li>
 *   <li>Always return clean JSON parseable by this service</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper;

    @Value("${google.ai.studio.api.key}")
    private String apiKey;

    // ─────────────────────────────────────────────────────────────────────────
    // Fraud Classification Prompt (core IP — tune this heavily during testing)
    // ─────────────────────────────────────────────────────────────────────────
    private static final String FRAUD_CLASSIFICATION_PROMPT = """
            You are an expert AI analyst for India's Cyber Crime Coordination Centre (I4C).
            Your role is to classify suspicious communications reported by Indian citizens and
            provide actionable intelligence for law enforcement.
            
            FRAUD CATEGORIES YOU MUST DETECT:
            1. DIGITAL_ARREST — Callers impersonating CBI, ED, Customs, TRAI, or police officers.
               Claim victim's Aadhaar/number is linked to money laundering, drug trafficking, or
               terror finance. Demand the victim stay on a video call for "digital arrest".
               Key phrases: "digital arrest", "ED/CBI notice", "Aadhaar linked to crime",
               "stay on video call", "warrant", "FIR will be filed".
               
            2. INVESTMENT_FRAUD — Promises of extraordinary stock/crypto/trading returns.
               Often involve WhatsApp/Telegram investment groups, fake trading apps, initial
               small profits to build trust before large losses.
               Key phrases: "guaranteed returns", "double your money", "investment group",
               "trading tips", "blockchain profit".
               
            3. LOTTERY_FRAUD — Fake prize notifications requiring upfront payment.
               Key phrases: "you have won", "lucky draw", "claim your prize", "registration fee",
               "customs clearance", "iPhone/car won".
               
            4. JOB_FRAUD — Fake job offers from reputable companies requiring registration fees,
               equipment purchases, or personal document submission.
               Key phrases: "work from home", "registration fee", "job offer", "salary advance",
               "training fee", "onboarding payment".
               
            5. ROMANCE_FRAUD — Long-term emotional manipulation to extract money.
               Key phrases: "send money", "medical emergency", "stranded abroad", "gift customs".
               
            6. LEGITIMATE — The described interaction shows no fraud indicators.
            
            7. UNKNOWN — Insufficient information to classify.
            
            ANALYSIS RULES:
            - Base confidence on: specificity of fraud markers, financial pressure tactics,
              authority impersonation, urgency, and fear induction.
            - Extract ONLY phrases that directly appear in the user's description.
            - Risk levels: CRITICAL (>85% confidence + financial transfer requested),
              HIGH (70–85%), MEDIUM (40–70%), LOW (<40% or LEGITIMATE).
            - Recommended actions must be specific to India (cybercrime.gov.in, 1930 helpline).
            
            USER DESCRIPTION:
            "%s"
            
            Respond ONLY with valid JSON in this EXACT schema (no markdown, no explanation):
            {
              "scam_type": "<DIGITAL_ARREST|INVESTMENT_FRAUD|LOTTERY_FRAUD|JOB_FRAUD|ROMANCE_FRAUD|LEGITIMATE|UNKNOWN>",
              "confidence": <0-100 integer>,
              "risk_level": "<CRITICAL|HIGH|MEDIUM|LOW>",
              "trigger_phrases": ["phrase1", "phrase2"],
              "recommended_actions": [
                "Immediately disconnect and do not call back",
                "File a complaint at cybercrime.gov.in",
                "Call the national cybercrime helpline: 1930"
              ],
              "explanation": "<1-2 sentences explaining classification for the citizen>"
            }
            """;

    /**
     * Classifies a fraud description using Gemini and returns structured results.
     *
     * @param description The user-submitted incident description
     * @return A parsed {@link FraudAnalysisResponse}
     */
    public FraudAnalysisResponse classifyFraud(String description) {
        log.info("Sending fraud classification request to Gemini for description length: {}",
                description.length());

        String prompt = FRAUD_CLASSIFICATION_PROMPT.formatted(description);

        // Build the Gemini API request body
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,          // low temp for deterministic classification
                        "maxOutputTokens", 1024,
                        "responseMimeType", "application/json"
                ),
                "safetySettings", List.of(
                        Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_NONE"),
                        Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_NONE"),
                        Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_NONE"),
                        Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_NONE")
                )
        );

        try {
            String rawResponse = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(":generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseGeminiResponse(rawResponse);

        } catch (Exception ex) {
            log.error("Gemini API call failed: {}", ex.getMessage(), ex);
            // Return a safe fallback so the frontend never crashes
            return FraudAnalysisResponse.builder()
                    .scamType("UNKNOWN")
                    .confidence(0.0)
                    .riskLevel("LOW")
                    .triggerPhrases(List.of())
                    .recommendedActions(List.of(
                            "Service temporarily unavailable. Please call 1930 for immediate assistance."))
                    .explanation("Analysis service encountered an error. Manual review recommended.")
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extracts the JSON content from Gemini's wrapper response and maps it
     * to our internal DTO.
     */
    private FraudAnalysisResponse parseGeminiResponse(String rawJson) throws Exception {
        JsonNode root = objectMapper.readTree(rawJson);

        // Navigate Gemini response structure: candidates[0].content.parts[0].text
        String contentText = root
                .path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();

        log.debug("Gemini raw classification JSON: {}", contentText);

        JsonNode parsed = objectMapper.readTree(contentText);

        List<String> triggerPhrases = objectMapper.convertValue(
                parsed.path("trigger_phrases"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

        List<String> recommendedActions = objectMapper.convertValue(
                parsed.path("recommended_actions"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

        return FraudAnalysisResponse.builder()
                .scamType(parsed.path("scam_type").asText("UNKNOWN"))
                .confidence(parsed.path("confidence").asDouble(0))
                .riskLevel(parsed.path("risk_level").asText("LOW"))
                .triggerPhrases(triggerPhrases)
                .recommendedActions(recommendedActions)
                .explanation(parsed.path("explanation").asText())
                .build();
    }
}
