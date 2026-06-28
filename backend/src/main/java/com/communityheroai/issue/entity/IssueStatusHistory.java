package com.communityheroai.issue.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "issue_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private IssueStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IssueStatus toStatus;
    @Column(nullable = false, length = 100)
    private String actorName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusActorType actorType;
    @Column(nullable = false, length = 1000)
    private String note;
    @Column(length = 500)
    private String evidenceUrl;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
