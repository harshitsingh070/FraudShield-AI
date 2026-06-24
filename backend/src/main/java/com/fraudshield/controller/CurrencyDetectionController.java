package com.fraudshield.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudshield.dto.CurrencyAnalysisResponse;
import com.fraudshield.service.GeminiAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/currency")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(originPatterns = "*")
public class CurrencyDetectionController {

    private final GeminiAiService geminiAiService;
    private final ObjectMapper objectMapper;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeCurrency(@RequestParam("image") MultipartFile image) {
        if (image.isEmpty() || image.getContentType() == null || !image.getContentType().startsWith("image/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid file: must be a non-empty image");
        }

        try {
            String jsonResult = geminiAiService.analyzeCurrencyImage(image.getBytes(), image.getContentType());
            CurrencyAnalysisResponse response = objectMapper.readValue(jsonResult, CurrencyAnalysisResponse.class);
            return ResponseEntity.ok(response);
        } catch (Throwable ex) {
            log.error("Error analyzing currency image: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ex.getClass().getName() + ": " + ex.getMessage());
        }
    }
}
