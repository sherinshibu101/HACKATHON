package com.communityheroai.issue.controller;

import com.communityheroai.issue.dto.IssueMediaResponse;
import com.communityheroai.issue.service.IssueMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/issues/{issueId}/media")
@RequiredArgsConstructor
@CrossOrigin
public class IssueMediaController {
    private final IssueMediaService mediaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<IssueMediaResponse> upload(
            @PathVariable Long issueId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "validationResults", required = false) String validationResults) {
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        return mediaService.upload(issueId, files, baseUrl, validationResults);
    }

    @GetMapping
    public List<IssueMediaResponse> all(@PathVariable Long issueId) {
        return mediaService.findByIssue(issueId);
    }

    @DeleteMapping("/{mediaId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long issueId, @PathVariable Long mediaId) {
        mediaService.delete(issueId, mediaId);
    }
}
