package com.fraudshield.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Data structure required by the frontend's `react-force-graph` component.
 * Represents the nodes and edges of the fraud network.
 */
@Data
@Builder
public class GraphDataResponse {

    private List<NodeData> nodes;
    private List<LinkData> links;

    @Data
    @Builder
    public static class NodeData {
        private String id;
        private String name;
        private String group; // "ring" or "mule"
        
        /** Visual size of the node in the graph */
        private int val;
        
        @JsonProperty("threat_score")
        private Double threatScore;
        
        /** Additional metadata for the node tooltip */
        private String description;
    }

    @Data
    @Builder
    public static class LinkData {
        private String source;
        private String target;
        private String label; // e.g., "OPERATES_THROUGH"
    }
}
