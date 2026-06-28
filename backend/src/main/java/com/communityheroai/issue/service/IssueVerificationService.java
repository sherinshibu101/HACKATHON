package com.communityheroai.issue.service;

import com.communityheroai.ledger.service.CivicLedgerService;
import com.communityheroai.exception.ResourceNotFoundException;
import com.communityheroai.issue.dto.IssueVerificationRequest;
import com.communityheroai.issue.dto.IssueVerificationResponse;
import com.communityheroai.issue.entity.Issue;
import com.communityheroai.issue.entity.IssueStatus;
import com.communityheroai.issue.entity.IssueVerification;
import com.communityheroai.issue.entity.StatusActorType;
import com.communityheroai.issue.repository.IssueRepository;
import com.communityheroai.issue.repository.IssueVerificationRepository;
import com.communityheroai.agent.entity.AgentTrigger;
import com.communityheroai.agent.service.CivicCaseAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueVerificationService {
    private static final long COMMUNITY_VERIFIED_THRESHOLD = 3;
    private final IssueRepository issueRepository;
    private final IssueVerificationRepository verificationRepository;
    private final IssueStatusHistoryService statusHistoryService;
    private final CivicLedgerService ledgerService;
    private final CivicCaseAgentService civicCaseAgentService;

    @Transactional
    public IssueVerificationResponse verify(Long issueId, IssueVerificationRequest request) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));
        IssueVerification verification = verificationRepository.save(IssueVerification.builder()
                .issue(issue)
                .verifierName(request.getVerifierName())
                .verifierEmail(blankToNull(request.getVerifierEmail()))
                .comment(blankToNull(request.getComment()))
                .build());
        ledgerService.append("COMMUNITY_VERIFICATION_RECORDED", "ISSUE", issueId,
                verification.getVerifierName(), verificationPayload(verification));

        if (issue.getStatus() == IssueStatus.REPORTED
                && verificationRepository.countByIssueId(issueId) >= COMMUNITY_VERIFIED_THRESHOLD) {
            IssueStatus previousStatus = issue.getStatus();
            issue.setStatus(IssueStatus.VERIFIED);
            issueRepository.save(issue);
            statusHistoryService.recordTransition(issue, previousStatus, IssueStatus.VERIFIED,
                    "Community", StatusActorType.COMMUNITY,
                    "Community verification threshold reached with three citizen confirmations.", null);
            civicCaseAgentService.run(issueId, AgentTrigger.COMMUNITY_VERIFIED);
        }
        return toResponse(verification);
    }

    public List<IssueVerificationResponse> findByIssue(Long issueId) {
        if (!issueRepository.existsById(issueId)) {
            throw new ResourceNotFoundException("Issue not found: " + issueId);
        }
        return verificationRepository.findByIssueIdOrderByCreatedAtDesc(issueId).stream()
                .map(this::toResponse).toList();
    }

    private IssueVerificationResponse toResponse(IssueVerification verification) {
        return IssueVerificationResponse.builder()
                .id(verification.getId())
                .issueId(verification.getIssue().getId())
                .verifierName(verification.getVerifierName())
                .comment(verification.getComment())
                .createdAt(verification.getCreatedAt())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String verificationPayload(IssueVerification verification) {
        return """
                {"verificationId":%s,"verifierName":"%s","comment":"%s"}
                """.formatted(
                String.valueOf(verification.getId()),
                jsonEscape(verification.getVerifierName()),
                jsonEscape(verification.getComment()));
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
