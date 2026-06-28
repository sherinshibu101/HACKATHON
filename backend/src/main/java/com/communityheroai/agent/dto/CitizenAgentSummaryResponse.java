package com.communityheroai.agent.dto;

import com.communityheroai.agent.entity.AgentRunStatus;
import com.communityheroai.issue.entity.IssueStatus;

import java.time.LocalDateTime;

public record CitizenAgentSummaryResponse(
        Long runId,
        AgentRunStatus status,
        String citizenSummary,
        String recommendedNextAction,
        IssueStatus proposedStatus,
        Integer confidence,
        LocalDateTime updatedAt
) {
}
