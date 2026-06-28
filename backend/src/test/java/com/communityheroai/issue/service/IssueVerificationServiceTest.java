package com.communityheroai.issue.service;

import com.communityheroai.agent.entity.AgentTrigger;
import com.communityheroai.agent.service.CivicCaseAgentService;
import com.communityheroai.ledger.service.CivicLedgerService;
import com.communityheroai.issue.dto.IssueVerificationRequest;
import com.communityheroai.issue.entity.Issue;
import com.communityheroai.issue.entity.IssueStatus;
import com.communityheroai.issue.repository.IssueRepository;
import com.communityheroai.issue.repository.IssueVerificationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IssueVerificationServiceTest {
    @Test
    void thirdVerificationPromotesReportedIssue() {
        IssueRepository issueRepository = mock(IssueRepository.class);
        IssueVerificationRepository verificationRepository = mock(IssueVerificationRepository.class);
        Issue issue = Issue.builder().id(12L).status(IssueStatus.REPORTED).build();
        when(issueRepository.findById(12L)).thenReturn(Optional.of(issue));
        when(verificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationRepository.countByIssueId(12L)).thenReturn(3L);
        IssueVerificationRequest request = new IssueVerificationRequest();
        request.setVerifierName("Asha");
        IssueStatusHistoryService historyService = mock(IssueStatusHistoryService.class);
        CivicLedgerService ledgerService = mock(CivicLedgerService.class);
        CivicCaseAgentService agentService = mock(CivicCaseAgentService.class);

        new IssueVerificationService(issueRepository, verificationRepository, historyService, ledgerService, agentService)
                .verify(12L, request);

        assertThat(issue.getStatus()).isEqualTo(IssueStatus.VERIFIED);
        verify(ledgerService).append(eq("COMMUNITY_VERIFICATION_RECORDED"), eq("ISSUE"), eq(12L),
                eq("Asha"), contains("verificationId"));
        verify(issueRepository).save(issue);
        verify(historyService).recordTransition(eq(issue), eq(IssueStatus.REPORTED), eq(IssueStatus.VERIFIED),
                eq("Community"), any(), anyString(), isNull());
        verify(agentService).run(12L, AgentTrigger.COMMUNITY_VERIFIED);
    }
}
