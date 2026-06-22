package com.fraudshield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for the {@code POST /api/v1/gemini/analyze-transcript} endpoint.
 */
@Data
public class TranscriptAnalysisRequest {

    /**
     * The raw text to analyse — either a user-typed description or the
     * output from Whisper transcription of an audio file.
     */
    @NotBlank(message = "Transcript must not be blank")
    @Size(max = 8000, message = "Transcript must not exceed 8000 characters")
    private String transcript;
}
