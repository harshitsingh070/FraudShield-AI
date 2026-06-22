package com.fraudshield.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Neo4j node representing a bank account used as a money-mule vehicle by a
 * {@link FraudRing}.
 *
 * <p>Mule accounts are the critical financial chokepoints in fraud operations:
 * stolen funds are transferred here before being further laundered or
 * withdrawn. Storing them as graph nodes enables cross-ring linkage analysis —
 * the same mule account appearing under two FraudRings is strong evidence of
 * a coordinated network.
 *
 * <p>Account numbers are stored in plain text here for demo purposes. In
 * production, replace {@code accountNumber} with a SHA-256 hash to protect PII.
 */
@Node("MuleAccount")
@Data
public class MuleAccount {

    /** Bank account number — use a hash in production. */
    @Id
    private String accountNumber;

    /** Bank name (e.g., "State Bank of India", "HDFC Bank") */
    private String bankName;

    /** IFSC code of the branch (links to geolocation in production) */
    private String ifscCode;

    /** Cumulative total of fraudulent inflows in INR */
    private double totalMoneyFlow;

    /** Number of distinct victims who transferred money to this account */
    private int victimCount;

    /** Whether this account is confirmed as a mule (vs. suspected) */
    private boolean confirmed;

    /** State where the account holder's KYC address is registered */
    private String registeredState;

    /** Date when this account was first observed in a fraud chain (ISO-8601) */
    private String flaggedDate;
}
