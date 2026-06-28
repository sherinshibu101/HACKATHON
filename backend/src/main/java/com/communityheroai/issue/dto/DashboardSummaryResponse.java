package com.communityheroai.issue.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DashboardSummaryResponse {
    long totalIssues;
    long reportedIssues;
    long inProgressIssues;
    long resolvedIssues;
    long communityVerifications;
}
