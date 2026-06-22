package com.fraudshield.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * Neo4j node representing a coordinated fraud ring.
 *
 * <p>A FraudRing is a named, geolocated criminal operation (e.g., a Jamtara
 * call-centre gang or a Delhi digital-arrest cluster). It aggregates one or
 * more {@link MuleAccount}s through which stolen funds flow, and carries a
 * pre-computed {@code threatScore} that drives the Command Centre dashboard's
 * colour-coding and alert priority.
 *
 * <p>Relationship: {@code (FraudRing)-[:OPERATES_THROUGH]->(MuleAccount)}
 */
@Node("FraudRing")
@Data
public class FraudRing {

    /** Stable business key — format: RING-{STATE_CODE}-{SEQ}, e.g. RING-JH-01 */
    @Id
    private String ringId;

    /** Human-readable name displayed in the dashboard, e.g. "Jamtara Network" */
    private String locationName;

    /** WGS-84 latitude of the ring's primary operating base */
    private double latitude;

    /** WGS-84 longitude of the ring's primary operating base */
    private double longitude;

    /**
     * AI-computed threat score in range [0.0, 10.0].
     * Combines complaint velocity, financial impact, and victim count.
     * ≥ 8.0 → CRITICAL, 6–8 → HIGH, 4–6 → MEDIUM, < 4 → LOW
     */
    private double threatScore;

    /** Category of fraud this ring primarily runs */
    private String fraudType;

    /** Current operational status: ACTIVE | DORMANT | DISMANTLED */
    private String status;

    /** Total estimated money laundered in INR across all mule accounts */
    private double totalMoneyLaundered;

    /** Total unique victims across all mule accounts */
    private int totalVictimCount;

    /** ISO-8601 date when first complaint was linked to this ring */
    private String firstSeenDate;

    /** ISO-8601 date of most recent complaint linked to this ring */
    private String lastActiveDate;

    /**
     * Mule bank accounts through which this ring moves funds.
     * Spring Data Neo4j cascades saves through this relationship.
     */
    @Relationship(type = "OPERATES_THROUGH", direction = Relationship.Direction.OUTGOING)
    private Set<MuleAccount> muleAccounts = new HashSet<>();
}
