package com.communityheroai.issue.controller;

import com.communityheroai.issue.dto.VisualFactCheckResponse;
import com.communityheroai.issue.service.VisualFactCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues/{issueId}/visual-fact-check")
@RequiredArgsConstructor
@CrossOrigin
public class VisualFactCheckController {
    private final VisualFactCheckService factCheckService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public VisualFactCheckResponse analyze(@PathVariable Long issueId) {
        return factCheckService.analyze(issueId);
    }

    @GetMapping
    public List<VisualFactCheckResponse> history(@PathVariable Long issueId) {
        return factCheckService.history(issueId);
    }

    @GetMapping("/latest")
    public VisualFactCheckResponse latest(@PathVariable Long issueId) {
        return factCheckService.latest(issueId);
    }
}
