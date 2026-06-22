package com.fraudshield.repository;

import com.fraudshield.model.jpa.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA repository for the {@link Complaint} entity (Neon/PostgreSQL).
 */
@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    /** Retrieve the N most recent complaints for the dashboard feed. */
    List<Complaint> findTop5ByOrderByCreatedAtDesc();

    /** Count complaints created today (for the dashboard metric card). */
    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.createdAt >= :startOfDay")
    long countTodayComplaints(LocalDateTime startOfDay);

    /** Find all complaints linked to a specific phone number. */
    List<Complaint> findByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);
}
