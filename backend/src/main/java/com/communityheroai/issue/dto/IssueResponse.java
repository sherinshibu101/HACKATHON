package com.communityheroai.issue.dto;

import com.communityheroai.issue.entity.IssueCategory;
import com.communityheroai.issue.entity.IssueSeverity;
import com.communityheroai.issue.entity.IssueStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class IssueResponse {
    Long id;
    String title;
    String reporterName;
    String description;
    IssueCategory category;
    IssueStatus status;
    IssueSeverity severity;
    Double latitude;
    Double longitude;
    String ward;
    String locality;
    String country;
    String state;
    String district;
    String city;
    String postalCode;
    String formattedAddress;
    Double locationAccuracyMeters;
    com.communityheroai.issue.entity.LocationSource locationSource;
    String recommendedDepartment;
    Integer impactScore;
    String riskExplanation;
    String suggestedAction;
    String complaintDraft;
    String escalationMessage;
    String resolutionUrgency;
    LocalDateTime aiGeneratedAt;
    LocalDateTime authorityEmailSentAt;
    String authorityEmailRecipient;
    String aiAnalysisMessage;
    String dispatchDepartment;
    String dispatchPriority;
    String dispatchCitizenNotification;
    LocalDateTime dispatchAnalyzedAt;
    long verificationCount;
    boolean communityVerified;
    boolean duplicateWarning;
    java.util.List<PossibleDuplicateIssueResponse> possibleDuplicateIssues;
    java.util.List<IssueMediaResponse> media;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
