package com.communityheroai.issue.controller;

import com.communityheroai.issue.dto.*;
import com.communityheroai.issue.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin
public class DashboardController {
    private final IssueService issueService;

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() { return issueService.summary(); }

    @GetMapping("/category-stats")
    public List<CategoryStatResponse> categories() { return issueService.categoryStats(); }

    @GetMapping("/ward-stats")
    public List<WardStatResponse> wards() { return issueService.wardStats(); }

    @GetMapping("/ward-health")
    public List<WardHealthResponse> wardHealth() { return issueService.wardHealth(); }

    @GetMapping("/high-impact")
    public List<IssueResponse> highImpact() { return issueService.highImpactIssues(); }
}
