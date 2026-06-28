package com.communityheroai.issue.controller;

import com.communityheroai.issue.dto.ResolutionCertificateResponse;
import com.communityheroai.issue.service.ResolutionCertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/issues/{id}/certificate")
@RequiredArgsConstructor
@CrossOrigin
public class ResolutionCertificateController {
    private final ResolutionCertificateService certificateService;

    @GetMapping
    public ResolutionCertificateResponse certificate(@PathVariable Long id) {
        return certificateService.certificate(id);
    }
}
