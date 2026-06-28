package com.communityheroai.issue.dto;

import com.communityheroai.issue.entity.ImageValidationStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ImageValidationResponse {
    ImageValidationStatus validationStatus;
    Integer validationConfidence;
    String validationSummary;
    String validationLabels;
    LocalDateTime validatedAt;
}
