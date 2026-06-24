package com.fraudshield.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyAnalysisResponse {

    private String denomination;
    private int authenticityScore;
    private String verdict;
    private List<String> flaggedIssues;
    private List<SecurityFeature> securityFeatures;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SecurityFeature {
        private String name;
        private String status;
    }
}
