package com.fraudshield.controller;

import com.fraudshield.domain.FraudRing;
import com.fraudshield.domain.MuleAccount;
import com.fraudshield.dto.GraphDataResponse;
import com.fraudshield.repository.FraudRingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * REST controller for the react-force-graph visualization layer.
 * Formats Neo4j graph data into the standard nodes/links JSON schema.
 */
@RestController
@RequestMapping("/api/v1/graph")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class NetworkGraphController {

    private final FraudRingRepository fraudRingRepository;

    @GetMapping("/network")
    public ResponseEntity<GraphDataResponse> getNetworkGraph() {
        log.info("Generating network graph data for visualization");
        List<FraudRing> rings = fraudRingRepository.findAll();

        List<GraphDataResponse.NodeData> nodes = new ArrayList<>();
        List<GraphDataResponse.LinkData> links = new ArrayList<>();
        Set<String> processedMules = new HashSet<>();

        for (FraudRing ring : rings) {
            // Add ring node
            nodes.add(GraphDataResponse.NodeData.builder()
                    .id(ring.getRingId())
                    .name(ring.getLocationName())
                    .group("ring")
                    .val(10) // Fixed size for rings
                    .threatScore(ring.getThreatScore())
                    .description("Threat: " + ring.getThreatScore() + " | Type: " + ring.getFraudType())
                    .build());

            if (ring.getMuleAccounts() != null) {
                for (MuleAccount mule : ring.getMuleAccounts()) {
                    // Add mule node if not already added (it might be shared)
                    if (processedMules.add(mule.getAccountNumber())) {
                        nodes.add(GraphDataResponse.NodeData.builder()
                                .id(mule.getAccountNumber())
                                .name("Bank: " + mule.getBankName())
                                .group("mule")
                                .val(5) // Smaller size for mules
                                .threatScore(null)
                                .description("Flow: ₹" + (mule.getTotalMoneyFlow() / 100_000) + "L | Victims: " + mule.getVictimCount())
                                .build());
                    }

                    // Add link from ring to mule
                    links.add(GraphDataResponse.LinkData.builder()
                            .source(ring.getRingId())
                            .target(mule.getAccountNumber())
                            .label("OPERATES_THROUGH")
                            .build());
                }
            }
        }

        return ResponseEntity.ok(GraphDataResponse.builder()
                .nodes(nodes)
                .links(links)
                .build());
    }
}
