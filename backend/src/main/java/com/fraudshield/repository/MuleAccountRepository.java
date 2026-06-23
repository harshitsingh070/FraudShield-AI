package com.fraudshield.repository;

import com.fraudshield.domain.MuleAccount;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fraudshield.dto.SharedMuleLink;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data Neo4j repository for {@link MuleAccount} nodes.
 *
 * <p>Key queries surface high-value accounts for the dashboard's financial
 * intelligence section and enable cross-ring linkage detection.
 */
@Repository
public interface MuleAccountRepository extends Neo4jRepository<MuleAccount, String> {

    Optional<MuleAccount> findByAccountNumber(String accountNumber);

    List<MuleAccount> findByBankNameOrderByTotalMoneyFlowDesc(String bankName);

    List<MuleAccount> findByConfirmedTrue();

    /** Accounts by state — used for the geospatial banking intelligence layer. */
    List<MuleAccount> findByRegisteredState(String state);

    /**
     * Top mule accounts ranked by total money flow — powers the "Biggest Mule Accounts"
     * dashboard table.
     */
    @Query("""
            MATCH (m:MuleAccount)
            RETURN m
            ORDER BY m.totalMoneyFlow DESC
            LIMIT $limit
            """)
    List<MuleAccount> findTopByMoneyFlow(@Param("limit") int limit);

    /**
     * Sum of all confirmed mule account money flows — total financial intelligence figure.
     */
    @Query("MATCH (m:MuleAccount {confirmed: true}) RETURN coalesce(sum(m.totalMoneyFlow), 0)")
    Double sumConfirmedMoneyFlow();

    /**
     * Accounts that appear in more than one FraudRing relationship — the most
     * important graph intelligence finding (cross-ring coordination signal).
     */
    @Query("""
            MATCH (r:FraudRing)-[:OPERATES_THROUGH]->(m:MuleAccount)
            WITH m, collect(r.ringId) AS rings, count(r) AS ringCount
            WHERE ringCount > 1
            RETURN m.accountNumber AS sharedAccount, 
                   rings[0] AS ring1, 
                   rings[1] AS ring2
            ORDER BY ringCount DESC
            """)
    List<SharedMuleLink> findSharedAcrossRings();
}
