package com.fraudshield.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudshield.dto.FraudAnalysisRequest;
import com.fraudshield.dto.FraudAnalysisResponse;
import com.fraudshield.model.jpa.Complaint;
import com.fraudshield.repository.ComplaintRepository;
import com.fraudshield.repository.PhoneNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the full Fraud Shield pipeline:
 * <ol>
 *   <li>Calls Gemini for text classification</li>
 *   <li>Enriches with Neo4j graph data (is this phone in the fraud network?)</li>
 *   <li>Persists the complaint to PostgreSQL</li>
 *   <li>Returns the unified {@link FraudAnalysisResponse}</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudShieldService {

    private final GeminiService geminiService;
    private final ComplaintRepository complaintRepository;
    private final PhoneNumberRepository phoneNumberRepository;
    private final ObjectMapper objectMapper;

    /**
     * End-to-end fraud analysis for a text description.
     */
    public FraudAnalysisResponse analyze(FraudAnalysisRequest request) {
        log.info("Starting fraud analysis | phoneNumber={}", request.getPhoneNumber());

        // 1. Get Gemini classification
        FraudAnalysisResponse response = geminiService.classifyFraud(request.getDescription());

        // 2. Enrich with graph network data if a phone number was provided
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            enrichWithGraphData(response, request.getPhoneNumber());
        }

        // 3. Persist complaint to PostgreSQL
        Complaint saved = persistComplaint(request, response);
        response.setComplaintId(saved.getId());

        log.info("Fraud analysis complete | complaintId={} | scamType={} | confidence={}",
                saved.getId(), response.getScamType(), response.getConfidence());

        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void enrichWithGraphData(FraudAnalysisResponse response, String phoneNumber) {
        phoneNumberRepository.findByNumber(phoneNumber).ifPresent(node -> {
            response.setInFraudNetwork(true);
            int victimCount = node.getVictims() != null ? node.getVictims().size() : 0;
            response.setNetworkVictimCount(victimCount);
            log.info("Phone {} is in fraud network with {} linked victims", phoneNumber, victimCount);
        });

        if (response.getInFraudNetwork() == null) {
            response.setInFraudNetwork(false);
            response.setNetworkVictimCount(0);
        }
    }

    private Complaint persistComplaint(FraudAnalysisRequest request, FraudAnalysisResponse response) {
        try {
            Complaint complaint = Complaint.builder()
                    .description(request.getDescription())
                    .scamType(response.getScamType())
                    .confidence(response.getConfidence())
                    .triggerPhrases(objectMapper.writeValueAsString(response.getTriggerPhrases()))
                    .recommendedActions(objectMapper.writeValueAsString(response.getRecommendedActions()))
                    .sourceChannel("TEXT")
                    .phoneNumber(request.getPhoneNumber())
                    .victimCity(request.getVictimCity())
                    .build();

            return complaintRepository.save(complaint);
        } catch (Exception ex) {
            log.error("Failed to persist complaint: {}", ex.getMessage(), ex);
            // Return a transient complaint with id=null — don't fail the user
            return new Complaint();
        }
    }
}
