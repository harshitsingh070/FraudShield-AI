package com.fraudshield.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Gemini API client configuration.
 * Wires a pre-configured WebClient bean that all Gemini service classes inject.
 */
@Configuration
public class GeminiConfig {

    @Value("${google.ai.studio.api.key}")
    private String apiKey;

    @Value("${google.ai.studio.url}")
    private String baseUrl;

    /**
     * Returns a WebClient scoped to the Gemini base URL.
     * The API key is appended as a query parameter per Google AI Studio spec.
     */
    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultUriVariables(java.util.Map.of("key", apiKey))
                .build();
    }
}
