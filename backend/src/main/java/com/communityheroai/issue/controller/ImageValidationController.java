package com.communityheroai.issue.controller;

import com.communityheroai.issue.dto.ImageValidationResponse;
import com.communityheroai.issue.entity.IssueCategory;
import com.communityheroai.issue.service.ImageValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@CrossOrigin
public class ImageValidationController {
    private final ImageValidationService imageValidationService;

    @PostMapping("/validate-image")
    public ImageValidationResponse validateImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") IssueCategory category) {
        ImageValidationService.ValidationResult result = imageValidationService.validate(file, category);
        return ImageValidationResponse.builder()
                .validationStatus(result.status())
                .validationConfidence(result.confidence())
                .validationSummary(result.summary())
                .validationLabels(result.labels())
                .validatedAt(result.validatedAt())
                .build();
    }
}
