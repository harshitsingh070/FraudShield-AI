package com.fraudshield.repository;

import com.fraudshield.model.graph.PhoneNumber;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data Neo4j repository for {@link PhoneNumber} nodes.
 *
 * <p>Custom Cypher queries power the fraud ring analysis endpoints.
 */
@Repository
public interface PhoneNumberRepository extends Neo4jRepository<PhoneNumber, String> {

    Optional<PhoneNumber> findByNumber(String number);

    /** Find all phone numbers whose risk level is HIGH or CRITICAL. */
    @Query("MATCH (p:PhoneNumber) WHERE p.risk_level IN ['HIGH', 'CRITICAL'] RETURN p")
    List<PhoneNumber> findHighRiskNumbers();

    /**
     * Core fraud-ring query: given a phone number, return the entire subgraph
     * within 3 relationship hops (scammer infrastructure + victims + accounts).
     */
    @Query("""
            MATCH path = (start:PhoneNumber {number: $number})-[*1..3]-(connected)
            RETURN path
            """)
    List<PhoneNumber> findFraudNetworkByNumber(@Param("number") String number);

    /** Count all PhoneNumber nodes with risk_level = HIGH or CRITICAL. */
    @Query("MATCH (p:PhoneNumber) WHERE p.risk_level IN ['HIGH', 'CRITICAL'] RETURN count(p)")
    long countHighRiskNumbers();

    /** Return top N most complained-about numbers for the dashboard alert table. */
    @Query("""
            MATCH (p:PhoneNumber)
            WHERE p.complaint_count IS NOT NULL
            RETURN p
            ORDER BY p.complaint_count DESC
            LIMIT $limit
            """)
    List<PhoneNumber> findTopByComplaintCount(@Param("limit") int limit);
}
