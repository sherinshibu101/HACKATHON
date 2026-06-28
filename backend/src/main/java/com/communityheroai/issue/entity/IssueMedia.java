package com.communityheroai.issue.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "issue_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueMediaType mediaType;
    @Column(nullable = false, length = 500)
    private String mediaUrl;
    @Column(length = 500)
    private String thumbnailUrl;
    @Column(nullable = false, length = 100)
    private String storageKey;
    @Column(length = 255)
    private String originalFilename;
    @Column(nullable = false, length = 100)
    private String contentType;
    @Column(nullable = false)
    private Long fileSize;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaProcessingStatus processingStatus;
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ImageValidationStatus validationStatus;
    private Integer validationConfidence;
    @Column(length = 1000)
    private String validationSummary;
    @Column(columnDefinition = "TEXT")
    private String validationLabels;
    private LocalDateTime validatedAt;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (processingStatus == null) processingStatus = MediaProcessingStatus.READY;
        createdAt = LocalDateTime.now();
    }
}
