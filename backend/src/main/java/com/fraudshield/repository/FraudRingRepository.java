package com.fraudshield.repository;

import com.fraudshield.domain.FraudRing;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data Neo4j repository for {@link FraudRing} nodes.
 *
 * <p>Provides both standard CRUD and custom Cypher queries that power:
 * <ul>
 *   <li>The dashboard's threat-score leaderboard</li>
 *   <li>Geospatial map pin data (lat/lon + threat score)</li>
 *   <li>ACTIVE ring count for the top metric cards</li>
 *   <li>Cross-ring mule account linkage detection</li>
 * </ul>
 */
@Repository
public interface FraudRingRepository extends Neo4jRepository<FraudRing, String> {

    /** All active rings ordered by threat score descending — powers the alert table. */
    List<FraudRing> findByStatusOrderByThreatScoreDesc(String status);

    /** Rings whose threat score meets or exceeds a threshold (for CRITICAL/HIGH filters). */
    List<FraudRing> findByThreatScoreGreaterThanEqualOrderByThreatScoreDesc(double minScore);

    /** All rings of a specific fraud type (e.g., DIGITAL_ARREST, INVESTMENT_FRAUD). */
    List<FraudRing> findByFraudType(String fraudType);

    // findAllWithMuleAccounts removed in favor of standard findAll()

    /**
     * Counts all ACTIVE rings — used by the "Fraud Rings Detected" dashboard card.
     */
    @Query("MATCH (r:FraudRing {status: 'ACTIVE'}) RETURN count(r)")
    Long countActiveRings();

    /**
     * Sums total money laundered across ALL rings — for the "Financial Impact" card.
     */
    @Query("MATCH (r:FraudRing) RETURN coalesce(sum(r.totalMoneyLaundered), 0)")
    Double sumTotalMoneyLaundered();

    interface CrossRingLink {
        String getRing1();
        String getRing2();
        String getSharedAccount();
    }

    /**
     * Detects shared mule accounts across rings — a graph-only insight impossible
     * in relational databases. Returns pairs of ring IDs sharing an account.
     */
    @Query("""
            MATCH (r1:FraudRing)-[:OPERATES_THROUGH]->(m:MuleAccount)<-[:OPERATES_THROUGH]-(r2:FraudRing)
            WHERE r1.ringId < r2.ringId
            RETURN r1.ringId AS ring1, r2.ringId AS ring2, m.accountNumber AS sharedAccount
            """)
    List<CrossRingLink> findSharedMuleAccounts();

    /**
     * Top N rings by total victim count — for dashboard victim impact summary.
     */
    @Query("""
            MATCH (r:FraudRing)
            RETURN r
            ORDER BY r.totalVictimCount DESC
            LIMIT $limit
            """)
    List<FraudRing> findTopByVictimCount(@Param("limit") int limit);
}
