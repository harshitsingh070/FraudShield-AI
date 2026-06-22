package com.fraudshield.model.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * Neo4j graph node representing a bank account used in the fraud network.
 * Account numbers are stored as hashes to protect PII.
 */
@Node("BankAccount")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount {

    @Id
    @GeneratedValue
    private Long id;

    /** SHA-256 hash of the actual account number (never store raw). */
    @Property("account_hash")
    private String accountHash;

    /** Name of the bank (e.g. SBI, HDFC). */
    @Property("bank_name")
    private String bankName;

    /** Total number of incoming fraud transactions observed. */
    @Property("transaction_count")
    private Integer transactionCount;

    /** Whether this is a suspected mule account. */
    @Property("is_mule")
    private Boolean isMule;

    /** State where the account is registered. */
    @Property("state")
    private String state;
}
