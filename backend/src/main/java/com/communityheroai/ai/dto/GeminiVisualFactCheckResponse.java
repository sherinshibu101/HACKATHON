package com.communityheroai.ai.dto;

import com.communityheroai.issue.entity.VisualVerificationResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GeminiVisualFactCheckResponse(
        @NotNull VisualVerificationResult verificationResult,
        @Min(0) @Max(100) Integer confidenceScore,
        @NotBlank String reasoningReport,
        @NotBlank String riskFlags
) {
}
