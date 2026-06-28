package com.communityheroai.issue.dto;

import com.communityheroai.issue.entity.IssueStatus;
import com.communityheroai.issue.entity.StatusActorType;

import java.time.LocalDateTime;

public record IssueStatusHistoryResponse(
        Long id,
        IssueStatus fromStatus,
        IssueStatus toStatus,
        String actorName,
        StatusActorType actorType,
        String note,
        String evidenceUrl,
        LocalDateTime createdAt
) {
}
