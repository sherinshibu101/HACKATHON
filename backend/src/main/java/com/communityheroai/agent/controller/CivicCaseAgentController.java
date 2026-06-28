package com.communityheroai.agent.controller;

import com.communityheroai.agent.dto.*;
import com.communityheroai.agent.entity.AgentTrigger;
import com.communityheroai.agent.service.CivicCaseAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CivicCaseAgentController {
    private final CivicCaseAgentService agentService;

    @GetMapping("/api/issues/{issueId}/agent/public-summary")
    public ResponseEntity<CitizenAgentSummaryResponse> publicSummary(@PathVariable Long issueId) {
        return agentService.latestPublic(issueId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/api/admin/issues/{issueId}/agent-runs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminAgentRunResponse> runs(@PathVariable Long issueId) {
        return agentService.runs(issueId);
    }

    @GetMapping("/api/admin/issues/{issueId}/agent-runs/latest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminAgentRunResponse> latest(@PathVariable Long issueId) {
        return agentService.latestAdmin(issueId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/api/admin/issues/{issueId}/agent-runs")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminAgentRunResponse run(@PathVariable Long issueId) {
        return agentService.run(issueId, AgentTrigger.MANUAL_ADMIN);
    }

    @PostMapping("/api/admin/issues/{issueId}/agent-runs/{runId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminAgentRunResponse approve(@PathVariable Long issueId, @PathVariable Long runId,
                                         @Valid @RequestBody AgentReviewRequest request) {
        return agentService.approve(issueId, runId, request);
    }

    @PostMapping("/api/admin/issues/{issueId}/agent-runs/{runId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminAgentRunResponse reject(@PathVariable Long issueId, @PathVariable Long runId,
                                        @Valid @RequestBody AgentReviewRequest request) {
        return agentService.reject(issueId, runId, request);
    }
}
