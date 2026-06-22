package com.fraudshield;

import com.fraudshield.model.graph.BankAccount;
import com.fraudshield.model.graph.PhoneNumber;
import com.fraudshield.model.graph.Victim;
import com.fraudshield.repository.PhoneNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Seeds "Operation Suraksha" demo data into Neo4j on startup.
 *
 * <p>Only runs in the {@code seed} Spring profile to avoid polluting production.
 * Activate with: {@code --spring.profiles.active=seed}
 *
 * <p>The seed creates:
 * <ul>
 *   <li>4 scammer phone numbers (Jamtara digital arrest ring)</li>
 *   <li>6 bank mule accounts</li>
 *   <li>8 victims (2 reported, 6 unreported — the system finds the unreported ones)</li>
 *   <li>CALLED, VICTIMIZED, and RECEIVED_MONEY relationships</li>
 * </ul>
 */
/**
 * @deprecated Superseded by {@link com.fraudshield.config.DataSeeder} (Phase 2).
 * That seeder runs automatically on every startup and seeds FraudRing + MuleAccount nodes.
 * This class seeds low-level PhoneNumber/Victim/BankAccount nodes for the Graph Intelligence
 * component and can be activated independently with {@code --spring.profiles.active=legacy-seed}.
 */
@Deprecated(since = "Phase 2")
@Component
@Profile("legacy-seed")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final PhoneNumberRepository phoneNumberRepository;

    @Override
    public void run(String... args) {
        log.info("=== Seeding 'Operation Suraksha' demo data into Neo4j ===");

        // ── Mule bank accounts ────────────────────────────────────────────
        BankAccount mule1 = BankAccount.builder()
                .accountHash("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4")
                .bankName("SBI").transactionCount(14).isMule(true).state("Jharkhand").build();

        BankAccount mule2 = BankAccount.builder()
                .accountHash("b2c3d4e5f6a7b2c3d4e5f6a7b2c3d4e5")
                .bankName("HDFC").transactionCount(9).isMule(true).state("West Bengal").build();

        BankAccount mule3 = BankAccount.builder()
                .accountHash("c3d4e5f6a7b8c3d4e5f6a7b8c3d4e5f6")
                .bankName("PNB").transactionCount(7).isMule(true).state("Bihar").build();

        // ── Victims ──────────────────────────────────────────────────────
        Victim v1 = Victim.builder().victimId("VIC-2024-001").city("Delhi").amountLost(250000.0)
                .reported(true).incidentDate("2024-09-12")
                .bankAccounts(Set.of(mule1)).build();

        Victim v2 = Victim.builder().victimId("VIC-2024-002").city("Dwarka, Delhi").amountLost(180000.0)
                .reported(true).incidentDate("2024-09-14")
                .bankAccounts(Set.of(mule1)).build();

        Victim v3 = Victim.builder().victimId("VIC-2024-003").city("Noida").amountLost(320000.0)
                .reported(false).incidentDate("2024-09-18")
                .bankAccounts(Set.of(mule2)).build();

        Victim v4 = Victim.builder().victimId("VIC-2024-004").city("Gurgaon").amountLost(95000.0)
                .reported(false).incidentDate("2024-09-20")
                .bankAccounts(Set.of(mule2)).build();

        Victim v5 = Victim.builder().victimId("VIC-2024-005").city("Faridabad").amountLost(410000.0)
                .reported(false).incidentDate("2024-09-22")
                .bankAccounts(Set.of(mule3)).build();

        Victim v6 = Victim.builder().victimId("VIC-2024-006").city("Ghaziabad").amountLost(175000.0)
                .reported(false).incidentDate("2024-09-25")
                .bankAccounts(Set.of(mule1, mule3)).build();

        // ── Scammer phone numbers ─────────────────────────────────────────
        PhoneNumber scammer2 = PhoneNumber.builder()
                .number("+917654321098").riskLevel("HIGH")
                .complaintCount(4).registeredLocation("Jamtara, Jharkhand").isBurner(true)
                .victims(Set.of(v3, v4)).build();

        PhoneNumber scammer3 = PhoneNumber.builder()
                .number("+916543210987").riskLevel("HIGH")
                .complaintCount(3).registeredLocation("Jamtara, Jharkhand").isBurner(true)
                .victims(Set.of(v5)).build();

        PhoneNumber scammer4 = PhoneNumber.builder()
                .number("+915432109876").riskLevel("MEDIUM")
                .complaintCount(2).registeredLocation("Deoghar, Jharkhand").isBurner(true)
                .victims(Set.of(v6)).build();

        // Master scammer — connects to all others via CALLED
        PhoneNumber masterScammer = PhoneNumber.builder()
                .number("+918765432109").riskLevel("CRITICAL")
                .complaintCount(12).registeredLocation("Jamtara, Jharkhand").isBurner(true)
                .victims(Set.of(v1, v2))
                .calledNumbers(Set.of(scammer2, scammer3, scammer4))
                .build();

        // Save — Spring Data Neo4j cascades to related nodes
        phoneNumberRepository.save(masterScammer);

        log.info("✅ Operation Suraksha seeded — 4 scammer nodes, 6 victims, 3 mule accounts");
        log.info("   Master scammer: +918765432109 → search this in the Graph Intelligence UI");
    }
}
