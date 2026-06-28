package com.communityheroai.agent.entity;

import com.communityheroai.issue.entity.Issue;
import com.communityheroai.issue.entity.IssueStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agent_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AgentRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AgentTrigger triggerType;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(columnDefinition = "TEXT")
    private String citizenSummary;

    @Column(columnDefinition = "TEXT")
    private String adminRecommendation;

    @Column(columnDefinition = "TEXT")
    private String recommendedNextAction;

    @Column(length = 180)
    private String proposedDepartment;

    @Column(length = 30)
    private String proposedPriority;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private IssueStatus proposedStatus;

    private Integer targetResolutionHours;
    private Integer confidence;
    private boolean requiresHumanApproval;

    @Column(length = 1000)
    private String failureMessage;

    @Column(length = 100)
    private String reviewedBy;

    @Column(length = 1000)
    private String reviewNote;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime reviewedAt;

    @OneToMany(mappedBy = "agentRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber ASC")
    @Builder.Default
    private List<AgentRunStep> steps = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (status == null) status = AgentRunStatus.RUNNING;
        if (startedAt == null) startedAt = LocalDateTime.now();
    }
}
