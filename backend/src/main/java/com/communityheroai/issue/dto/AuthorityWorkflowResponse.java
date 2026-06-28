package com.communityheroai.issue.dto;

import com.communityheroai.issue.entity.IssueStatus;

import java.util.List;

public record AuthorityWorkflowResponse(
        boolean workflowEnabled,
        IssueStatus currentStatus,
        List<IssueStatus> allowedTransitions,
        List<IssueStatusHistoryResponse> history,
        String warning
) {
}
