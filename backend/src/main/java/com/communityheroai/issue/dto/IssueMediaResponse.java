package com.communityheroai.issue.dto;

import com.communityheroai.issue.entity.IssueMediaType;
import com.communityheroai.issue.entity.ImageValidationStatus;
import com.communityheroai.issue.entity.MediaProcessingStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class IssueMediaResponse {
    Long id;
    Long issueId;
    IssueMediaType mediaType;
    String mediaUrl;
    String thumbnailUrl;
    String originalFilename;
    String contentType;
    Long fileSize;
    MediaProcessingStatus processingStatus;
    ImageValidationStatus validationStatus;
    Integer validationConfidence;
    String validationSummary;
    String validationLabels;
    LocalDateTime validatedAt;
    LocalDateTime createdAt;
}
