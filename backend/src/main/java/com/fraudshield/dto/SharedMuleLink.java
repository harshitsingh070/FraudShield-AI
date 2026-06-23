package com.fraudshield.dto;

/**
 * Projection interface for the cross-ring shared mule account query.
 * Matches the RETURN variables from the Neo4j query.
 */
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedMuleLink {
    private String sharedAccount;
    private String ring1;
    private String ring2;
}
