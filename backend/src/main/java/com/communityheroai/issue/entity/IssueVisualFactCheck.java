package com.communityheroai.issue.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "issue_visual_fact_checks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueVisualFactCheck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_media_id")
    private IssueMedia issueMedia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VisualFactCheckStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private VisualVerificationResult verificationResult;

    private Integer confidenceScore;
    @Column(length = 1000)
    private String baselineImageUrl;
    @Column(length = 500)
    private String userImageUrl;
    @Column(columnDefinition = "TEXT")
    private String reasoningReport;
    @Column(columnDefinition = "TEXT")
    private String riskFlags;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
