package com.communityheroai.ai.dto;

/** @deprecated Use {@link GeminiIssueAnalysisRequest}. */
@Deprecated
public record GeminiAnalysisRequest(Long issueId, String title, String description, String category) {
}
