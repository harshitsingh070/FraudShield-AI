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
 * Neo4j graph node representing a phone number in the fraud network.
 *
 * <p>Relationships:
 * <ul>
 *   <li>{@code CALLED}   → another PhoneNumber (scammer-to-scammer or call chain)</li>
 *   <li>{@code VICTIMIZED} → Victim nodes (victims reached by this number)</li>
 * </ul>
 */
@Node("PhoneNumber")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhoneNumber {

    @Id
    @GeneratedValue
    private Long id;

    /** The actual phone number string (E.164 or local format). */
    @Property("number")
    private String number;

    /** Computed risk level: LOW | MEDIUM | HIGH | CRITICAL */
    @Property("risk_level")
    private String riskLevel;

    /** Total number of complaints that reference this number. */
    @Property("complaint_count")
    private Integer complaintCount;

    /** State/city where the SIM was registered (if known). */
    @Property("registered_location")
    private String registeredLocation;

    /** Whether this number is flagged as a known scammer burner. */
    @Property("is_burner")
    private Boolean isBurner;

    // ---- Relationships ----

    @Relationship(type = "CALLED", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<PhoneNumber> calledNumbers = new HashSet<>();

    @Relationship(type = "VICTIMIZED", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<Victim> victims = new HashSet<>();
}
