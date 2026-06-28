package com.communityheroai.issue.service;

import com.communityheroai.exception.WorkflowException;
import com.communityheroai.issue.dto.IssueStatusUpdateRequest;
import com.communityheroai.issue.entity.Issue;
import com.communityheroai.issue.entity.IssueStatus;
import com.communityheroai.issue.repository.IssueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthorityWorkflowServiceTest {
    @Test
    void escalatedIssueCanMoveToInProgress() {
        IssueRepository issueRepository = mock(IssueRepository.class);
        IssueStatusHistoryService historyService = mock(IssueStatusHistoryService.class);
        Issue issue = Issue.builder().id(7L).status(IssueStatus.ESCALATED).build();
        when(issueRepository.findById(7L)).thenReturn(Optional.of(issue));
        when(issueRepository.save(issue)).thenReturn(issue);
        when(historyService.responsesForIssue(7L)).thenReturn(List.of());
        AuthorityWorkflowService service = service(issueRepository, historyService, true);

        var response = service.updateStatus(7L, new IssueStatusUpdateRequest(
                IssueStatus.IN_PROGRESS, "Ward Officer", "Inspection team assigned.", null));

        assertThat(response.currentStatus()).isEqualTo(IssueStatus.IN_PROGRESS);
        verify(issueRepository).save(issue);
        verify(historyService).recordTransition(eq(issue), eq(IssueStatus.ESCALATED),
                eq(IssueStatus.IN_PROGRESS), eq("Ward Officer"), any(), anyString(), isNull());
    }

    @Test
    void reportedIssueCannotJumpToResolved() {
        IssueRepository issueRepository = mock(IssueRepository.class);
        IssueStatusHistoryService historyService = mock(IssueStatusHistoryService.class);
        Issue issue = Issue.builder().id(7L).status(IssueStatus.REPORTED).build();
        when(issueRepository.findById(7L)).thenReturn(Optional.of(issue));
        AuthorityWorkflowService service = service(issueRepository, historyService, true);

        assertThatThrownBy(() -> service.updateStatus(7L, new IssueStatusUpdateRequest(
                IssueStatus.RESOLVED, "Ward Officer", "Done.", null)))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    void disabledWorkflowRejectsStatusUpdates() {
        AuthorityWorkflowService service = service(mock(IssueRepository.class), mock(IssueStatusHistoryService.class), false);

        assertThatThrownBy(() -> service.updateStatus(7L, new IssueStatusUpdateRequest(
                IssueStatus.IN_PROGRESS, "Ward Officer", "Assigned.", null)))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("disabled");
    }

    private AuthorityWorkflowService service(IssueRepository issueRepository,
                                             IssueStatusHistoryService historyService,
                                             boolean enabled) {
        AuthorityWorkflowService service = new AuthorityWorkflowService(issueRepository, historyService);
        ReflectionTestUtils.setField(service, "workflowEnabled", enabled);
        return service;
    }
}
