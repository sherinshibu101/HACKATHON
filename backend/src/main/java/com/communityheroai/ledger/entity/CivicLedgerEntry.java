package com.communityheroai.ledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "civic_audit_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CivicLedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String eventType;
    @Column(nullable = false, length = 60)
    private String aggregateType;
    @Column(nullable = false)
    private Long aggregateId;
    @Column(nullable = false, length = 120)
    private String actorName;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Column(nullable = false, length = 64)
    private String previousHash;
    @Column(nullable = false, length = 64)
    private String entryHash;
    @Column(length = 20)
    private String hashAlgorithm;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
