package com.fraudshield.service;

import com.fraudshield.domain.FraudRing;
import com.fraudshield.domain.MuleAccount;
import com.fraudshield.dto.ThreatScoreBreakdown;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <h2>Threat Prioritisation Engine</h2>
 *
 * <p>Computes a composite threat score for a {@link FraudRing} across five
 * independently weighted dimensions. The score drives dashboard colour-coding,
 * alert ordering, and law-enforcement resource allocation.
 *
 * <h3>Scoring Model (total weight = 100%)</h3>
 * <pre>
 * ┌──────────────────────────┬────────┬──────────────────────────────────────────┐
 * │ Dimension                │ Weight │ Rationale                                │
 * ├──────────────────────────┼────────┼──────────────────────────────────────────┤
 * │ 1. Financial Impact      │  30 %  │ Money laundered is the primary harm       │
 * │ 2. Victim Volume         │  25 %  │ Victim count signals operational scale    │
 * │ 3. Network Complexity    │  20 %  │ More mule accounts = harder to dismantle  │
 * │ 4. Recency               │  15 %  │ Active rings need immediate attention     │
 * │ 5. Fraud Type Severity   │  10 %  │ Digital arrest causes greatest harm/fear  │
 * └──────────────────────────┴────────┴──────────────────────────────────────────┘
 * </pre>
 *
 * <p>Each dimension is independently normalised to [0, 10], multiplied by its
 * weight fraction, and summed. The final score is clamped to [0.0, 10.0].
 *
 * <h3>Risk Bands</h3>
 * <pre>
 *   ≥ 8.0  → CRITICAL  (immediate interdiction recommended)
 *   6.0–8.0 → HIGH     (escalate to state cybercrime cell)
 *   4.0–6.0 → MEDIUM   (monitor and gather intelligence)
 *   0.0–4.0 → LOW      (log and review quarterly)
 * </pre>
 *
 * <h3>Calibration baselines</h3>
 * <ul>
 *   <li>Financial: ₹2 Cr per mule account = score 10 (typical large ring)</li>
 *   <li>Victims: 20 victims = score 10</li>
 *   <li>Complexity: 4 confirmed mule accounts = fully complex</li>
 *   <li>Recency: active within 7 days = maximum urgency</li>
 * </ul>
 */
@Service
@Slf4j
public class ThreatEngineService {

    // ── Scoring weights (must sum to 1.0) ────────────────────────────────────
    private static final double W_FINANCIAL  = 0.30;
    private static final double W_VICTIM     = 0.25;
    private static final double W_COMPLEXITY = 0.20;
    private static final double W_RECENCY    = 0.15;
    private static final double W_FRAUD_TYPE = 0.10;

    // ── Calibration constants ─────────────────────────────────────────────────
    /** INR amount that maps to a raw component score of 10 (₹2 Cr) */
    private static final double FINANCIAL_CEILING_INR = 20_000_000.0;

    /** Victim count that maps to a raw component score of 10 */
    private static final int VICTIM_CEILING = 20;

    /** Mule account count considered "fully complex" */
    private static final int COMPLEXITY_CEILING_ACCOUNTS = 4;

