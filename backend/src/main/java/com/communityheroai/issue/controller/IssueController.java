package com.communityheroai.issue.controller;

import com.communityheroai.issue.dto.*;
import com.communityheroai.issue.service.IssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@CrossOrigin
public class IssueController {
    private final IssueService issueService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse create(@Valid @RequestBody IssueRequest request) {
        return issueService.create(request);
    }

    @PostMapping("/check-duplicates")
    public DuplicateCheckResponse checkDuplicates(@Valid @RequestBody IssueRequest request) {
        return issueService.checkDuplicates(request);
    }

    @GetMapping
    public List<IssueResponse> all() {
        return issueService.findAll();
    }

    @GetMapping("/{id}")
    public IssueResponse one(@PathVariable Long id) {
        return issueService.findById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public IssueResponse update(@PathVariable Long id, @Valid @RequestBody IssueRequest request) {
        return issueService.update(id, request);
    }

    @PostMapping("/{id}/analyze")
    @PreAuthorize("hasRole('ADMIN')")
    public IssueResponse analyze(@PathVariable Long id) {
        return issueService.analyze(id);
    }

    @GetMapping("/{id}/duplicates")
    public List<PossibleDuplicateIssueResponse> duplicates(@PathVariable Long id) {
        return issueService.duplicates(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        issueService.delete(id);
    }
}
