package com.communityheroai.issue.dto;

public record IssueEmailPreviewResponse(
        boolean configured,
        String recipient,
        String subject,
        String body,
        String warning
) {
}
