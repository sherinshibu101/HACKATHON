package com.communityheroai.issue.dto;

import com.communityheroai.issue.entity.IssueStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IssueStatusUpdateRequest(
        @NotNull IssueStatus targetStatus,
        @NotBlank @Size(max = 100) String actorName,
        @NotBlank @Size(max = 1000) String note,
        @Size(max = 500) String evidenceUrl
) {
}
