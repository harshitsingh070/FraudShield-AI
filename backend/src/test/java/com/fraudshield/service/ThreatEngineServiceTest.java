package com.fraudshield.service;

import com.fraudshield.domain.FraudRing;
import com.fraudshield.domain.MuleAccount;
import com.fraudshield.dto.ThreatScoreBreakdown;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link ThreatEngineService}.
 *
 * <p>Tests are organised by scoring dimension so failures pinpoint exactly
 * which axis is broken. All assertions use delta tolerances of ±0.01 to
 * account for floating-point rounding.
 *
 * <p>These tests double as documentation of the engine's behaviour — run
 * {@code mvn test -Dtest=ThreatEngineServiceTest} to generate the report
 * for the hackathon submission.
 */
class ThreatEngineServiceTest {

    private ThreatEngineService engine;

    @BeforeEach
    void setUp() {
        engine = new ThreatEngineService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Financial Impact component
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("1. Financial Impact Score")
    class FinancialImpact {

        @Test
        @DisplayName("₹0 laundered → score 0")
        void zero() {
            assertThat(engine.financialImpactScore(0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("₹2 Cr (ceiling) → score 10")
        void atCeiling() {
            assertThat(engine.financialImpactScore(20_000_000))
                    .isCloseTo(10.0, within(0.01));
        }

        @Test
        @DisplayName("₹5 Cr (over ceiling) → clamped to 10")
        void overCeiling() {
            assertThat(engine.financialImpactScore(50_000_000)).isEqualTo(10.0);
        }

        @Test
        @DisplayName("₹1 Cr → sqrt(0.5) × 10 ≈ 7.07")
        void halfCeiling() {
            double expected = Math.sqrt(0.5) * 10.0;
            assertThat(engine.financialImpactScore(10_000_000))
                    .isCloseTo(expected, within(0.01));
        }

        @Test
        @DisplayName("Square-root curve: ₹50 L < ₹1 Cr < ₹2 Cr are strictly ordered")
        void strictOrder() {
            double low  = engine.financialImpactScore(5_000_000);
            double mid  = engine.financialImpactScore(10_000_000);
            double high = engine.financialImpactScore(20_000_000);
            assertThat(low).isLessThan(mid);
            assertThat(mid).isLessThan(high);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Victim Volume component
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("2. Victim Volume Score")
    class VictimVolume {

        @Test
        @DisplayName("0 victims → score 0")
        void zero() {
            assertThat(engine.victimVolumeScore(0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("20 victims (ceiling) → score 10")
        void atCeiling() {
            assertThat(engine.victimVolumeScore(20)).isCloseTo(10.0, within(0.01));
        }

        @Test
        @DisplayName("40 victims (double ceiling) → clamped to 10")
        void overCeiling() {
            assertThat(engine.victimVolumeScore(40)).isEqualTo(10.0);
        }

        @Test
        @DisplayName("10 victims → score 5.0")
        void halfCeiling() {
            assertThat(engine.victimVolumeScore(10)).isCloseTo(5.0, within(0.01));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Network Complexity component
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("3. Network Complexity Score")
    class NetworkComplexity {

        @Test
        @DisplayName("0 mule accounts → score 0")
        void noAccounts() {
            assertThat(engine.networkComplexityScore(0, 0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("4 confirmed accounts (ceiling) → score 10")
        void fullyComplexAllConfirmed() {
            // 4 accounts all confirmed → count=10×0.6=6 + ratio=10×0.4=4 = 10
            assertThat(engine.networkComplexityScore(4, 4)).isCloseTo(10.0, within(0.01));
        }

        @Test
        @DisplayName("4 accounts, none confirmed → count component only")
        void maxCountNoConfirmed() {
            // count=6, ratio=0 → 6.0
            assertThat(engine.networkComplexityScore(4, 0)).isCloseTo(6.0, within(0.01));
        }

        @Test
        @DisplayName("2 accounts, 1 confirmed → intermediate score")
        void partial() {
            // count=(2/4)*10*0.6=3.0, ratio=(0.5)*10*0.4=2.0 → 5.0
            assertThat(engine.networkComplexityScore(2, 1)).isCloseTo(5.0, within(0.01));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Recency component
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("4. Recency Score")
    class Recency {

        @Test
        @DisplayName("Active today (0 days) → score 10")
        void activeToday() {
            assertThat(engine.recencyScore(0)).isCloseTo(10.0, within(0.01));
        }

        @Test
        @DisplayName("Active 7 days ago → score ≈ 7.8")
        void oneWeekOld() {
            double expected = 10.0 * Math.exp(-3.0 * 7.0 / 90.0);
            assertThat(engine.recencyScore(7)).isCloseTo(expected, within(0.01));
        }

        @Test
        @DisplayName("90 days ago → score approaches 0 (≈ 0.05)")
        void stale() {
            assertThat(engine.recencyScore(90)).isLessThan(0.1);
        }

        @Test
        @DisplayName("Unknown date (-1) → neutral score 5.0")
        void unknownDate() {
            assertThat(engine.recencyScore(-1)).isEqualTo(5.0);
        }

        @Test
        @DisplayName("Recency is strictly monotone decreasing as days increase")
        void monotone() {
            double d0  = engine.recencyScore(0);
            double d7  = engine.recencyScore(7);
            double d30 = engine.recencyScore(30);
            double d90 = engine.recencyScore(90);
            assertThat(d0).isGreaterThan(d7);
            assertThat(d7).isGreaterThan(d30);
            assertThat(d30).isGreaterThan(d90);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Fraud Type Severity component
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("5. Fraud Type Severity Score")
    class FraudTypeSeverity {

        @Test void digitalArrest()   { assertThat(engine.fraudTypeSeverityScore("DIGITAL_ARREST")).isEqualTo(10.0); }
        @Test void investmentFraud() { assertThat(engine.fraudTypeSeverityScore("INVESTMENT_FRAUD")).isEqualTo(8.0); }
        @Test void jobFraud()        { assertThat(engine.fraudTypeSeverityScore("JOB_FRAUD")).isEqualTo(6.0); }
        @Test void romanceFraud()    { assertThat(engine.fraudTypeSeverityScore("ROMANCE_FRAUD")).isEqualTo(6.0); }
        @Test void lotteryFraud()    { assertThat(engine.fraudTypeSeverityScore("LOTTERY_FRAUD")).isEqualTo(4.0); }
        @Test void unknown()         { assertThat(engine.fraudTypeSeverityScore("UNKNOWN")).isEqualTo(5.0); }
        @Test void nullType()        { assertThat(engine.fraudTypeSeverityScore(null)).isEqualTo(5.0); }

        @Test
        @DisplayName("Case-insensitive matching")
        void caseInsensitive() {
            assertThat(engine.fraudTypeSeverityScore("digital_arrest")).isEqualTo(10.0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Full composite score — end-to-end
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("6. Composite Score & Risk Bands")
    class Composite {

        @Test
        @DisplayName("Jamtara-profile ring scores CRITICAL (≥ 8.0)")
        void jamtaraProfile() {
            FraudRing ring = buildRing("DIGITAL_ARREST", todayMinus(2), 18_500_000, 14, 3, 3);
            double score = engine.calculateThreatScore(ring);
            assertThat(score).isGreaterThanOrEqualTo(8.0);
        }

        @Test
        @DisplayName("Lottery ring with low money and high victim count scores MEDIUM")
        void lotteryProfile() {
            FraudRing ring = buildRing("LOTTERY_FRAUD", todayMinus(10), 4_200_000, 31, 2, 2);
            double score = engine.calculateThreatScore(ring);
            assertThat(score).isBetween(4.0, 8.0);
        }

        @Test
        @DisplayName("Dormant ring with no mule accounts scores LOW (< 4.0)")
        void dormantProfile() {
            FraudRing ring = new FraudRing();
            ring.setRingId("TEST-LOW");
            ring.setFraudType("LOTTERY_FRAUD");
            ring.setLastActiveDate(todayMinus(120));
            ring.setMuleAccounts(Set.of());
            double score = engine.calculateThreatScore(ring);
            assertThat(score).isLessThan(4.0);
        }

        @Test
        @DisplayName("Score is always within [0.0, 10.0]")
        void scoreBounds() {
            // Extreme high
            FraudRing maxRing = buildRing("DIGITAL_ARREST", todayMinus(0), 100_000_000, 500, 20, 20);
            assertThat(engine.calculateThreatScore(maxRing)).isLessThanOrEqualTo(10.0);

            // Extreme low
            FraudRing minRing = buildRing("LOTTERY_FRAUD", todayMinus(365), 0, 0, 0, 0);
            assertThat(engine.calculateThreatScore(minRing)).isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("Full breakdown DTO contains all required fields")
        void breakdownFields() {
            FraudRing ring = buildRing("INVESTMENT_FRAUD", todayMinus(5), 35_000_000, 23, 2, 2);
            ring.setRingId("RING-TEST-01");
            ring.setLocationName("Test Location");

            ThreatScoreBreakdown breakdown = engine.score(ring, 7.0);

            assertThat(breakdown.getRingId()).isEqualTo("RING-TEST-01");
            assertThat(breakdown.getThreatScore()).isBetween(0.0, 10.0);
            assertThat(breakdown.getRiskLevel()).isIn("CRITICAL", "HIGH", "MEDIUM", "LOW");
            assertThat(breakdown.getScoredAt()).isNotNull();
            assertThat(breakdown.getKeyDrivers()).isNotEmpty();
            assertThat(breakdown.getRecommendedAction()).isNotNull();
            assertThat(breakdown.getScoreDelta()).isNotNull(); // previousScore was provided
        }

        @Test
        @DisplayName("scoreDelta is null when no previous score is provided")
        void nullDeltaWhenNoHistory() {
            FraudRing ring = buildRing("JOB_FRAUD", todayMinus(15), 6_700_000, 18, 2, 2);
            ring.setRingId("RING-NEW");
            ThreatScoreBreakdown breakdown = engine.score(ring, null);
            assertThat(breakdown.getScoreDelta()).isNull();
        }

        @Test
        @DisplayName("Risk band thresholds are correct")
        void riskBands() {
            assertThat(engine.riskBand(9.0)).isEqualTo("CRITICAL");
            assertThat(engine.riskBand(8.0)).isEqualTo("CRITICAL");
            assertThat(engine.riskBand(7.9)).isEqualTo("HIGH");
            assertThat(engine.riskBand(6.0)).isEqualTo("HIGH");
            assertThat(engine.riskBand(5.9)).isEqualTo("MEDIUM");
            assertThat(engine.riskBand(4.0)).isEqualTo("MEDIUM");
            assertThat(engine.riskBand(3.9)).isEqualTo("LOW");
            assertThat(engine.riskBand(0.0)).isEqualTo("LOW");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Builds a FraudRing with a single mule account for composite testing. */
    private FraudRing buildRing(String fraudType, String lastActiveDate,
                                 double moneyFlow, int victims,
                                 int muleCount, int confirmedCount) {
        FraudRing ring = new FraudRing();
        ring.setFraudType(fraudType);
        ring.setLastActiveDate(lastActiveDate);

        java.util.Set<MuleAccount> mules = new java.util.HashSet<>();
        // Distribute money and victims across requested mule count
        for (int i = 0; i < muleCount; i++) {
            MuleAccount m = new MuleAccount();
            m.setAccountNumber("TEST-ACC-" + i);
            m.setTotalMoneyFlow(muleCount > 0 ? moneyFlow / muleCount : 0);
            m.setVictimCount(muleCount > 0 ? victims / muleCount : 0);
            m.setConfirmed(i < confirmedCount);
            mules.add(m);
        }
        ring.setMuleAccounts(mules);
        return ring;
    }

    private String todayMinus(int days) {
        return LocalDate.now().minusDays(days).toString();
    }
}
