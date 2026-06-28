package com.communityheroai.issue.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String title;
    @Column(length = 100)
    private String reporterName;
    @Column(length = 254)
    private String reporterEmail;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueCategory category;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status;
    @Enumerated(EnumType.STRING)
    private IssueSeverity severity;
    @Column(nullable = false)
    private Double latitude;
    @Column(nullable = false)
    private Double longitude;
    @Column(nullable = false)
    private String ward;
    @Column(nullable = false)
    private String locality;
    private String country;
    private String state;
    private String district;
    private String city;
    private String postalCode;
    @Column(length = 1000)
    private String formattedAddress;
    private Double locationAccuracyMeters;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LocationSource locationSource;
    private String recommendedDepartment;
    private Integer impactScore;
    @Column(columnDefinition = "TEXT")
    private String riskExplanation;
    @Column(columnDefinition = "TEXT")
    private String suggestedAction;
    @Column(columnDefinition = "TEXT")
    private String complaintDraft;
    @Column(columnDefinition = "TEXT")
    private String escalationMessage;
    private String resolutionUrgency;
    @Column(name = "ai_generated_at")
    private LocalDateTime aiGeneratedAt;
    private LocalDateTime authorityEmailSentAt;
    @Column(length = 254)
    private String authorityEmailRecipient;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "dispatch_department")
    private String dispatchDepartment;

    @Column(name = "dispatch_priority")
    private String dispatchPriority;

    @Column(name = "dispatch_citizen_notification", columnDefinition = "TEXT")
    private String dispatchCitizenNotification;

    @Column(name = "dispatch_analyzed_at")
    private LocalDateTime dispatchAnalyzedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = IssueStatus.REPORTED;
        if (locationSource == null) locationSource = LocationSource.MANUAL;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
