package com.communityheroai.issue.controller;

import com.communityheroai.issue.dto.AuthorityWorkflowResponse;
import com.communityheroai.issue.dto.IssueStatusUpdateRequest;
import com.communityheroai.issue.service.AuthorityWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/issues/{id}")
@RequiredArgsConstructor
@CrossOrigin
public class IssueStatusController {
    private final AuthorityWorkflowService workflowService;

    @GetMapping("/status-workflow")
    public AuthorityWorkflowResponse workflow(@PathVariable Long id) {
        return workflowService.workflow(id);
    }

    @PatchMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthorityWorkflowResponse updateStatus(@PathVariable Long id,
                                                  @Valid @RequestBody IssueStatusUpdateRequest request) {
        return workflowService.updateStatus(id, request);
    }
}
