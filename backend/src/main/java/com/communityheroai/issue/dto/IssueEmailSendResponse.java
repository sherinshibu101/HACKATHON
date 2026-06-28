package com.communityheroai.issue.dto;

import com.communityheroai.issue.entity.IssueStatus;

import java.time.LocalDateTime;

public record IssueEmailSendResponse(
        String message,
        String recipient,
        LocalDateTime sentAt,
        IssueStatus issueStatus
) {
}
