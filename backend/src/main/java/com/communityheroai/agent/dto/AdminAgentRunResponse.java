package com.communityheroai.agent.dto;

import com.communityheroai.agent.entity.AgentRunStatus;
import com.communityheroai.agent.entity.AgentTrigger;
import com.communityheroai.issue.entity.IssueStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminAgentRunResponse(
        Long id,
        Long issueId,
        AgentRunStatus status,
        AgentTrigger triggerType,
        String model,
        String citizenSummary,
        String adminRecommendation,
        String recommendedNextAction,
        String proposedDepartment,
        String proposedPriority,
        IssueStatus proposedStatus,
        Integer targetResolutionHours,
        Integer confidence,
        boolean requiresHumanApproval,
        String failureMessage,
        String reviewedBy,
        String reviewNote,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime reviewedAt,
        List<AgentRunStepResponse> steps
) {
}
