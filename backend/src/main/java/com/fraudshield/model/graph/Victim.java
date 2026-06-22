package com.fraudshield.model.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * Neo4j graph node representing a fraud victim.
 *
 * <p>Relationships:
 * <ul>
 *   <li>{@code RECEIVED_MONEY} → BankAccount (where victim's money was sent)</li>
 * </ul>
 */
@Node("Victim")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Victim {

    @Id
    @GeneratedValue
    private Long id;

    /** Anonymised victim identifier (e.g. UUID or case number). */
    @Property("victim_id")
    private String victimId;

    /** City where the victim resides. */
    @Property("city")
    private String city;

    /** Amount lost in Indian Rupees. */
    @Property("amount_lost")
    private Double amountLost;

    /** Whether this victim has officially filed a complaint. */
    @Property("reported")
    private Boolean reported;

    /** Date of the scam incident (ISO-8601 string for simplicity). */
    @Property("incident_date")
    private String incidentDate;

    // ---- Relationships ----

    @Relationship(type = "RECEIVED_MONEY", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<BankAccount> bankAccounts = new HashSet<>();
}
