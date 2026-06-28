package com.communityheroai.issue.service;

import com.communityheroai.exception.EmailDeliveryException;
import com.communityheroai.exception.ResourceNotFoundException;
import com.communityheroai.issue.dto.EmergencyEscalationRequest;
import com.communityheroai.issue.dto.EmergencyEscalationResponse;
import com.communityheroai.issue.dto.IssueEmailPreviewResponse;
import com.communityheroai.issue.dto.IssueEmailSendRequest;
import com.communityheroai.issue.dto.IssueEmailSendResponse;
import com.communityheroai.issue.entity.*;
import com.communityheroai.issue.repository.IssueEmailLogRepository;
import com.communityheroai.issue.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@Service
public class IssueEmailService {
    private final IssueRepository issueRepository;
    private final IssueEmailLogRepository emailLogRepository;
    private final OutboundEmailGateway emailGateway;
    private final IssueStatusHistoryService statusHistoryService;
    private final Map<IssueCategory, String> recipients = new EnumMap<>(IssueCategory.class);

    public IssueEmailService(
            IssueRepository issueRepository,
            IssueEmailLogRepository emailLogRepository,
            OutboundEmailGateway emailGateway,
            IssueStatusHistoryService statusHistoryService,
            @Value("${app.email.authorities.pothole:}") String pothole,
            @Value("${app.email.authorities.water-leakage:}") String waterLeakage,
            @Value("${app.email.authorities.streetlight-damage:}") String streetlightDamage,
            @Value("${app.email.authorities.waste-management:}") String wasteManagement,
            @Value("${app.email.authorities.drainage-issue:}") String drainageIssue) {
        this.issueRepository = issueRepository;
        this.emailLogRepository = emailLogRepository;
        this.emailGateway = emailGateway;
        this.statusHistoryService = statusHistoryService;
        recipients.put(IssueCategory.POTHOLE, pothole);
        recipients.put(IssueCategory.WATER_LEAKAGE, waterLeakage);
        recipients.put(IssueCategory.STREETLIGHT_DAMAGE, streetlightDamage);
        recipients.put(IssueCategory.WASTE_MANAGEMENT, wasteManagement);
        recipients.put(IssueCategory.DRAINAGE_ISSUE, drainageIssue);
    }

    public IssueEmailPreviewResponse preview(Long issueId) {
        Issue issue = findIssue(issueId);
        String recipient = recipients.get(issue.getCategory());
        boolean configured = emailGateway.isConfigured() && notBlank(recipient);
        String warning = configured
                ? "Review the authority, subject, and complaint before sending. This action will be recorded."
                : notBlank(recipient) ? emailGateway.configurationWarning()
                : "No authority recipient is configured for this issue category.";
        return new IssueEmailPreviewResponse(
                configured,
                recipient,
                "Civic issue report #%d: %s".formatted(issue.getId(), issue.getTitle()),
                complaintBody(issue),
                warning
        );
    }

    public IssueEmailSendResponse send(Long issueId, IssueEmailSendRequest request) {
        Issue issue = findIssue(issueId);
        IssueEmailPreviewResponse preview = preview(issueId);
        if (!preview.configured()) {
            throw new EmailDeliveryException(HttpStatus.SERVICE_UNAVAILABLE, preview.warning());
        }
        if (!Objects.equals(preview.recipient(), request.recipient())
                || !Objects.equals(preview.subject(), request.subject())
                || !Objects.equals(preview.body(), request.body())) {
            throw new EmailDeliveryException(HttpStatus.BAD_REQUEST,
                    "Email content changed after preview. Refresh and confirm it again.");
        }
        return deliver(issue, preview, preview.subject(), preview.body(),
                null,
                "Community Hero AI",
                "Complaint emailed to configured authority: " + preview.recipient());
    }

    public EmergencyEscalationResponse requestEmergencyEscalation(
            Long issueId, EmergencyEscalationRequest request) {
        Issue issue = findIssue(issueId);
        String actor = notBlank(request.requesterName()) ? request.requesterName().trim() : "Concerned citizen";
        String note = "Citizen requested emergency escalation. Reason: " + request.reason().trim();
        boolean urgent = issue.getSeverity() == IssueSeverity.CRITICAL
                || (issue.getImpactScore() != null && issue.getImpactScore() >= 85)
                || issue.getStatus() == IssueStatus.VERIFIED;
        if (!urgent && !request.overrideAiAssessment()) {
            return new EmergencyEscalationResponse(false, false, true,
                    "AI did not classify this report as a high-confidence emergency. If the danger is real, review your reason and choose Send escalation anyway.",
                    null, null, issue.getStatus());
        }

        if (!urgent) note += " Citizen explicitly overrode the AI urgency assessment.";

        IssueEmailPreviewResponse preview = preview(issueId);
        if (!preview.configured()) {
            statusHistoryService.recordNote(issue, actor, StatusActorType.COMMUNITY,
                    note + " Email delivery is not configured, so admin review is required.",
                    null);
            return new EmergencyEscalationResponse(false, true, false,
                    "Emergency request recorded, but authority email is not configured. Admin review is required.",
                    null, null, issue.getStatus());
        }

        IssueEmailSendResponse sent = deliver(issue, preview,
                "[EMERGENCY ESCALATION] " + preview.subject(),
                emergencyBody(issue, request, preview.body()),
                request.requesterEmail(),
                "Emergency Escalation Agent",
                "Emergency complaint emailed to configured authority after citizen request by "
                        + actor + ": " + preview.recipient()
                        + (!urgent ? ". Citizen explicitly overrode the AI urgency assessment." : ""));
        return new EmergencyEscalationResponse(true, false, false,
                "Emergency complaint emailed to the configured authority.",
                sent.recipient(), sent.sentAt(), sent.issueStatus());
    }

