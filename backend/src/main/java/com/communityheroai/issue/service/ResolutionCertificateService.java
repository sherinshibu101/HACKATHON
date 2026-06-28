package com.communityheroai.issue.service;

import com.communityheroai.exception.ResourceNotFoundException;
import com.communityheroai.issue.dto.IssueStatusHistoryResponse;
import com.communityheroai.issue.dto.ResolutionCertificateResponse;
import com.communityheroai.issue.entity.Issue;
import com.communityheroai.issue.entity.IssueSeverity;
import com.communityheroai.issue.entity.IssueStatus;
import com.communityheroai.issue.repository.IssueRepository;
import com.communityheroai.issue.repository.IssueVerificationRepository;
import com.communityheroai.ledger.dto.LedgerIntegrityResponse;
import com.communityheroai.ledger.entity.CivicLedgerEntry;
import com.communityheroai.ledger.repository.CivicLedgerRepository;
import com.communityheroai.ledger.service.CivicLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResolutionCertificateService {
    private final IssueRepository issueRepository;
    private final IssueVerificationRepository verificationRepository;
    private final IssueStatusHistoryService historyService;
    private final CivicLedgerService ledgerService;
    private final CivicLedgerRepository ledgerRepository;

    @Transactional(readOnly = true)
    public ResolutionCertificateResponse certificate(Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));
        List<IssueStatusHistoryResponse> timeline = historyService.responsesForIssue(issueId);
        IssueStatusHistoryResponse resolvedEvent = latestResolvedEvent(timeline);
        LedgerIntegrityResponse integrity = ledgerService.verify();
        CivicLedgerEntry latestIssueLedger = ledgerRepository
                .findTopByAggregateTypeAndAggregateIdOrderByIdDesc("ISSUE", issueId)
                .orElse(null);

        boolean resolved = issue.getStatus() == IssueStatus.RESOLVED;
        LocalDateTime reportedAt = issue.getCreatedAt();
        LocalDateTime resolvedAt = resolvedEvent == null ? null : resolvedEvent.createdAt();
        long resolutionHours = reportedAt == null || resolvedAt == null ? 0 : Math.max(0, Duration.between(reportedAt, resolvedAt).toHours());
        long targetHours = targetHours(issue.getSeverity());
        boolean onTime = resolved && resolutionHours <= targetHours;
        long verificationCount = verificationRepository.countByIssueId(issueId);

        return ResolutionCertificateResponse.builder()
                .issueId(issue.getId())
                .certificateNumber("CHAI-RC-%05d".formatted(issue.getId()))
                .certificateAvailable(resolved)
                .availabilityMessage(resolved
                        ? "Official resolution certificate generated from public workflow history."
                        : "Certificate preview only. Official certificate is available after the issue is marked RESOLVED.")
                .title(issue.getTitle())
                .description(issue.getDescription())
                .category(issue.getCategory())
                .severity(issue.getSeverity())
                .status(issue.getStatus())
                .ward(issue.getWard())
                .locality(issue.getLocality())
                .city(issue.getCity())
                .district(issue.getDistrict())
                .state(issue.getState())
                .formattedAddress(issue.getFormattedAddress())
                .latitude(issue.getLatitude())
                .longitude(issue.getLongitude())
                .reporterName(issue.getReporterName())
                .recommendedDepartment(issue.getRecommendedDepartment())
                .impactScore(issue.getImpactScore())
                .resolutionSummary(resolutionSummary(resolvedEvent, issue))
                .resolutionEvidenceUrl(resolvedEvent == null ? null : resolvedEvent.evidenceUrl())
                .resolvedBy(resolvedEvent == null ? null : resolvedEvent.actorName())
                .reportedAt(reportedAt)
                .resolvedAt(resolvedAt)
                .resolutionHours(resolutionHours)
                .resolvedOnTime(onTime)
                .slaAssessment(slaAssessment(resolved, resolutionHours, targetHours))
                .verificationCount((int) verificationCount)
                .communityVerified(verificationCount >= 3)
                .ledgerVerified(integrity.isValid())
                .ledgerMessage(integrity.getMessage())
                .auditHash(latestIssueLedger == null ? integrity.getLastHash() : latestIssueLedger.getEntryHash())
                .ledgerEntryId(latestIssueLedger == null ? null : latestIssueLedger.getId())
                .timeline(timeline)
                .build();
    }

    private IssueStatusHistoryResponse latestResolvedEvent(List<IssueStatusHistoryResponse> timeline) {
        return timeline.stream()
                .filter(item -> item.toStatus() == IssueStatus.RESOLVED)
                .max(Comparator.comparing(IssueStatusHistoryResponse::createdAt))
                .orElse(null);
    }

    private String resolutionSummary(IssueStatusHistoryResponse resolvedEvent, Issue issue) {
        if (resolvedEvent != null && resolvedEvent.note() != null && !resolvedEvent.note().isBlank()) {
            return resolvedEvent.note();
        }
        if (issue.getStatus() == IssueStatus.RESOLVED) {
            return "The authority marked this civic issue as resolved.";
        }
        return "Resolution summary will be generated after an authority marks this issue resolved.";
    }

    private long targetHours(IssueSeverity severity) {
        return switch (severity == null ? IssueSeverity.LOW : severity) {
            case CRITICAL -> 24;
            case HIGH -> 48;
            case MEDIUM -> 120;
            case LOW -> 240;
        };
    }

    private String slaAssessment(boolean resolved, long resolutionHours, long targetHours) {
        if (!resolved) return "Pending resolution. Target window: within %d hours.".formatted(targetHours);
        if (resolutionHours <= targetHours) {
            return "Resolved on time in %d hours against a %d-hour target.".formatted(resolutionHours, targetHours);
        }
        return "Resolved late in %d hours against a %d-hour target.".formatted(resolutionHours, targetHours);
    }
}
