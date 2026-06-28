package com.communityheroai.issue.service;

import com.communityheroai.exception.ResourceNotFoundException;
import com.communityheroai.exception.WorkflowException;
import com.communityheroai.issue.dto.AuthorityWorkflowResponse;
import com.communityheroai.issue.dto.IssueStatusUpdateRequest;
import com.communityheroai.issue.entity.Issue;
import com.communityheroai.issue.entity.IssueStatus;
import com.communityheroai.issue.entity.StatusActorType;
import com.communityheroai.issue.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthorityWorkflowService {
    private static final Map<IssueStatus, List<IssueStatus>> TRANSITIONS = Map.of(
            IssueStatus.REPORTED, List.of(IssueStatus.ESCALATED),
            IssueStatus.VERIFIED, List.of(IssueStatus.ESCALATED),
            IssueStatus.ESCALATED, List.of(IssueStatus.IN_PROGRESS),
            IssueStatus.IN_PROGRESS, List.of(IssueStatus.RESOLVED),
            IssueStatus.RESOLVED, List.of()
    );

    private final IssueRepository issueRepository;
    private final IssueStatusHistoryService historyService;

    @Value("${app.workflow.authority-enabled:true}")
    private boolean workflowEnabled;

    @Transactional(readOnly = true)
    public AuthorityWorkflowResponse workflow(Long issueId) {
        Issue issue = findIssue(issueId);
        return response(issue);
    }

    @Transactional
    public AuthorityWorkflowResponse updateStatus(Long issueId, IssueStatusUpdateRequest request) {
        if (!workflowEnabled) {
            throw new WorkflowException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Authority workflow is disabled by configuration.");
        }
        Issue issue = findIssue(issueId);
        IssueStatus fromStatus = issue.getStatus();
        IssueStatus targetStatus = request.targetStatus();
        if (!allowedTransitions(fromStatus).contains(targetStatus)) {
            throw new WorkflowException(HttpStatus.BAD_REQUEST,
                    "Invalid status transition from %s to %s.".formatted(fromStatus, targetStatus));
        }

        issue.setStatus(targetStatus);
        Issue saved = issueRepository.save(issue);
        historyService.recordTransition(saved, fromStatus, targetStatus,
                request.actorName(), StatusActorType.AUTHORITY, request.note(), request.evidenceUrl());
        return response(saved);
    }

    private AuthorityWorkflowResponse response(Issue issue) {
        return new AuthorityWorkflowResponse(
                workflowEnabled,
                issue.getStatus(),
                allowedTransitions(issue.getStatus()),
                historyService.responsesForIssue(issue.getId()),
                workflowEnabled ? null : "Authority workflow is disabled by configuration."
        );
    }

    private List<IssueStatus> allowedTransitions(IssueStatus status) {
        return TRANSITIONS.getOrDefault(status, List.of());
    }

    private Issue findIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));
    }
}
