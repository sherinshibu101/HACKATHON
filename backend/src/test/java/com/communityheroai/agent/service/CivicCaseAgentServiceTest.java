package com.communityheroai.agent.service;

import com.communityheroai.agent.dto.AdminAgentRunResponse;
import com.communityheroai.agent.entity.AgentRun;
import com.communityheroai.agent.entity.AgentRunStatus;
import com.communityheroai.agent.entity.AgentTrigger;
import com.communityheroai.agent.repository.AgentRunRepository;
import com.communityheroai.agent.repository.AgentRunStepRepository;
import com.communityheroai.issue.entity.*;
import com.communityheroai.issue.repository.IssueMediaRepository;
import com.communityheroai.issue.repository.IssueRepository;
import com.communityheroai.issue.repository.IssueVerificationRepository;
import com.communityheroai.issue.service.AuthorityWorkflowService;
import com.communityheroai.issue.service.DuplicateDetectionService;
import com.communityheroai.issue.service.IssueStatusHistoryService;
import com.communityheroai.ledger.service.CivicLedgerService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CivicCaseAgentServiceTest {
    @Test
    void highImpactReportProducesHumanGatedEscalationProposal() {
        AgentRunRepository runRepository = mock(AgentRunRepository.class);
        AgentRunStepRepository stepRepository = mock(AgentRunStepRepository.class);
        IssueRepository issueRepository = mock(IssueRepository.class);
        IssueVerificationRepository verificationRepository = mock(IssueVerificationRepository.class);
        IssueMediaRepository mediaRepository = mock(IssueMediaRepository.class);
        DuplicateDetectionService duplicateService = mock(DuplicateDetectionService.class);
        IssueStatusHistoryService historyService = mock(IssueStatusHistoryService.class);
        AuthorityWorkflowService workflowService = mock(AuthorityWorkflowService.class);
        CivicLedgerService ledgerService = mock(CivicLedgerService.class);
        CivicCaseAgentService service = new CivicCaseAgentService(runRepository, stepRepository,
                issueRepository, verificationRepository, mediaRepository, duplicateService,
                historyService, workflowService, ledgerService);

        Issue issue = Issue.builder().id(7L).title("Burst pipe near school")
                .description("Water is flooding the school road.")
                .category(IssueCategory.WATER_LEAKAGE).status(IssueStatus.REPORTED)
                .severity(IssueSeverity.CRITICAL).impactScore(93).ward("Ward 7")
                .latitude(9.59).longitude(76.52).recommendedDepartment("Water Authority")
                .suggestedAction("Barricade and repair the pipe.")
                .aiGeneratedAt(LocalDateTime.now()).createdAt(LocalDateTime.now()).build();
        when(issueRepository.findById(7L)).thenReturn(Optional.of(issue));
        when(issueRepository.findByWard("Ward 7")).thenReturn(List.of(issue));
        when(mediaRepository.findByIssueIdOrderByCreatedAtAsc(7L)).thenReturn(List.of());
        when(verificationRepository.countByIssueId(7L)).thenReturn(0L);
        when(duplicateService.findDuplicates(issue)).thenReturn(List.of());
        when(historyService.responsesForIssue(7L)).thenReturn(List.of());
        when(runRepository.save(any(AgentRun.class))).thenAnswer(invocation -> {
            AgentRun run = invocation.getArgument(0);
            if (run.getId() == null) run.setId(41L);
            return run;
        });
        when(stepRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminAgentRunResponse response = service.run(7L, AgentTrigger.ISSUE_CREATED);

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.proposedStatus()).isEqualTo(IssueStatus.ESCALATED);
        assertThat(response.requiresHumanApproval()).isTrue();
        assertThat(response.steps()).hasSize(8);
        assertThat(response.targetResolutionHours()).isEqualTo(12);
        verifyNoInteractions(workflowService);
        verify(ledgerService).append(eq("CIVIC_AGENT_RUN_COMPLETED"), eq("ISSUE"), eq(7L),
                eq("Civic Case Manager"), contains("ESCALATED"));
    }
}
