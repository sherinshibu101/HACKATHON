package com.communityheroai.issue.controller;

import com.communityheroai.issue.dto.IssueVerificationRequest;
import com.communityheroai.issue.dto.IssueVerificationResponse;
import com.communityheroai.issue.service.IssueVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues/{issueId}")
@RequiredArgsConstructor
@CrossOrigin
public class IssueVerificationController {
    private final IssueVerificationService verificationService;

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueVerificationResponse verify(
            @PathVariable Long issueId, @Valid @RequestBody IssueVerificationRequest request) {
        return verificationService.verify(issueId, request);
    }

    @GetMapping("/verifications")
    public List<IssueVerificationResponse> all(@PathVariable Long issueId) {
        return verificationService.findByIssue(issueId);
    }
}
