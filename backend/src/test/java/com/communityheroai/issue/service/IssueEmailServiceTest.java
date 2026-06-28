package com.communityheroai.issue.service;

import com.communityheroai.exception.EmailDeliveryException;
import com.communityheroai.issue.dto.IssueEmailPreviewResponse;
import com.communityheroai.issue.dto.IssueEmailSendRequest;
import com.communityheroai.issue.dto.EmergencyEscalationRequest;
import com.communityheroai.issue.entity.Issue;
import com.communityheroai.issue.entity.IssueCategory;
import com.communityheroai.issue.entity.IssueStatus;
import com.communityheroai.issue.repository.IssueEmailLogRepository;
import com.communityheroai.issue.repository.IssueRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IssueEmailServiceTest {
    @Test
    void confirmedPreviewSendsToConfiguredAuthorityAndEscalatesIssue() {
        IssueRepository issueRepository = mock(IssueRepository.class);
        IssueEmailLogRepository logRepository = mock(IssueEmailLogRepository.class);
        OutboundEmailGateway emailGateway = mock(OutboundEmailGateway.class);
        IssueStatusHistoryService historyService = mock(IssueStatusHistoryService.class);
        Issue issue = Issue.builder().id(4L).title("Dangerous pothole")
                .description("Deep road damage").category(IssueCategory.POTHOLE)
                .status(IssueStatus.REPORTED).locality("Market").city("Delhi")
                .ward("Ward 12").latitude(28.61).longitude(77.21)
                .complaintDraft("Please repair this dangerous pothole.").build();
        when(issueRepository.findById(4L)).thenReturn(Optional.of(issue));
        when(logRepository.findTopByIssueIdAndStatusOrderByCreatedAtDesc(anyLong(), any()))
                .thenReturn(Optional.empty());
        IssueEmailService service = service(issueRepository, logRepository, emailGateway, historyService, true);
        IssueEmailPreviewResponse preview = service.preview(4L);

        service.send(4L, new IssueEmailSendRequest(
                preview.recipient(), preview.subject(), preview.body(), true));

        verify(emailGateway).send(any(OutboundEmailMessage.class));
        verify(issueRepository).save(issue);
        assertThat(issue.getStatus()).isEqualTo(IssueStatus.ESCALATED);
        assertThat(issue.getAuthorityEmailRecipient()).isEqualTo("roads@authority.test");
        verify(historyService).recordTransition(eq(issue), eq(IssueStatus.REPORTED), eq(IssueStatus.ESCALATED),
                eq("Community Hero AI"), any(), contains("roads@authority.test"), isNull());
    }

    @Test
    void disabledConfigurationCannotSend() {
        IssueRepository issueRepository = mock(IssueRepository.class);
        IssueEmailLogRepository logRepository = mock(IssueEmailLogRepository.class);
        OutboundEmailGateway emailGateway = mock(OutboundEmailGateway.class);
        IssueStatusHistoryService historyService = mock(IssueStatusHistoryService.class);
        Issue issue = Issue.builder().id(4L).title("Pothole").description("Damage")
                .category(IssueCategory.POTHOLE).status(IssueStatus.REPORTED)
                .locality("Market").city("Delhi").ward("Ward 12")
                .latitude(28.61).longitude(77.21).build();
        when(issueRepository.findById(4L)).thenReturn(Optional.of(issue));
        IssueEmailService service = service(issueRepository, logRepository, emailGateway, historyService, false);
        IssueEmailPreviewResponse preview = service.preview(4L);

        assertThatThrownBy(() -> service.send(4L, new IssueEmailSendRequest(
                preview.recipient(), preview.subject(), preview.body(), true)))
                .isInstanceOf(EmailDeliveryException.class);
        verify(emailGateway, never()).send(any());
    }

    @Test
    void nonUrgentEmergencyRequiresConfirmationThenAllowsCitizenOverride() {
        IssueRepository issueRepository = mock(IssueRepository.class);
        IssueEmailLogRepository logRepository = mock(IssueEmailLogRepository.class);
        OutboundEmailGateway emailGateway = mock(OutboundEmailGateway.class);
        IssueStatusHistoryService historyService = mock(IssueStatusHistoryService.class);
        Issue issue = Issue.builder().id(7L).title("Streetlight outage")
                .description("Dark road").category(IssueCategory.STREETLIGHT_DAMAGE)
                .status(IssueStatus.REPORTED).severity(com.communityheroai.issue.entity.IssueSeverity.LOW)
                .impactScore(30).locality("School Road").city("Kottayam")
                .latitude(9.59).longitude(76.52).build();
        when(issueRepository.findById(7L)).thenReturn(Optional.of(issue));
        when(logRepository.findTopByIssueIdAndStatusOrderByCreatedAtDesc(anyLong(), any()))
                .thenReturn(Optional.empty());
        IssueEmailService service = service(issueRepository, logRepository, emailGateway, historyService, true);

        var warning = service.requestEmergencyEscalation(7L,
                new EmergencyEscalationRequest("Citizen", "citizen@example.com", "Children are in danger", false));
        assertThat(warning.confirmationRequired()).isTrue();
        verify(emailGateway, never()).send(any());

        var sent = service.requestEmergencyEscalation(7L,
                new EmergencyEscalationRequest("Citizen", "citizen@example.com", "Children are in danger", true));
        assertThat(sent.emailSent()).isTrue();
        verify(emailGateway).send(any(OutboundEmailMessage.class));
    }

    private IssueEmailService service(IssueRepository issues, IssueEmailLogRepository logs,
                                      OutboundEmailGateway emailGateway, IssueStatusHistoryService historyService,
                                      boolean enabled) {
        when(emailGateway.isConfigured()).thenReturn(enabled);
        when(emailGateway.configurationWarning()).thenReturn("Email provider is not configured.");
        return new IssueEmailService(issues, logs, emailGateway, historyService,
                "roads@authority.test", "water@authority.test",
                "lights@authority.test", "waste@authority.test", "drains@authority.test");
    }
}
