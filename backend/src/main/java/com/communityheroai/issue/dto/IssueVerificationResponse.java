package com.communityheroai.issue.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class IssueVerificationResponse {
    Long id;
    Long issueId;
    String verifierName;
    String comment;
    LocalDateTime createdAt;
}
