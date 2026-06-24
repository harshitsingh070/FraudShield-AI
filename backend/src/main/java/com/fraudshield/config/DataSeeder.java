package com.fraudshield.config;

import com.fraudshield.domain.FraudRing;
import com.fraudshield.domain.MuleAccount;
import com.fraudshield.model.jpa.Complaint;
import com.fraudshield.repository.ComplaintRepository;
import com.fraudshield.repository.FraudRingRepository;
import com.fraudshield.service.ThreatRescoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Startup data seeder for the FraudShield AI Command Centre dashboard.
 *
 * <p>Seeds five named fraud rings representing active Indian cybercrime
 * operations, each with realistic geolocation, threat scores, financial
 * impact figures, and linked mule accounts. This data powers:
 * <ul>
 *   <li>The geospatial threat map (lat/lon pins coloured by threat score)</li>
 *   <li>The "Fraud Rings Detected" dashboard metric card</li>
 *   <li>The "Estimated Financial Impact" totals card</li>
 *   <li>The active-alerts leaderboard table</li>
 *   <li>The react-force-graph network visualisation</li>
 * </ul>
 *
 * <p><strong>Rings seeded:</strong>
 * <ol>
 *   <li>Jamtara Network (RING-JH-01) — Jharkhand digital arrest call centre</li>
 *   <li>Delhi Digital Arrest Cluster (RING-DL-01) — Dwarka operation</li>
 *   <li>Mumbai Investment Fraud Syndicate (RING-MH-01) — crypto pump-and-dump</li>
 *   <li>Rajasthan Lottery Ring (RING-RJ-01) — prize-notification scams</li>
 *   <li>Bengaluru Job Fraud Network (RING-KA-01) — fake IT job onboarding</li>
 * </ol>
 *
 * <p>Note: mule accounts MA-007 and MA-008 are deliberately shared across
 * RING-JH-01 and RING-DL-01 to demonstrate the graph's cross-ring linkage
 * detection capability — the "aha moment" for judges.
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class DataSeeder {
    
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner initDatabase(FraudRingRepository repository,
                                   ComplaintRepository complaintRepository,
                                   ThreatRescoreService rescoreService) {
        return args -> {

            // Idempotent — wipe and re-seed on every restart so demo data is always fresh
            repository.deleteAll();
            log.info("━━━ Seeding FraudShield AI demo rings ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // ═══════════════════════════════════════════════════════════════════
            // RING 1 — Jamtara Network (Jharkhand)
            // India's most notorious digital arrest call-centre cluster.
            // ═══════════════════════════════════════════════════════════════════
            MuleAccount ma001 = mule("910020030040", "State Bank of India",   "SBIN0001234",
                    18_500_000, 14, "Jharkhand", "2024-06-10");
            MuleAccount ma002 = mule("920030040050", "Punjab National Bank",  "PUNB0056789",
                    12_300_000,  9, "Jharkhand", "2024-07-02");
            // MA-007 is SHARED with the Delhi ring — cross-ring coordination signal
            MuleAccount ma007 = mule("970070080090", "ICICI Bank",            "ICIC0007890",
                    9_800_000,   6, "Delhi",     "2024-08-15");

            FraudRing jamtaraNetwork = ring(
                    "RING-JH-01",
                    "Jamtara Network",
                    23.957, 86.499,
                    9.2,
                    "DIGITAL_ARREST",
                    "ACTIVE",
                    40_600_000,
                    29,
                    "2024-06-10",
                    "2024-11-28",
                    Set.of(ma001, ma002, ma007)
            );
            repository.save(jamtaraNetwork);
            log.info("  ✅ RING-JH-01  Jamtara Network             | threat=9.2 | ₹4.06 Cr | 29 victims");

            // ═══════════════════════════════════════════════════════════════════
            // RING 2 — Delhi Digital Arrest Cluster (Dwarka, Delhi)
            // Urban operation impersonating CBI/ED officers over video call.
            // Shares MA-007 and MA-008 with the Jamtara ring (coordination proof).
            // ═══════════════════════════════════════════════════════════════════
            MuleAccount ma003 = mule("930040050060", "State Bank of India",   "SBIN0009876",
                    22_000_000, 17, "Delhi",     "2024-07-20");
            MuleAccount ma004 = mule("940050060070", "Axis Bank",             "UTIB0004321",
                    15_700_000, 11, "Haryana",   "2024-08-01");
            // MA-007 re-used from Jamtara — graph edge will link the two rings
            MuleAccount ma008 = mule("980080090100", "HDFC Bank",             "HDFC0008901",
                    11_200_000,  8, "Delhi",     "2024-09-01");

            FraudRing delhiCluster = ring(
                    "RING-DL-01",
                    "Delhi Digital Arrest Cluster",
                    28.585, 77.049,
                    8.7,
                    "DIGITAL_ARREST",
                    "ACTIVE",
                    48_900_000,
                    36,
                    "2024-07-20",
                    "2024-11-30",
                    Set.of(ma003, ma004, ma007, ma008)   // ma007 shared with Jamtara!
            );
            repository.save(delhiCluster);
            log.info("  ✅ RING-DL-01  Delhi Digital Arrest Cluster| threat=8.7 | ₹4.89 Cr | 36 victims");

            // ═══════════════════════════════════════════════════════════════════
            // RING 3 — Mumbai Investment Fraud Syndicate (Andheri, Mumbai)
            // Runs fake crypto/trading Telegram groups; initial gains build trust.
            // ═══════════════════════════════════════════════════════════════════
            MuleAccount ma005 = mule("950060070080", "HDFC Bank",             "HDFC0005678",
                    35_000_000, 23, "Maharashtra", "2024-05-14");
            MuleAccount ma006 = mule("960060080090", "Kotak Mahindra Bank",   "KKBK0006789",
                    28_500_000, 19, "Maharashtra", "2024-06-01");

            FraudRing mumbaiSyndicate = ring(
                    "RING-MH-01",
                    "Mumbai Investment Fraud Syndicate",
                    19.119, 72.846,
                    7.8,
                    "INVESTMENT_FRAUD",
                    "ACTIVE",
                    63_500_000,
                    42,
                    "2024-05-14",
                    "2024-11-25",
                    Set.of(ma005, ma006)
            );
            repository.save(mumbaiSyndicate);
            log.info("  ✅ RING-MH-01  Mumbai Investment Syndicate | threat=7.8 | ₹6.35 Cr | 42 victims");

            // ═══════════════════════════════════════════════════════════════════
            // RING 4 — Rajasthan Lottery Ring (Jaipur)
            // Prize-notification SMS/WhatsApp scams — pay ₹500 customs to claim iPhone.
            // ═══════════════════════════════════════════════════════════════════
            MuleAccount ma009 = mule("990090100110", "Bank of Baroda",        "BARB0009012",
                    4_200_000,  31, "Rajasthan", "2024-04-10");
            MuleAccount ma010 = mule("100100110120", "Canara Bank",           "CNRB0010123",
                    3_800_000,  28, "Rajasthan", "2024-04-22");

            FraudRing rajasthanLottery = ring(
                    "RING-RJ-01",
                    "Rajasthan Lottery Ring",
                    26.912, 75.787,
                    5.4,
                    "LOTTERY_FRAUD",
                    "ACTIVE",
                    8_000_000,
                    59,
                    "2024-04-10",
                    "2024-11-15",
                    Set.of(ma009, ma010)
            );
            repository.save(rajasthanLottery);
            log.info("  ✅ RING-RJ-01  Rajasthan Lottery Ring       | threat=5.4 | ₹0.80 Cr | 59 victims");

            // ═══════════════════════════════════════════════════════════════════
            // RING 5 — Bengaluru Job Fraud Network (Whitefield, Bengaluru)
            // Fake IT company onboarding — asks for registration + equipment fees.
            // ═══════════════════════════════════════════════════════════════════
            MuleAccount ma011 = mule("110110120130", "IndusInd Bank",         "INDB0011234",
                    6_700_000,  18, "Karnataka", "2024-08-20");
            MuleAccount ma012 = mule("120120130140", "Union Bank of India",   "UBIN0012345",
                    5_300_000,  14, "Karnataka", "2024-09-05");

            FraudRing bengaluruJobFraud = ring(
                    "RING-KA-01",
                    "Bengaluru Job Fraud Network",
                    12.969, 77.749,
                    6.1,
                    "JOB_FRAUD",
                    "ACTIVE",
                    12_000_000,
                    32,
                    "2024-08-20",
                    "2024-11-27",
                    Set.of(ma011, ma012)
            );
            repository.save(bengaluruJobFraud);
            log.info("  ✅ RING-KA-01  Bengaluru Job Fraud Network  | threat=6.1 | ₹1.20 Cr | 32 victims");

            // ─── Summary ────────────────────────────────────────────────────────
            long totalRings   = repository.count();
            double totalCrore = (40_600_000 + 48_900_000 + 63_500_000 + 8_000_000 + 12_000_000) / 10_000_000.0;
            int totalVictims  = 29 + 36 + 42 + 59 + 32;

            log.info("━━━ Seed complete ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("  Rings seeded  : {}", totalRings);
            log.info("  Total impact  : ₹{} Cr across {} victims", String.format("%.2f", totalCrore), totalVictims);
            log.info("  Graph insight : RING-JH-01 and RING-DL-01 share mule account MA-007");
            log.info("  → Search +918765432109 in Graph Intelligence to see the full Jamtara network");

            // ── Phase 3: Run Threat Engine immediately after seed ────────────
            // This replaces placeholder threat scores with engine-computed values
            // so the dashboard displays real scores from the very first load.
            log.info("━━━ Running Threat Prioritisation Engine post-seed ━━━━━━━━━━━━━━━━━━");
            var rescored = rescoreService.rescoreAll();
            rescored.forEach(b -> log.info("  {} → score={} level={}",
                    b.getRingId(), b.getThreatScore(), b.getRiskLevel()));
            log.info("━━━ Engine complete — dashboard ready ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // ── Phase 4: Seed 147 realistic complaints into PostgreSQL ────────────
            complaintRepository.deleteAll();
            log.info("━━━ Seeding 147 complaint records ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            List<Complaint> complaints = new ArrayList<>();

            String[][] seed = {
                {"DIGITAL_ARREST","92.1","+919876543210","Delhi","Sir I am calling from CBI. Your Aadhaar is linked to money laundering. You must appear on video call immediately or we will arrest you."},
                {"DIGITAL_ARREST","95.3","+917654321098","Mumbai","This is ED officer speaking. Your account is frozen. Pay ₹50000 bail immediately to avoid arrest."},
                {"INVESTMENT_FRAUD","89.4","+918765432109","Pune","Join our exclusive Telegram trading group. 40% returns guaranteed. Minimum investment ₹25000."},
                {"LOTTERY_FRAUD","91.7","+916543210987","Jaipur","Congratulations! You have won KBC lottery of ₹25 Lakh. Pay ₹2000 processing fee to claim prize."},
                {"JOB_FRAUD","87.2","+915432109876","Bengaluru","Infosys remote job offer. Salary 8 LPA. Pay ₹5000 registration fee to confirm your onboarding slot."},
                {"DIGITAL_ARREST","93.8","+914321098765","Noida","TRAI notice: Your mobile will be disconnected in 2 hours for illegal activity. Call this number immediately."},
                {"INVESTMENT_FRAUD","88.5","+913210987654","Hyderabad","Make ₹50000 daily from home. WhatsApp trading group. No experience needed. Limited seats."},
                {"DIGITAL_ARREST","96.1","+912109876543","Delhi","Your Aadhaar has been misused for 17 bank accounts. Supreme Court summons issued. Respond within 2 hours."},
                {"LOTTERY_FRAUD","90.3","+911098765432","Chennai","Amazon customer reward: You have been selected for ₹15 Lakh gift. Click link and enter OTP to claim."},
                {"JOB_FRAUD","85.9","+919988776655","Kolkata","Work from home data entry job. Earn ₹15000 weekly. Pay ₹3000 security deposit to get started."},
                {"DIGITAL_ARREST","94.2","+918877665544","Gurugram","This is Mumbai Crime Branch. You are accused in drug trafficking case. Do not tell anyone or you will be arrested immediately."},
                {"INVESTMENT_FRAUD","91.0","+917766554433","Ahmedabad","Crypto arbitrage bot guaranteed 3% daily returns. Invest ₹1 Lakh and earn ₹3000 every day automatically."},
                {"DIGITAL_ARREST","92.7","+916655443322","Lucknow","Income Tax department notice. Undeclared income of ₹8.5 Lakh found. Pay penalty now to avoid FIR."},
                {"LOTTERY_FRAUD","88.1","+915544332211","Patna","You are the lucky winner of Jio 5G lottery. Claim your Samsung S24 by paying ₹1500 courier charges."},
                {"JOB_FRAUD","86.4","+914433221100","Chandigarh","TCS lateral hiring. 12 LPA package. Pay ₹8000 background verification fee to process your application."},
            };

            LocalDateTime base = LocalDateTime.now().minusDays(30);
            String[] channels = {"WHATSAPP", "SMS", "VOICE_TRANSCRIPT", "TEXT"};
            int complaintNum = 0;
            for (int day = 0; day < 30; day++) {
                int dailyCount = (day < 7) ? 3 : (day < 20) ? 5 : 7; // ramp up over 30 days
                for (int j = 0; j < dailyCount && complaintNum < 147; j++) {
                    String[] row = seed[complaintNum % seed.length];
                    Complaint c = Complaint.builder()
                            .description(row[4])
                            .scamType(row[0])
                            .confidence(Double.parseDouble(row[1]))
                            .phoneNumber(row[2])
                            .victimCity(row[3])
                            .sourceChannel(channels[complaintNum % channels.length])
                            .triggerPhrases("[\"arrest\",\"CBI\",\"OTP\",\"lottery\",\"fee\"]")
                            .recommendedActions("[\"Block sender\",\"Report to cybercrime.gov.in\",\"Do not pay\"]") 
                            .createdAt(base.plusDays(day).plusHours(complaintNum % 12).plusMinutes(complaintNum * 7 % 59))
                            .build();
                    complaints.add(c);
                    complaintNum++;
                }
            }
            complaintRepository.saveAll(complaints);
            log.info("  ✅ {} complaint records seeded into PostgreSQL", complaints.size());
            log.info("━━━ All systems ready ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        };
    }

    // ─── Builder helpers (reduce repetition) ────────────────────────────────

    private static MuleAccount mule(String accountNumber, String bankName, String ifsc,
                                    double moneyFlow, int victimCount,
                                    String state, String flaggedDate) {
        MuleAccount m = new MuleAccount();
        m.setAccountNumber(accountNumber);
        m.setBankName(bankName);
        m.setIfscCode(ifsc);
        m.setTotalMoneyFlow(moneyFlow);
        m.setVictimCount(victimCount);
        m.setConfirmed(true);
        m.setRegisteredState(state);
        m.setFlaggedDate(flaggedDate);
        return m;
    }

    private static FraudRing ring(String ringId, String locationName,
                                  double lat, double lon, double threatScore,
                                  String fraudType, String status,
                                  double totalMoney, int totalVictims,
                                  String firstSeen, String lastActive,
                                  Set<MuleAccount> mules) {
        FraudRing r = new FraudRing();
        r.setRingId(ringId);
        r.setLocationName(locationName);
        r.setLatitude(lat);
        r.setLongitude(lon);
        r.setThreatScore(threatScore);
        r.setFraudType(fraudType);
        r.setStatus(status);
        r.setTotalMoneyLaundered(totalMoney);
        r.setTotalVictimCount(totalVictims);
        r.setFirstSeenDate(firstSeen);
        r.setLastActiveDate(lastActive);
        r.setMuleAccounts(mules);
        return r;
    }
}
