package com.communityheroai.issue.controller;

import com.communityheroai.issue.dto.IssueEmailPreviewResponse;
import com.communityheroai.issue.dto.IssueEmailSendRequest;
import com.communityheroai.issue.dto.IssueEmailSendResponse;
import com.communityheroai.issue.dto.EmergencyEscalationRequest;
import com.communityheroai.issue.dto.EmergencyEscalationResponse;
import com.communityheroai.issue.service.IssueEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/issues/{issueId}/authority-email")
@RequiredArgsConstructor
@CrossOrigin
public class IssueEmailController {
    private final IssueEmailService issueEmailService;

    @GetMapping("/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public IssueEmailPreviewResponse preview(@PathVariable Long issueId) {
        return issueEmailService.preview(issueId);
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    public IssueEmailSendResponse send(
            @PathVariable Long issueId, @Valid @RequestBody IssueEmailSendRequest request) {
        return issueEmailService.send(issueId, request);
    }

    @PostMapping("/emergency-request")
    public EmergencyEscalationResponse requestEmergencyEscalation(
            @PathVariable Long issueId, @Valid @RequestBody EmergencyEscalationRequest request) {
        return issueEmailService.requestEmergencyEscalation(issueId, request);
    }
}
