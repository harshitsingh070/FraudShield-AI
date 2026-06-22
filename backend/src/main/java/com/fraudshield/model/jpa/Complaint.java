package com.fraudshield.model.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity that persists each fraud analysis request to the relational
 * database (Neon/PostgreSQL). Maps to the {@code complaints} table.
 */
@Entity
@Table(name = "complaints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Raw user-submitted description (text or audio transcript). */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    /**
     * Gemini-classified scam type:
     * DIGITAL_ARREST | INVESTMENT_FRAUD | LOTTERY_FRAUD | JOB_FRAUD |
     * ROMANCE_FRAUD | LEGITIMATE | UNKNOWN
     */
    @Column(name = "scam_type", length = 50)
    private String scamType;

    /** Classification confidence percentage (0–100). */
    @Column(name = "confidence")
    private Double confidence;

    /** JSON array string of extracted trigger phrases. */
    @Column(name = "trigger_phrases", columnDefinition = "TEXT")
    private String triggerPhrases;

    /** Recommended actions JSON array (for the citizen UI). */
    @Column(name = "recommended_actions", columnDefinition = "TEXT")
    private String recommendedActions;

    /** Source channel: TEXT | AUDIO | API */
    @Column(name = "source_channel", length = 20)
    private String sourceChannel;

    /** Phone number associated with the complaint (if provided). */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /** City of the victim (if known). */
    @Column(name = "victim_city", length = 100)
    private String victimCity;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