    private IssueEmailSendResponse deliver(Issue issue, IssueEmailPreviewResponse preview,
                                           String subject, String body,
                                           String replyTo,
                                           String historyActor, String historyNote) {
        emailLogRepository.findTopByIssueIdAndStatusOrderByCreatedAtDesc(issue.getId(), IssueEmailStatus.SENT)
                .filter(log -> log.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24)))
                .ifPresent(log -> {
                    throw new EmailDeliveryException(HttpStatus.TOO_MANY_REQUESTS,
                            "An authority email was already sent for this issue within the last 24 hours.");
                });

        IssueEmailPreviewResponse deliveredPreview = new IssueEmailPreviewResponse(
                preview.configured(), preview.recipient(), subject, body, preview.warning());
        try {
            emailGateway.send(new OutboundEmailMessage(preview.recipient(), subject, body, replyTo));
        } catch (RuntimeException ex) {
            saveLog(issue, deliveredPreview, IssueEmailStatus.FAILED,
                    ex.getMessage() == null ? "Email provider delivery failed" : ex.getMessage());
            throw new EmailDeliveryException(HttpStatus.BAD_GATEWAY,
                    "The email provider could not deliver this complaint. Please try again later.");
        }

        LocalDateTime sentAt = LocalDateTime.now();
        saveLog(issue, deliveredPreview, IssueEmailStatus.SENT, null);
        issue.setAuthorityEmailSentAt(sentAt);
        issue.setAuthorityEmailRecipient(preview.recipient());
        IssueStatus previousStatus = issue.getStatus();
        if (issue.getStatus() == IssueStatus.REPORTED || issue.getStatus() == IssueStatus.VERIFIED) {
            issue.setStatus(IssueStatus.ESCALATED);
        }
        issueRepository.save(issue);
        if (previousStatus != issue.getStatus()) {
            statusHistoryService.recordTransition(issue, previousStatus, issue.getStatus(),
                    historyActor, StatusActorType.SYSTEM, historyNote, null);
        } else {
            statusHistoryService.recordNote(issue, historyActor, StatusActorType.SYSTEM, historyNote, null);
        }
        return new IssueEmailSendResponse(
                "Complaint emailed to the configured authority.", preview.recipient(), sentAt, issue.getStatus());
    }

    private String emergencyBody(Issue issue, EmergencyEscalationRequest request, String baseComplaint) {
        String contact = notBlank(request.requesterEmail()) ? request.requesterEmail().trim() : "Not provided";
        return """
                EMERGENCY ESCALATION REQUEST

                Citizen requester: %s
                Contact email: %s
                Emergency reason: %s

                Issue severity: %s
                Impact score: %s
                Current status: %s

                Original complaint:

                %s
                """.formatted(
                request.requesterName().trim(),
                contact,
                request.reason().trim(),
                issue.getSeverity() == null ? "Pending" : issue.getSeverity(),
                issue.getImpactScore() == null ? "Pending" : issue.getImpactScore(),
                issue.getStatus(),
                baseComplaint);
    }

    private String complaintBody(Issue issue) {
        if (notBlank(issue.getComplaintDraft())) return issue.getComplaintDraft();
        String location = notBlank(issue.getFormattedAddress())
                ? issue.getFormattedAddress()
                : "%s, %s (%.6f, %.6f)".formatted(issue.getLocality(), issue.getCity(),
                issue.getLatitude(), issue.getLongitude());
        return """
                Respected Sir/Madam,

                I would like to report the following civic issue:

                %s

                Description: %s
                Location: %s
                Ward: %s

                Kindly inspect and resolve this issue at the earliest.

                Sincerely,
                A concerned citizen
                """.formatted(issue.getTitle(), issue.getDescription(), location, issue.getWard());
    }

    private void saveLog(Issue issue, IssueEmailPreviewResponse preview,
                         IssueEmailStatus status, String errorMessage) {
        emailLogRepository.save(IssueEmailLog.builder()
                .issue(issue)
                .recipient(preview.recipient())
                .subject(preview.subject())
                .body(preview.body())
                .status(status)
                .errorMessage(errorMessage)
                .build());
    }

    private Issue findIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
