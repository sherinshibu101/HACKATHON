package com.communityheroai.ai.dto;

import com.communityheroai.issue.entity.IssueCategory;

public record GeminiIssueAnalysisRequest(
        String title,
        String description,
        IssueCategory category,
        String ward,
        String locality,
        String state,
        String city,
        String formattedAddress,
        Double latitude,
        Double longitude
) {
}
