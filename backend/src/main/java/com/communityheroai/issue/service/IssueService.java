package com.communityheroai.issue.service;

import com.communityheroai.agent.dto.DispatchAnalysis;
import com.communityheroai.agent.entity.AgentTrigger;
import com.communityheroai.agent.service.CivicCaseAgentService;
import com.communityheroai.agent.service.DispatchAgent;
import com.communityheroai.ai.GeminiAnalysisService;
import com.communityheroai.ai.dto.GeminiIssueAnalysisRequest;
import com.communityheroai.ai.dto.GeminiIssueAnalysisResponse;
import com.communityheroai.exception.ResourceNotFoundException;
import com.communityheroai.issue.dto.*;
import com.communityheroai.issue.entity.*;
import com.communityheroai.issue.repository.IssueRepository;
import com.communityheroai.issue.repository.IssueVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class IssueService {
    private static final Logger log = LoggerFactory.getLogger(IssueService.class);
    private final IssueRepository issueRepository;
    private final GeminiAnalysisService geminiAnalysisService;
    private final DispatchAgent dispatchAgent;
    private final DuplicateDetectionService duplicateDetectionService;
    private final IssueVerificationRepository verificationRepository;
    private final IssueMediaService mediaService;
    private final com.communityheroai.issue.repository.IssueEmailLogRepository emailLogRepository;
    private final IssueStatusHistoryService statusHistoryService;
    private final VisualFactCheckService visualFactCheckService;
    private final CivicCaseAgentService civicCaseAgentService;

    public IssueResponse create(IssueRequest request) {
        List<PossibleDuplicateIssueResponse> duplicates = duplicateDetectionService.findDuplicates(request);
        IssueStatus initialStatus = duplicates.stream()
                .filter(duplicate -> duplicate.getStatus() != IssueStatus.RESOLVED)
                .findFirst()
                .map(PossibleDuplicateIssueResponse::getStatus)
                .orElse(IssueStatus.REPORTED);
        Issue issue = Issue.builder()
                .title(request.getTitle())
                .reporterName(blankToDefault(request.getReporterName(), "Community Member"))
                .reporterEmail(blankToNull(request.getReporterEmail()))
                .description(request.getDescription())
                .category(request.getCategory())
                .status(initialStatus)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .ward(normalizeWard(request.getWard()))
                .locality(request.getLocality())
                .country(request.getCountry())
                .state(request.getState())
                .district(request.getDistrict())
                .city(request.getCity())
                .postalCode(request.getPostalCode())
                .formattedAddress(request.getFormattedAddress())
                .locationAccuracyMeters(request.getLocationAccuracyMeters())
                .locationSource(request.getLocationSource() == null ? LocationSource.MANUAL : request.getLocationSource())
                .build();
        Issue savedIssue = issueRepository.save(issue);
        statusHistoryService.recordInitial(savedIssue);
        IssueResponse analyzed = analyzeAndSave(savedIssue,
                "Issue reported successfully, but AI analysis is currently unavailable.");
        civicCaseAgentService.run(savedIssue.getId(), AgentTrigger.ISSUE_CREATED);
        return analyzed
                .toBuilder()
                .duplicateWarning(!duplicates.isEmpty())
                .possibleDuplicateIssues(duplicates)
                .build();
    }

    public DuplicateCheckResponse checkDuplicates(IssueRequest request) {
        List<PossibleDuplicateIssueResponse> duplicates = duplicateDetectionService.findDuplicates(request);
        return DuplicateCheckResponse.builder()
                .duplicateWarning(!duplicates.isEmpty())
                .possibleDuplicateIssues(duplicates)
                .build();
    }

    public List<PossibleDuplicateIssueResponse> duplicates(Long id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + id));
        return duplicateDetectionService.findDuplicates(issue);
    }

    public List<IssueResponse> findAll() {
        return issueRepository.findAll().stream().map(this::toResponse).toList();
    }

    public IssueResponse findById(Long id) {
        return toResponse(issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + id)));
    }

    public IssueResponse update(Long id, IssueRequest request) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + id));
        issue.setTitle(request.getTitle());
        issue.setReporterName(blankToDefault(request.getReporterName(), issue.getReporterName()));
        issue.setReporterEmail(blankToNull(request.getReporterEmail()));
        issue.setDescription(request.getDescription());
        issue.setCategory(request.getCategory());
        issue.setLatitude(request.getLatitude());
        issue.setLongitude(request.getLongitude());
        issue.setWard(normalizeWard(request.getWard()));
        issue.setLocality(request.getLocality());
        issue.setCountry(request.getCountry());
        issue.setState(request.getState());
        issue.setDistrict(request.getDistrict());
        issue.setCity(request.getCity());
        issue.setPostalCode(request.getPostalCode());
        issue.setFormattedAddress(request.getFormattedAddress());
        issue.setLocationAccuracyMeters(request.getLocationAccuracyMeters());
        issue.setLocationSource(request.getLocationSource() == null ? LocationSource.MANUAL : request.getLocationSource());
        return toResponse(issueRepository.save(issue));
    }

    public IssueResponse analyze(Long id) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + id));
        IssueResponse analyzed = analyzeAndSave(issue,
                "AI re-analysis failed; the issue and any previous analysis were preserved.");
        civicCaseAgentService.run(id, AgentTrigger.MANUAL_ADMIN);
        return analyzed;
    }

    @Transactional
    public void delete(Long id) {
        if (!issueRepository.existsById(id)) throw new ResourceNotFoundException("Issue not found: " + id);
        verificationRepository.deleteByIssueId(id);
        visualFactCheckService.deleteByIssueId(id);
        mediaService.deleteAllForIssue(id);
        emailLogRepository.deleteByIssueId(id);
        civicCaseAgentService.deleteByIssueId(id);
        statusHistoryService.deleteByIssueId(id);
        issueRepository.deleteById(id);
    }

    public DashboardSummaryResponse summary() {
        return DashboardSummaryResponse.builder()
                .totalIssues(issueRepository.count())
                .reportedIssues(issueRepository.countByStatus(IssueStatus.REPORTED))
                .inProgressIssues(issueRepository.countByStatus(IssueStatus.IN_PROGRESS))
                .resolvedIssues(issueRepository.countByStatus(IssueStatus.RESOLVED))
                .communityVerifications(verificationRepository.count())
                .build();
    }

    public List<CategoryStatResponse> categoryStats() {
        return issueRepository.countByCategory().stream()
                .map(row -> CategoryStatResponse.builder()
                        .category((IssueCategory) row[0])
                        .count((Long) row[1])
                        .build())
                .toList();
    }

    public List<WardStatResponse> wardStats() {
        return issueRepository.countByWard().stream()
                .map(row -> WardStatResponse.builder()
                        .ward((String) row[0])
                        .count((Long) row[1])
                        .build())
                .toList();
    }

    public List<WardHealthResponse> wardHealth() {
        Map<String, List<Issue>> byWard = issueRepository.findAll().stream()
                .collect(Collectors.groupingBy(Issue::getWard));
        return byWard.entrySet().stream()
                .map(entry -> calculateWardHealth(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(WardHealthResponse::getHealthScore))
                .toList();
    }

    public List<IssueResponse> highImpactIssues() {
        return issueRepository.findByStatusNotOrderByImpactScoreDesc(IssueStatus.RESOLVED).stream()
                .filter(issue -> issue.getImpactScore() != null)
                .limit(5)
                .map(this::toResponse)
                .toList();
    }

    private WardHealthResponse calculateWardHealth(String ward, List<Issue> issues) {
        List<Issue> unresolved = issues.stream()
                .filter(issue -> issue.getStatus() != IssueStatus.RESOLVED).toList();
        int penalty = unresolved.stream().mapToInt(this::healthPenalty).sum();
        int score = Math.max(0, Math.min(100, 100 - penalty));
        long critical = unresolved.stream().filter(issue -> issue.getSeverity() == IssueSeverity.CRITICAL).count();
        return WardHealthResponse.builder()
                .ward(ward)
                .totalIssues(issues.size())
                .unresolvedIssues(unresolved.size())
                .criticalIssues(critical)
                .resolvedIssues(issues.size() - unresolved.size())
                .healthScore(score)
                .status(wardHealthStatus(score))
                .build();
    }

    private int healthPenalty(Issue issue) {
        int severityPenalty = switch (issue.getSeverity() == null ? IssueSeverity.LOW : issue.getSeverity()) {
            case CRITICAL -> 10;
            case HIGH -> 6;
            case MEDIUM -> 3;
            case LOW -> 1;
        };
        boolean olderThanSevenDays = issue.getCreatedAt() != null
                && ChronoUnit.DAYS.between(issue.getCreatedAt(), LocalDateTime.now()) > 7;
        return severityPenalty + (olderThanSevenDays ? 2 : 0);
    }

    private WardHealthResponse.WardHealthStatus wardHealthStatus(int score) {
        if (score >= 80) return WardHealthResponse.WardHealthStatus.HEALTHY;
        if (score >= 60) return WardHealthResponse.WardHealthStatus.MODERATE;
        if (score >= 35) return WardHealthResponse.WardHealthStatus.NEEDS_ATTENTION;
        return WardHealthResponse.WardHealthStatus.CRITICAL;
    }

    private IssueResponse analyzeAndSave(Issue issue, String failureMessage) {
        try {
            GeminiIssueAnalysisResponse analysis = geminiAnalysisService.analyze(toAnalysisRequest(issue));
            issue.setCategory(analysis.category());
            issue.setSeverity(analysis.severity());
            issue.setRecommendedDepartment(analysis.recommendedDepartment());
            issue.setImpactScore(analysis.impactScore());
            issue.setRiskExplanation(analysis.riskExplanation());
            issue.setSuggestedAction(analysis.suggestedAction());
            issue.setComplaintDraft(analysis.complaintDraft());
            issue.setEscalationMessage(analysis.escalationMessage());
            issue.setResolutionUrgency(analysis.resolutionUrgency());
            issue.setAiGeneratedAt(LocalDateTime.now());

            String visionLabels = mediaService.responsesForIssue(issue.getId()).stream()
                .filter(m -> m.getValidationLabels() != null)
                .map(IssueMediaResponse::getValidationLabels)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

            try {
                DispatchAnalysis dispatch = dispatchAgent.analyzeTicket(
                    issue.getDescription(),
                    visionLabels,
                    issue.getCategory().name(),
                    issue.getWard()
                );
                issue.setDispatchDepartment(dispatch.proposedDepartment());
                issue.setDispatchPriority(dispatch.proposedPriority());
                issue.setDispatchCitizenNotification(dispatch.draftedCitizenNotification());
                issue.setDispatchAnalyzedAt(LocalDateTime.now());
                log.info("Dispatch analysis for issue {}: department={}, priority={}", 
                    issue.getId(), dispatch.proposedDepartment(), dispatch.proposedPriority());
            } catch (Exception ex) {
                log.warn("Dispatch analysis failed for issue {}: {}", issue.getId(), ex.getMessage());
                issue.setDispatchDepartment("General Administration");
                issue.setDispatchPriority("MEDIUM");
                issue.setDispatchCitizenNotification("Thank you for reporting this issue. Our team is reviewing it and will get back to you soon.");
                issue.setDispatchAnalyzedAt(LocalDateTime.now());
            }

            return toResponse(issueRepository.save(issue), "AI analysis completed successfully.");
        } catch (RuntimeException ex) {
            log.warn("AI analysis failed for issue {}: {}", issue.getId(), ex.getMessage());
            return toResponse(issue, failureMessage);
        }
    }

    private GeminiIssueAnalysisRequest toAnalysisRequest(Issue issue) {
        return new GeminiIssueAnalysisRequest(
                issue.getTitle(), issue.getDescription(), issue.getCategory(), issue.getWard(),
                issue.getLocality(), issue.getState(), issue.getCity(), issue.getFormattedAddress(),
                issue.getLatitude(), issue.getLongitude());
    }

    private IssueResponse toResponse(Issue issue) {
        String message = issue.getAiGeneratedAt() == null
                ? "AI analysis is not available for this issue."
                : "AI analysis completed successfully.";
        return toResponse(issue, message);
    }

    private IssueResponse toResponse(Issue issue, String aiAnalysisMessage) {
        long verificationCount = verificationRepository.countByIssueId(issue.getId());
        return IssueResponse.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .reporterName(issue.getReporterName())
                .description(issue.getDescription())
                .category(issue.getCategory())
                .status(issue.getStatus())
                .severity(issue.getSeverity())
                .latitude(issue.getLatitude())
                .longitude(issue.getLongitude())
                .ward(issue.getWard())
                .locality(issue.getLocality())
                .country(issue.getCountry())
                .state(issue.getState())
                .district(issue.getDistrict())
                .city(issue.getCity())
                .postalCode(issue.getPostalCode())
                .formattedAddress(issue.getFormattedAddress())
                .locationAccuracyMeters(issue.getLocationAccuracyMeters())
                .locationSource(issue.getLocationSource())
                .recommendedDepartment(issue.getRecommendedDepartment())
                .impactScore(issue.getImpactScore())
                .riskExplanation(issue.getRiskExplanation())
                .suggestedAction(issue.getSuggestedAction())
                .complaintDraft(issue.getComplaintDraft())
                .escalationMessage(issue.getEscalationMessage())
                .resolutionUrgency(issue.getResolutionUrgency())
                .aiGeneratedAt(issue.getAiGeneratedAt())
                .authorityEmailSentAt(issue.getAuthorityEmailSentAt())
                .authorityEmailRecipient(issue.getAuthorityEmailRecipient())
                .aiAnalysisMessage(aiAnalysisMessage)
                .dispatchDepartment(issue.getDispatchDepartment())
                .dispatchPriority(issue.getDispatchPriority())
                .dispatchCitizenNotification(issue.getDispatchCitizenNotification())
                .dispatchAnalyzedAt(issue.getDispatchAnalyzedAt())
                .verificationCount(verificationCount)
                .communityVerified(verificationCount >= 3)
                .duplicateWarning(false)
                .possibleDuplicateIssues(List.of())
                .media(mediaService.responsesForIssue(issue.getId()))
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }

    private String normalizeWard(String ward) {
        return ward == null || ward.isBlank() ? "UNASSIGNED" : ward.trim();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