    /** Days of inactivity at which recency score drops to 0 */
    private static final int RECENCY_STALE_DAYS = 90;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Computes the composite threat score for a ring.
     *
     * <p>Convenience overload — returns only the final score value.
     * Use {@link #score(FraudRing, Double)} for the full auditable breakdown.
     *
     * @param ring the FraudRing node (must have muleAccounts populated)
     * @return composite score clamped to [0.0, 10.0]
     */
    public double calculateThreatScore(FraudRing ring) {
        return score(ring, null).getThreatScore();
    }

    /**
     * Computes and returns the full auditable {@link ThreatScoreBreakdown}.
     *
     * @param ring          the FraudRing to score
     * @param previousScore the ring's previous score (null if first computation);
     *                      used to populate the {@code score_delta} field
     * @return complete breakdown including component scores and key drivers
     */
    public ThreatScoreBreakdown score(FraudRing ring, Double previousScore) {
        Set<MuleAccount> mules = ring.getMuleAccounts();
        if (mules == null) mules = Set.of();

        // ── Raw aggregates ────────────────────────────────────────────────────
        double totalFunds  = mules.stream().mapToDouble(MuleAccount::getTotalMoneyFlow).sum();
        int    totalVictims = mules.stream().mapToInt(MuleAccount::getVictimCount).sum();
        int    muleCount   = mules.size();
        long   confirmedCount = mules.stream().filter(MuleAccount::isConfirmed).count();
        long   daysSinceActive = daysSinceLastActive(ring.getLastActiveDate());

        // ── Component scores (each normalised 0 → 10) ─────────────────────────
        double financialScore  = financialImpactScore(totalFunds);
        double victimScore     = victimVolumeScore(totalVictims);
        double complexityScore = networkComplexityScore(muleCount, confirmedCount);
        double recencyScore    = recencyScore(daysSinceActive);
        double fraudTypeScore  = fraudTypeSeverityScore(ring.getFraudType());

        // ── Weighted sum → composite score on 0–10 scale ──────────────────────
        double composite = (financialScore  * W_FINANCIAL)
                         + (victimScore     * W_VICTIM)
                         + (complexityScore * W_COMPLEXITY)
                         + (recencyScore    * W_RECENCY)
                         + (fraudTypeScore  * W_FRAUD_TYPE);

        double finalScore = clamp(composite, 0.0, 10.0);

        // ── Risk level band ───────────────────────────────────────────────────
        String riskLevel = riskBand(finalScore);

        // ── Key drivers narrative ─────────────────────────────────────────────
        List<String> drivers = buildKeyDrivers(
                financialScore, victimScore, complexityScore,
                recencyScore, fraudTypeScore,
                totalFunds, totalVictims, muleCount, daysSinceActive);

        // ── Recommended action ────────────────────────────────────────────────
        String action = recommendedAction(riskLevel);

        // ── Score delta ───────────────────────────────────────────────────────
        Double delta = (previousScore != null)
                ? Math.round((finalScore - previousScore) * 100.0) / 100.0
                : null;

        log.debug("Scored {} → {:.2f} ({}) | Δ={}", ring.getRingId(), finalScore, riskLevel, delta);

        return ThreatScoreBreakdown.builder()
                .ringId(ring.getRingId())
                .locationName(ring.getLocationName())
                .threatScore(Math.round(finalScore * 100.0) / 100.0)
                .riskLevel(riskLevel)
                .scoredAt(Instant.now().toString())
                .financialImpactScore(Math.round(financialScore  * 10.0) / 10.0)
                .victimVolumeScore(Math.round(victimScore     * 10.0) / 10.0)
                .networkComplexityScore(Math.round(complexityScore * 10.0) / 10.0)
                .recencyScore(Math.round(recencyScore    * 10.0) / 10.0)
                .fraudTypeSeverityScore(Math.round(fraudTypeScore  * 10.0) / 10.0)
                .totalMoneyFlowInr(totalFunds)
                .totalVictims(totalVictims)
                .muleAccountCount(muleCount)
                .confirmedMuleCount(confirmedCount)
                .daysSinceLastActive(daysSinceActive)
                .fraudType(ring.getFraudType())
                .scoreDelta(delta)
                .keyDrivers(drivers)
                .recommendedAction(action)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Component scoring functions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dimension 1 — Financial Impact (weight 30%)
     *
     * <p>Uses a square-root curve so moderate rings aren't unfairly penalised
     * but extreme outliers (>₹2 Cr) are still capped at 10.
     *
     * <pre>
     *   score = min( sqrt(totalFunds / CEILING) × 10, 10 )
     * </pre>
     */
    double financialImpactScore(double totalFundsInr) {
        if (totalFundsInr <= 0) return 0.0;
        double raw = Math.sqrt(totalFundsInr / FINANCIAL_CEILING_INR) * 10.0;
        return clamp(raw, 0.0, 10.0);
    }

    /**
     * Dimension 2 — Victim Volume (weight 25%)
     *
     * <p>Linear scale: 20 victims → score 10. Beyond 20 the score is capped.
     *
     * <pre>
     *   score = min( (victims / CEILING) × 10, 10 )
     * </pre>
     */
    double victimVolumeScore(int totalVictims) {
        if (totalVictims <= 0) return 0.0;
        double raw = ((double) totalVictims / VICTIM_CEILING) * 10.0;
        return clamp(raw, 0.0, 10.0);
    }

    /**
     * Dimension 3 — Network Complexity (weight 20%)
     *
     * <p>Combines total mule count (60%) and confirmed-to-total ratio (40%).
     * A ring with 4 confirmed accounts is maximally complex. The ratio bonus
     * rewards intelligence quality — confirmed accounts are harder evidence.
     *
     * <pre>
     *   countComponent  = min( (count / CEILING) × 10, 10 ) × 0.6
     *   ratioComponent  = (confirmed / total) × 10 × 0.4   [0 if total == 0]
     *   score           = countComponent + ratioComponent
     * </pre>
     */
    double networkComplexityScore(int muleCount, long confirmedCount) {
        if (muleCount == 0) return 0.0;
        double countComponent = clamp(((double) muleCount / COMPLEXITY_CEILING_ACCOUNTS) * 10.0, 0, 10) * 0.6;
        double ratioComponent = ((double) confirmedCount / muleCount) * 10.0 * 0.4;
        return clamp(countComponent + ratioComponent, 0.0, 10.0);
    }

    /**
     * Dimension 4 — Recency (weight 15%)
     *
     * <p>Exponential decay: a ring active today scores 10; a ring last seen
     * 90 days ago scores 0. Rings with no date string score 5 (unknown).
     *
     * <pre>
     *   score = 10 × e^( -3 × daysSinceActive / STALE_DAYS )
     * </pre>
     * The factor of -3 gives:
     *   0 days  → 10.0
     *   7 days  →  7.8
     *   30 days →  3.7
     *   60 days →  1.4
     *   90 days →  0.5
     */
    double recencyScore(long daysSinceActive) {
        if (daysSinceActive < 0) return 5.0;   // unknown date → neutral
        double raw = 10.0 * Math.exp(-3.0 * daysSinceActive / RECENCY_STALE_DAYS);
        return clamp(raw, 0.0, 10.0);
    }

    /**
     * Dimension 5 — Fraud Type Severity (weight 10%)
     *
     * <p>Categorical multiplier reflecting the psychological and financial
     * harm profile of each scam type in the Indian context.
     *
     * <pre>
     *   DIGITAL_ARREST   → 10  (multi-day psychological hostage, largest losses)
     *   INVESTMENT_FRAUD → 8   (large losses, sophisticated deception)
     *   JOB_FRAUD        → 6   (targets unemployed youth, financial + emotional)
     *   ROMANCE_FRAUD    → 6   (long-term emotional manipulation)
     *   LOTTERY_FRAUD    → 4   (lower individual loss, higher victim count)
     *   default          → 5   (unknown type)
     * </pre>
     */
    double fraudTypeSeverityScore(String fraudType) {
        if (fraudType == null) return 5.0;
        return switch (fraudType.toUpperCase()) {
            case "DIGITAL_ARREST"   -> 10.0;
            case "INVESTMENT_FRAUD" ->  8.0;
            case "JOB_FRAUD"        ->  6.0;
            case "ROMANCE_FRAUD"    ->  6.0;
            case "LOTTERY_FRAUD"    ->  4.0;
            default                 ->  5.0;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Risk band, drivers, and action
    // ─────────────────────────────────────────────────────────────────────────

    /** Maps a composite score to a named risk band. */
    public String riskBand(double score) {
        if (score >= 8.0) return "CRITICAL";
        if (score >= 6.0) return "HIGH";
        if (score >= 4.0) return "MEDIUM";
        return "LOW";
    }

    /** Generates a human-readable list of the top factors driving the score. */
    private List<String> buildKeyDrivers(
            double fin, double vic, double cmp, double rec, double fraud,
            double totalFunds, int totalVictims, int muleCount, long daysSince) {

        List<String> drivers = new ArrayList<>();

        if (fin >= 7.0)
            drivers.add(String.format("High financial impact: ₹%.2f Cr laundered", totalFunds / 10_000_000.0));
        if (vic >= 7.0)
            drivers.add(String.format("Large victim pool: %d known victims", totalVictims));
        if (cmp >= 7.0)
            drivers.add(String.format("Complex network: %d mule accounts identified", muleCount));
        if (rec >= 8.0)
            drivers.add(String.format("Actively operating: last seen %d day(s) ago", daysSince));
        if (fraud >= 8.0)
            drivers.add("High-severity fraud type (Digital Arrest) — causes psychological trauma");
        if (drivers.isEmpty())
            drivers.add("Moderate risk across multiple dimensions — continued monitoring recommended");

        return drivers;
    }

    /** Returns a law-enforcement action recommendation for a given risk level. */
    private String recommendedAction(String riskLevel) {
        return switch (riskLevel) {
            case "CRITICAL" -> "Immediate interdiction — escalate to I4C National Cyber Coordination Centre and state STF within 24 hours";
            case "HIGH"     -> "Escalate to state cybercrime cell — initiate account freeze and MLAT requests within 72 hours";
            case "MEDIUM"   -> "Assign dedicated investigator — gather additional intelligence and identify victims";
            default         -> "Log and monitor — review in next weekly threat assessment cycle";
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────────

    private long daysSinceLastActive(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return -1L;
        try {
            LocalDate lastActive = LocalDate.parse(isoDate);
            return ChronoUnit.DAYS.between(lastActive, LocalDate.now());
        } catch (DateTimeParseException ex) {
            log.warn("Could not parse lastActiveDate '{}' — using neutral recency", isoDate);
            return -1L;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
