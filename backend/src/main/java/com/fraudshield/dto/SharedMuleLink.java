package com.fraudshield.dto;

/**
 * Projection interface for the cross-ring shared mule account query.
 * Matches the RETURN variables from the Neo4j query.
 */
public interface SharedMuleLink {
    String getSharedAccount();
    String getRing1();
    String getRing2();
}
