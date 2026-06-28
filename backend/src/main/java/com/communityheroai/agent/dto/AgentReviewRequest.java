package com.communityheroai.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentReviewRequest(
        @NotBlank @Size(max = 100) String actorName,
        @Size(max = 1000) String note,
        @Size(max = 500) String evidenceUrl
) {
}
