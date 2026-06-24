package com.fraudshield.controller;

import com.fraudshield.service.AudioAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/audio")
@CrossOrigin(originPatterns = "*")
@RequiredArgsConstructor
@Slf4j
public class AudioAnalysisController {

    private final AudioAnalysisService audioAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeAudio(@RequestParam("audioFile") MultipartFile audioFile) {
        if (audioFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File must not be empty");
        }

        String contentType = audioFile.getContentType();
        if (contentType == null || (!contentType.startsWith("audio/") && !contentType.equals("application/octet-stream"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File must be an audio file (received " + contentType + ")");
        }

        // Windows curl often defaults to application/octet-stream for mp3s, but Gemini strictly requires an audio MIME type
        if ("application/octet-stream".equals(contentType)) {
            contentType = "audio/mp3";
        }

        try {
            log.info("Processing audio file: {} (size: {} bytes)", audioFile.getOriginalFilename(), audioFile.getSize());

            // 1. Transcribe and Classify Audio via Gemini
            Map<String, Object> response = audioAnalysisService.transcribeAndAnalyze(audioFile.getBytes(), contentType);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Audio analysis failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error occurred"));
        }
    }
}
