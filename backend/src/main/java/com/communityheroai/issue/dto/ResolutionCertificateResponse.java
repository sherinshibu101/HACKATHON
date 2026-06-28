package com.communityheroai.issue.dto;

import com.communityheroai.issue.entity.IssueCategory;
import com.communityheroai.issue.entity.IssueSeverity;
import com.communityheroai.issue.entity.IssueStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ResolutionCertificateResponse(
        Long issueId,
        String certificateNumber,
        boolean certificateAvailable,
        String availabilityMessage,
        String title,
        String description,
        IssueCategory category,
        IssueSeverity severity,
        IssueStatus status,
        String ward,
        String locality,
        String city,
        String district,
        String state,
        String formattedAddress,
        Double latitude,
        Double longitude,
        String reporterName,
        String recommendedDepartment,
        Integer impactScore,
        String resolutionSummary,
        String resolutionEvidenceUrl,
        String resolvedBy,
        LocalDateTime reportedAt,
        LocalDateTime resolvedAt,
        long resolutionHours,
        boolean resolvedOnTime,
        String slaAssessment,
        int verificationCount,
        boolean communityVerified,
        boolean ledgerVerified,
        String ledgerMessage,
        String auditHash,
        Long ledgerEntryId,
        List<IssueStatusHistoryResponse> timeline
) {
}
