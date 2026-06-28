package com.communityheroai.issue.service;

import com.communityheroai.ledger.service.CivicLedgerService;
import com.communityheroai.issue.dto.IssueStatusHistoryResponse;
import com.communityheroai.issue.entity.Issue;
import com.communityheroai.issue.entity.IssueStatus;
import com.communityheroai.issue.entity.IssueStatusHistory;
import com.communityheroai.issue.entity.StatusActorType;
import com.communityheroai.issue.repository.IssueStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueStatusHistoryService {
    private final IssueStatusHistoryRepository historyRepository;
    private final CivicLedgerService ledgerService;

    @Transactional
    public void recordInitial(Issue issue) {
        if (issue == null || issue.getId() == null) return;
        if (!historyRepository.findByIssueIdOrderByCreatedAtAsc(issue.getId()).isEmpty()) return;
        record(issue, null, issue.getStatus(), "Community Hero AI", StatusActorType.SYSTEM,
                "Issue reported by a citizen.", null);
    }

    @Transactional
    public void recordTransition(Issue issue, IssueStatus fromStatus, IssueStatus toStatus,
                                 String actorName, StatusActorType actorType, String note,
                                 String evidenceUrl) {
        if (fromStatus == toStatus) return;
        record(issue, fromStatus, toStatus, actorName, actorType, note, evidenceUrl);
    }

    @Transactional
    public void recordNote(Issue issue, String actorName, StatusActorType actorType, String note,
                           String evidenceUrl) {
        if (issue == null || issue.getId() == null) return;
        record(issue, issue.getStatus(), issue.getStatus(), actorName, actorType, note, evidenceUrl);
    }

    @Transactional(readOnly = true)
    public List<IssueStatusHistoryResponse> responsesForIssue(Long issueId) {
        return historyRepository.findByIssueIdOrderByCreatedAtAsc(issueId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteByIssueId(Long issueId) {
        historyRepository.deleteByIssueId(issueId);
    }

    private void record(Issue issue, IssueStatus fromStatus, IssueStatus toStatus,
                        String actorName, StatusActorType actorType, String note,
                        String evidenceUrl) {
        IssueStatusHistory saved = historyRepository.save(IssueStatusHistory.builder()
                .issue(issue)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .actorName(blankToDefault(actorName, "Community Hero AI"))
                .actorType(actorType == null ? StatusActorType.SYSTEM : actorType)
                .note(blankToDefault(note, "Status updated."))
                .evidenceUrl(blankToNull(evidenceUrl))
                .build());
        ledgerService.append("STATUS_HISTORY_RECORDED", "ISSUE", issue.getId(),
                saved.getActorName(), statusPayload(saved));
    }

    private String statusPayload(IssueStatusHistory history) {
        return """
                {"historyId":%s,"fromStatus":"%s","toStatus":"%s","actorType":"%s","note":"%s","evidenceUrl":"%s"}
                """.formatted(
                String.valueOf(history.getId()),
                history.getFromStatus(),
                history.getToStatus(),
                history.getActorType(),
                jsonEscape(history.getNote()),
                jsonEscape(history.getEvidenceUrl()));
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private IssueStatusHistoryResponse toResponse(IssueStatusHistory history) {
        return new IssueStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getActorName(),
                history.getActorType(),
                history.getNote(),
                history.getEvidenceUrl(),
                history.getCreatedAt()
        );
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
