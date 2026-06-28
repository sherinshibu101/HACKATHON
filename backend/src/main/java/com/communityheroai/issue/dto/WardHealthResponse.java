package com.communityheroai.issue.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WardHealthResponse {
    String ward;
    long totalIssues;
    long unresolvedIssues;
    long criticalIssues;
    long resolvedIssues;
    int healthScore;
    WardHealthStatus status;

    public enum WardHealthStatus { HEALTHY, MODERATE, NEEDS_ATTENTION, CRITICAL }
}
