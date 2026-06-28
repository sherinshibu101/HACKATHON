package com.communityheroai.issue.dto;

import com.communityheroai.issue.entity.IssueCategory;
import com.communityheroai.issue.entity.IssueStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PossibleDuplicateIssueResponse {
    Long id;
    String title;
    double distanceMeters;
    IssueCategory category;
    IssueStatus status;
}
