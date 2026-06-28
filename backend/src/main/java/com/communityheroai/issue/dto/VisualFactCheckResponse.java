package com.communityheroai.issue.dto;

import com.communityheroai.issue.entity.VisualFactCheckStatus;
import com.communityheroai.issue.entity.VisualVerificationResult;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class VisualFactCheckResponse {
    Long id;
    Long issueId;
    Long mediaId;
    VisualFactCheckStatus status;
    VisualVerificationResult verificationResult;
    Integer confidenceScore;
    String baselineImageUrl;
    String userImageUrl;
    String reasoningReport;
    String riskFlags;
    String message;
    LocalDateTime createdAt;
}
