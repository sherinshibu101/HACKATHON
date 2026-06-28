package com.communityheroai.agent.service;

import com.communityheroai.agent.dto.*;
import com.communityheroai.agent.entity.*;
import com.communityheroai.agent.repository.AgentRunRepository;
import com.communityheroai.agent.repository.AgentRunStepRepository;
import com.communityheroai.exception.ResourceNotFoundException;
import com.communityheroai.exception.WorkflowException;
import com.communityheroai.issue.dto.IssueStatusHistoryResponse;
import com.communityheroai.issue.dto.IssueStatusUpdateRequest;
import com.communityheroai.issue.dto.PossibleDuplicateIssueResponse;
import com.communityheroai.issue.entity.*;
import com.communityheroai.issue.repository.IssueMediaRepository;
import com.communityheroai.issue.repository.IssueRepository;
import com.communityheroai.issue.repository.IssueVerificationRepository;
import com.communityheroai.issue.service.AuthorityWorkflowService;
import com.communityheroai.issue.service.DuplicateDetectionService;
import com.communityheroai.issue.service.IssueStatusHistoryService;
import com.communityheroai.ledger.service.CivicLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CivicCaseAgentService {
    private static final String MODEL = "gemini-assisted-policy-agent-v1";

    private final AgentRunRepository runRepository;
    private final AgentRunStepRepository stepRepository;
    private final IssueRepository issueRepository;
    private final IssueVerificationRepository verificationRepository;
    private final IssueMediaRepository mediaRepository;
    private final DuplicateDetectionService duplicateDetectionService;
    private final IssueStatusHistoryService historyService;
    private final AuthorityWorkflowService workflowService;
    private final CivicLedgerService ledgerService;

    @Transactional
    public AdminAgentRunResponse run(Long issueId, AgentTrigger trigger) {
        Issue issue = findIssue(issueId);
        AgentRun run = runRepository.save(AgentRun.builder()
                .issue(issue)
                .status(AgentRunStatus.RUNNING)
                .triggerType(trigger == null ? AgentTrigger.MANUAL_ADMIN : trigger)
                .model(MODEL)
                .requiresHumanApproval(true)
                .build());
        try {
            int step = 1;
            addStep(run, step++, "CASE_PLANNER", "Plan a bounded civic case investigation.",
                    "Selected issue context, evidence, duplicate, ward health, community signal, and workflow tools.");

            addStep(run, step++, "ISSUE_CONTEXT", "Inspect the saved report and Gemini analysis.",
                    contextObservation(issue));

            List<IssueMedia> media = mediaRepository.findByIssueIdOrderByCreatedAtAsc(issueId);
            long validMedia = media.stream().filter(item -> item.getValidationStatus() == ImageValidationStatus.VALID).count();
            long suspectMedia = media.stream().filter(item -> item.getValidationStatus() == ImageValidationStatus.SUSPECT).count();
            addStep(run, step++, "EVIDENCE_INSPECTION", "Review persistent evidence validation signals.",
                    "%d attachment(s): %d Vision-valid and %d suspect.".formatted(media.size(), validMedia, suspectMedia));

            List<PossibleDuplicateIssueResponse> duplicates = duplicateDetectionService.findDuplicates(issue);
            addStep(run, step++, "DUPLICATE_SEARCH", "Search for nearby reports with matching category and text.",
                    duplicates.isEmpty()
                            ? "No similar report was found within the duplicate-detection radius."
                            : "%d related report(s) found; nearest is issue #%d at %.0f metres."
                            .formatted(duplicates.size(), duplicates.getFirst().getId(), duplicates.getFirst().getDistanceMeters()));

            WardSignal ward = wardSignal(issue.getWard());
            addStep(run, step++, "WARD_HEALTH", "Measure unresolved civic pressure in the reported ward.",
                    "Ward %s has health score %d/100 with %d unresolved issue(s)."
                            .formatted(ward.name(), ward.score(), ward.unresolved()));

            long verifications = verificationRepository.countByIssueId(issueId);
            addStep(run, step++, "COMMUNITY_SIGNAL", "Count independent citizen confirmations.",
                    "%d community verification(s) recorded; threshold is 3.".formatted(verifications));

            List<IssueStatusHistoryResponse> history = historyService.responsesForIssue(issueId);
            addStep(run, step++, "WORKFLOW_HISTORY", "Inspect the public accountability timeline.",
                    "%d status event(s) recorded; current official status is %s."
                            .formatted(history.size(), issue.getStatus()));

            IssueStatus proposedStatus = safeProposedStatus(issue, verifications);
            int confidence = confidence(issue, validMedia, suspectMedia, verifications);
            int targetHours = targetHours(issue.getSeverity());
            String nextAction = nextAction(issue, proposedStatus, targetHours);

            run.setCitizenSummary(citizenSummary(issue, media.size(), verifications));
            run.setAdminRecommendation(adminRecommendation(issue, duplicates.size(), ward, verifications, suspectMedia));
            run.setRecommendedNextAction(nextAction);
            run.setProposedDepartment(defaultText(issue.getRecommendedDepartment(),
                    defaultText(issue.getDispatchDepartment(), "General Civic Administration")));
            run.setProposedPriority(issue.getSeverity() == null ? "MEDIUM" : issue.getSeverity().name());
            run.setProposedStatus(proposedStatus);
            run.setTargetResolutionHours(targetHours);
            run.setConfidence(confidence);
            run.setStatus(AgentRunStatus.COMPLETED);
            run.setCompletedAt(LocalDateTime.now());
            addStep(run, step, "RECOMMENDATION_SYNTHESIS",
                    "Combine Gemini analysis with verified tool observations under workflow policy.",
                    proposedStatus == null
                            ? "Prepared a monitoring recommendation with no automatic status change."
                            : "Proposed %s for explicit authority approval; no action was executed."
                            .formatted(proposedStatus));
            AgentRun saved = runRepository.save(run);
            ledgerService.append("CIVIC_AGENT_RUN_COMPLETED", "ISSUE", issueId, "Civic Case Manager",
                    "{\"agentRunId\":%d,\"trigger\":\"%s\",\"proposedStatus\":\"%s\",\"confidence\":%d}"
                            .formatted(saved.getId(), saved.getTriggerType(), saved.getProposedStatus(), saved.getConfidence()));
            return toAdmin(saved);
        } catch (RuntimeException ex) {
            run.setStatus(AgentRunStatus.FAILED);
            run.setFailureMessage(safeFailure(ex));
            run.setCompletedAt(LocalDateTime.now());
            return toAdmin(runRepository.save(run));
        }
    }

    @Transactional(readOnly = true)
    public Optional<CitizenAgentSummaryResponse> latestPublic(Long issueId) {
        findIssue(issueId);
        return runRepository.findFirstByIssueIdOrderByStartedAtDesc(issueId).map(this::toCitizen);
    }

    @Transactional(readOnly = true)
    public Optional<AdminAgentRunResponse> latestAdmin(Long issueId) {
        findIssue(issueId);
        return runRepository.findFirstByIssueIdOrderByStartedAtDesc(issueId).map(this::toAdmin);
    }

    @Transactional(readOnly = true)
    public List<AdminAgentRunResponse> runs(Long issueId) {
        findIssue(issueId);
        return runRepository.findByIssueIdOrderByStartedAtDesc(issueId).stream().map(this::toAdmin).toList();
    }

    @Transactional
    public AdminAgentRunResponse approve(Long issueId, Long runId, AgentReviewRequest request) {
        AgentRun run = reviewableRun(issueId, runId);
        String note = defaultText(request.note(), "Authority approved Civic Case Manager recommendation #%d."
                .formatted(run.getId()));
        if (run.getProposedStatus() != null) {
            workflowService.updateStatus(issueId, new IssueStatusUpdateRequest(
                    run.getProposedStatus(), request.actorName(), note, request.evidenceUrl()));
        } else {
            historyService.recordNote(run.getIssue(), request.actorName(), StatusActorType.AUTHORITY,
                    note, request.evidenceUrl());
        }
        run.setStatus(AgentRunStatus.APPROVED);
        run.setReviewedBy(request.actorName().trim());
        run.setReviewNote(note);
        run.setReviewedAt(LocalDateTime.now());
        AgentRun saved = runRepository.save(run);
        appendReviewLedger(saved, "APPROVED");
        return toAdmin(saved);
    }

    @Transactional
    public AdminAgentRunResponse reject(Long issueId, Long runId, AgentReviewRequest request) {
        AgentRun run = reviewableRun(issueId, runId);
        String note = defaultText(request.note(), "Authority rejected this recommendation for further review.");
        run.setStatus(AgentRunStatus.REJECTED);
        run.setReviewedBy(request.actorName().trim());
        run.setReviewNote(note);
        run.setReviewedAt(LocalDateTime.now());
        AgentRun saved = runRepository.save(run);
        appendReviewLedger(saved, "REJECTED");
        return toAdmin(saved);
    }

    @Transactional
    public void deleteByIssueId(Long issueId) {
        runRepository.deleteAll(runRepository.findByIssueIdOrderByStartedAtDesc(issueId));
    }

    private void addStep(AgentRun run, int number, String tool, String action, String observation) {
        AgentRunStep saved = stepRepository.save(AgentRunStep.builder()
                .agentRun(run).stepNumber(number).toolName(tool)
                .actionSummary(action).observationSummary(observation).build());
        run.getSteps().add(saved);
    }

    private WardSignal wardSignal(String ward) {
        String normalized = ward == null || ward.isBlank() ? "UNASSIGNED" : ward;
        List<Issue> wardIssues = issueRepository.findByWard(normalized);
        List<Issue> unresolved = wardIssues.stream().filter(item -> item.getStatus() != IssueStatus.RESOLVED).toList();
        int penalty = unresolved.stream().mapToInt(this::healthPenalty).sum();
        return new WardSignal(normalized, Math.max(0, 100 - penalty), unresolved.size());
    }

    private int healthPenalty(Issue issue) {
        int penalty = switch (issue.getSeverity() == null ? IssueSeverity.LOW : issue.getSeverity()) {
            case CRITICAL -> 10;
            case HIGH -> 6;
            case MEDIUM -> 3;
            case LOW -> 1;
        };
        boolean old = issue.getCreatedAt() != null
                && ChronoUnit.DAYS.between(issue.getCreatedAt(), LocalDateTime.now()) > 7;
        return penalty + (old ? 2 : 0);
    }

    private IssueStatus safeProposedStatus(Issue issue, long verifications) {
        return switch (issue.getStatus()) {
            case REPORTED -> isHighImpact(issue) || verifications >= 3 ? IssueStatus.ESCALATED : null;
            case VERIFIED -> IssueStatus.ESCALATED;
            case ESCALATED -> IssueStatus.IN_PROGRESS;
            case IN_PROGRESS, RESOLVED -> null;
        };
    }

    private boolean isHighImpact(Issue issue) {
        return issue.getSeverity() == IssueSeverity.HIGH || issue.getSeverity() == IssueSeverity.CRITICAL
                || (issue.getImpactScore() != null && issue.getImpactScore() >= 70);
    }

    private int confidence(Issue issue, long validMedia, long suspectMedia, long verifications) {
        int value = 35;
        if (issue.getAiGeneratedAt() != null) value += 20;
        if (validMedia > 0) value += 15;
        if (suspectMedia > 0) value -= 20;
        if (issue.getLocationAccuracyMeters() == null || issue.getLocationAccuracyMeters() <= 100) value += 10;
        value += (int) Math.min(15, verifications * 5);
        if (issue.getWard() != null && !"UNASSIGNED".equalsIgnoreCase(issue.getWard())) value += 5;
        return Math.max(20, Math.min(95, value));
    }

    private int targetHours(IssueSeverity severity) {
        if (severity == null) return 72;
        return switch (severity) {
            case CRITICAL -> 12;
            case HIGH -> 24;
            case MEDIUM -> 72;
            case LOW -> 168;
        };
    }

    private String nextAction(Issue issue, IssueStatus proposedStatus, int hours) {
        if (issue.getStatus() == IssueStatus.RESOLVED) return "No further action is proposed; monitor for recurrence.";
        if (proposedStatus == IssueStatus.ESCALATED) return "Authority should review and escalate this case within %d hours.".formatted(hours);
        if (proposedStatus == IssueStatus.IN_PROGRESS) return "Assign a field team and record the work-start update.";
        if (issue.getStatus() == IssueStatus.IN_PROGRESS) return "Continue work and require resolution evidence before closure.";
        return "Continue community monitoring and collect additional evidence or confirmations.";
    }

    private String citizenSummary(Issue issue, int mediaCount, long verifications) {
        return "The Civic Case Manager reviewed your report, confirmed its location context, checked %d attachment(s), searched for related cases, and considered %d community confirmation(s). The case remains %s while any official action awaits authority review."
                .formatted(mediaCount, verifications, issue.getStatus().name().replace('_', ' ').toLowerCase());
    }

    private String adminRecommendation(Issue issue, int duplicates, WardSignal ward,
                                       long verifications, long suspectMedia) {
        return "Route to %s with %s priority. %s Tool evidence: %d related case(s), ward health %d/100, %d community confirmation(s), and %d suspect attachment(s)."
                .formatted(defaultText(issue.getRecommendedDepartment(), "General Civic Administration"),
                        issue.getSeverity() == null ? "MEDIUM" : issue.getSeverity(),
                        defaultText(issue.getSuggestedAction(), "Inspect the location and document the authority response."),
                        duplicates, ward.score(), verifications, suspectMedia);
    }

    private String contextObservation(Issue issue) {
        return "Issue #%d is %s, category %s, AI severity %s, impact %s/100, in %s."
                .formatted(issue.getId(), issue.getStatus(), issue.getCategory(),
                        issue.getSeverity() == null ? "pending" : issue.getSeverity(),
                        issue.getImpactScore() == null ? "pending" : issue.getImpactScore(),
                        defaultText(issue.getWard(), "UNASSIGNED"));
    }

    private AgentRun reviewableRun(Long issueId, Long runId) {
        AgentRun run = runRepository.findById(runId)
                .filter(item -> item.getIssue().getId().equals(issueId))
                .orElseThrow(() -> new ResourceNotFoundException("Agent run not found: " + runId));
        if (run.getStatus() != AgentRunStatus.COMPLETED) {
            throw new WorkflowException(HttpStatus.BAD_REQUEST, "Only a completed, unreviewed recommendation can be reviewed.");
        }
        return run;
    }

    private void appendReviewLedger(AgentRun run, String decision) {
        ledgerService.append("CIVIC_AGENT_RECOMMENDATION_" + decision, "ISSUE", run.getIssue().getId(),
                run.getReviewedBy(), "{\"agentRunId\":%d,\"decision\":\"%s\"}".formatted(run.getId(), decision));
    }

    private CitizenAgentSummaryResponse toCitizen(AgentRun run) {
        return new CitizenAgentSummaryResponse(run.getId(), run.getStatus(), run.getCitizenSummary(),
                run.getRecommendedNextAction(), run.getProposedStatus(), run.getConfidence(),
                run.getCompletedAt() == null ? run.getStartedAt() : run.getCompletedAt());
    }

    private AdminAgentRunResponse toAdmin(AgentRun run) {
        List<AgentRunStepResponse> steps = run.getSteps().stream()
                .map(step -> new AgentRunStepResponse(step.getStepNumber(), step.getToolName(),
                        step.getActionSummary(), step.getObservationSummary(), step.getCreatedAt()))
                .toList();
        return new AdminAgentRunResponse(run.getId(), run.getIssue().getId(), run.getStatus(),
                run.getTriggerType(), run.getModel(), run.getCitizenSummary(), run.getAdminRecommendation(),
                run.getRecommendedNextAction(), run.getProposedDepartment(), run.getProposedPriority(),
                run.getProposedStatus(), run.getTargetResolutionHours(), run.getConfidence(),
                run.isRequiresHumanApproval(), run.getFailureMessage(), run.getReviewedBy(), run.getReviewNote(),
                run.getStartedAt(), run.getCompletedAt(), run.getReviewedAt(), steps);
    }

    private Issue findIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String safeFailure(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) return "The bounded agent investigation could not be completed.";
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private record WardSignal(String name, int score, int unresolved) { }
}
