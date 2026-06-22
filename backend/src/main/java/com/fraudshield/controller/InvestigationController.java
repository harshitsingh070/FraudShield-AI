package com.fraudshield.controller;

import com.fraudshield.domain.MuleAccount;
import com.fraudshield.dto.SharedMuleLink;
import com.fraudshield.model.graph.PhoneNumber;
import com.fraudshield.repository.FraudRingRepository;
import com.fraudshield.repository.MuleAccountRepository;
import com.fraudshield.repository.PhoneNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the Investigation Workbench.
 * Provides endpoints for analysts to drill down into specific phone numbers,
 * mule accounts, and cross-ring linkages.
 */
@RestController
@RequestMapping("/api/v1/investigation")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class InvestigationController {

    private final MuleAccountRepository muleAccountRepository;
    private final PhoneNumberRepository phoneNumberRepository;
    private final FraudRingRepository fraudRingRepository;

    /**
     * Cross-ring linkage detection.
     * Returns mule accounts that are connected to > 1 FraudRing.
     */
    @GetMapping("/cross-ring-links")
    public ResponseEntity<List<SharedMuleLink>> getCrossRingLinks() {
        log.info("Fetching cross-ring mule account links");
        return ResponseEntity.ok(muleAccountRepository.findSharedAcrossRings());
    }

    /**
     * Fetch the 3-hop subgraph for a specific phone number.
     * Uses the legacy Phase 1 Graph Intelligence data.
     */
    @GetMapping("/phone/{number}/network")
    public ResponseEntity<List<PhoneNumber>> getPhoneNetwork(@PathVariable String number) {
        log.info("Fetching fraud network for phone number: {}", number);
        List<PhoneNumber> network = phoneNumberRepository.findFraudNetworkByNumber(number);
        if (network.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(network);
    }
    
    /**
     * Get details of a specific mule account by account number.
     */
    @GetMapping("/mule/{accountNumber}")
    public ResponseEntity<MuleAccount> getMuleAccountDetails(@PathVariable String accountNumber) {
        log.info("Fetching details for mule account: {}", accountNumber);
        return muleAccountRepository.findByAccountNumber(accountNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves cross-ring linkages as pairs of Ring IDs sharing an account.
     * Useful for correlation matrix or direct visualization.
     */
    @GetMapping("/cross-ring-pairs")
    public ResponseEntity<?> getCrossRingPairs() {
        log.info("Fetching cross-ring pairs");
        return ResponseEntity.ok(fraudRingRepository.findSharedMuleAccounts());
    }
}
