package com.fraudshield.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Incoming request DTO for the Fraud Shield text analysis endpoint.
 */
@Data
public class FraudAnalysisRequest {

    /** The user's description of the suspicious event. */
    @NotBlank(message = "Description must not be empty")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    /** Optional: phone number mentioned in the incident. */
    @JsonProperty("phone_number")
    private String phoneNumber;

    /** Optional: victim's city. */
    @JsonProperty("victim_city")
    private String victimCity;
}
