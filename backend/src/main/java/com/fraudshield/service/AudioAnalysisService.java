package com.fraudshield.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class AudioAnalysisService {

    private final GeminiAiService geminiAiService;
    private final ObjectMapper objectMapper;

    public AudioAnalysisService(GeminiAiService geminiAiService, ObjectMapper objectMapper) {
        this.geminiAiService = geminiAiService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> transcribeAndAnalyze(byte[] audioBytes, String mimeType) {
        try {
            log.info("Requesting audio transcription and analysis via Gemini...");
            String jsonResult = geminiAiService.analyzeAudio(audioBytes, mimeType);
            return objectMapper.readValue(jsonResult, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to parse Gemini audio analysis result: {}", e.getMessage());
            throw new RuntimeException("Gemini audio analysis failed: " + e.getMessage(), e);
        }
    }
}
