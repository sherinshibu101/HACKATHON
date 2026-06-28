package com.communityheroai.ai.dto;

import com.communityheroai.issue.entity.IssueCategory;
import com.communityheroai.issue.entity.IssueSeverity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GeminiIssueAnalysisResponse(
        @NotNull IssueCategory category,
        @NotNull IssueSeverity severity,
        @NotBlank String recommendedDepartment,
        @NotNull @Min(0) @Max(100) Integer impactScore,
        @NotBlank String riskExplanation,
        @NotBlank String suggestedAction,
        @NotBlank String complaintDraft,
        @NotBlank String escalationMessage,
        @NotBlank String resolutionUrgency
) {
}
