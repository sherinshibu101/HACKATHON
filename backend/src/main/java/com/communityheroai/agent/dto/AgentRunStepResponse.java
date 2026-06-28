package com.communityheroai.agent.dto;

import java.time.LocalDateTime;

public record AgentRunStepResponse(
        int stepNumber,
        String toolName,
        String actionSummary,
        String observationSummary,
        LocalDateTime createdAt
) {
}
