package com.communityheroai.issue.service;

import com.communityheroai.ai.GeminiAnalysisService;
import com.communityheroai.ai.dto.GeminiVisualFactCheckResponse;
import com.communityheroai.ai.prompt.GeminiPrompts;
import com.communityheroai.exception.ResourceNotFoundException;
import com.communityheroai.issue.dto.VisualFactCheckResponse;
import com.communityheroai.issue.entity.*;
import com.communityheroai.issue.repository.IssueMediaRepository;
import com.communityheroai.issue.repository.IssueRepository;
import com.communityheroai.issue.repository.IssueVisualFactCheckRepository;
import com.communityheroai.upload.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VisualFactCheckService {
    private final IssueRepository issueRepository;
    private final IssueMediaRepository mediaRepository;
    private final IssueVisualFactCheckRepository factCheckRepository;
    private final MediaStorageService storageService;
    private final GeminiAnalysisService geminiAnalysisService;
    private final RestClient.Builder restClientBuilder;

    @Value("${google.street-view.api-key:}")
    private String streetViewApiKey;
    @Value("${google.street-view.base-url:https://maps.googleapis.com/maps/api/streetview}")
    private String streetViewBaseUrl;

    @Transactional
    public VisualFactCheckResponse analyze(Long issueId) {
        Issue issue = findIssue(issueId);
        IssueMedia image = mediaRepository.findByIssueIdOrderByCreatedAtAsc(issueId).stream()
                .filter(media -> media.getMediaType() == IssueMediaType.IMAGE)
                .findFirst()
                .orElse(null);
        if (image == null) {
            return saveUnavailable(issue, null,
                    "Upload at least one citizen image before running the Street-View Fact-Checker.");
        }
        if (streetViewApiKey == null || streetViewApiKey.isBlank()) {
            return saveUnavailable(issue, image,
                    "GOOGLE_STREET_VIEW_API_KEY is not configured, so baseline imagery could not be fetched.");
        }

        String baselineUrl = baselineUrl(issue);
        try {
            byte[] baselineImage = fetchBaseline(issue);
            byte[] userImage = storageService.read(image.getStorageKey()).bytes();
            String prompt = GeminiPrompts.buildVisualFactCheckPrompt(
                    issue.getTitle(), issue.getCategory().name(), address(issue), issue.getLatitude(), issue.getLongitude());
            GeminiVisualFactCheckResponse analysis = geminiAnalysisService.visualFactCheck(
                    prompt, userImage, image.getContentType(), baselineImage, MediaType.IMAGE_JPEG_VALUE);
            IssueVisualFactCheck saved = factCheckRepository.save(IssueVisualFactCheck.builder()
                    .issue(issue)
                    .issueMedia(image)
                    .status(VisualFactCheckStatus.COMPLETED)
                    .verificationResult(analysis.verificationResult())
                    .confidenceScore(analysis.confidenceScore())
                    .baselineImageUrl(baselineUrl)
                    .userImageUrl(image.getMediaUrl())
                    .reasoningReport(analysis.reasoningReport())
                    .riskFlags(analysis.riskFlags())
                    .build());
            return toResponse(saved, "Street-View visual fact-check completed.");
        } catch (Exception ex) {
            return saveFailed(issue, image, baselineUrl,
                    "Street-View visual fact-check failed: " + safeMessage(ex));
        }
    }

    @Transactional(readOnly = true)
    public List<VisualFactCheckResponse> history(Long issueId) {
        if (!issueRepository.existsById(issueId)) {
            throw new ResourceNotFoundException("Issue not found: " + issueId);
        }
        return factCheckRepository.findByIssueIdOrderByCreatedAtDesc(issueId).stream()
                .map(check -> toResponse(check, "Street-View visual fact-check loaded."))
                .toList();
    }

    @Transactional(readOnly = true)
    public VisualFactCheckResponse latest(Long issueId) {
        if (!issueRepository.existsById(issueId)) {
            throw new ResourceNotFoundException("Issue not found: " + issueId);
        }
        return factCheckRepository.findTopByIssueIdOrderByCreatedAtDesc(issueId)
                .map(check -> toResponse(check, "Street-View visual fact-check loaded."))
                .orElse(null);
    }

    @Transactional
    public void deleteByIssueId(Long issueId) {
        factCheckRepository.deleteByIssueId(issueId);
    }

    private byte[] fetchBaseline(Issue issue) {
        return restClientBuilder.build().get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.googleapis.com")
                        .path("/maps/api/streetview")
                        .queryParam("size", "640x640")
                        .queryParam("location", issue.getLatitude() + "," + issue.getLongitude())
                        .queryParam("fov", "80")
                        .queryParam("pitch", "0")
                        .queryParam("return_error_code", "true")
                        .queryParam("key", streetViewApiKey)
                        .build())
                .retrieve()
                .body(byte[].class);
    }

    private String baselineUrl(Issue issue) {
        return streetViewBaseUrl
                + "?size=640x640&location=" + issue.getLatitude() + "," + issue.getLongitude()
                + "&fov=80&pitch=0&return_error_code=true&key=CONFIGURED";
    }

    private VisualFactCheckResponse saveUnavailable(Issue issue, IssueMedia image, String message) {
        IssueVisualFactCheck saved = factCheckRepository.save(IssueVisualFactCheck.builder()
                .issue(issue)
                .issueMedia(image)
                .status(VisualFactCheckStatus.UNAVAILABLE)
                .verificationResult(VisualVerificationResult.UNAVAILABLE)
                .confidenceScore(0)
                .userImageUrl(image == null ? null : image.getMediaUrl())
                .reasoningReport(message)
                .riskFlags("Configuration or image evidence missing.")
                .build());
        return toResponse(saved, message);
    }

    private VisualFactCheckResponse saveFailed(Issue issue, IssueMedia image, String baselineUrl, String message) {
        IssueVisualFactCheck saved = factCheckRepository.save(IssueVisualFactCheck.builder()
                .issue(issue)
                .issueMedia(image)
                .status(VisualFactCheckStatus.FAILED)
                .verificationResult(VisualVerificationResult.NEEDS_REVIEW)
                .confidenceScore(0)
                .baselineImageUrl(baselineUrl)
                .userImageUrl(image == null ? null : image.getMediaUrl())
                .reasoningReport(message)
                .riskFlags("Manual review recommended.")
                .build());
        return toResponse(saved, message);
    }

    private VisualFactCheckResponse toResponse(IssueVisualFactCheck check, String message) {
        return VisualFactCheckResponse.builder()
                .id(check.getId())
                .issueId(check.getIssue().getId())
                .mediaId(check.getIssueMedia() == null ? null : check.getIssueMedia().getId())
                .status(check.getStatus())
                .verificationResult(check.getVerificationResult())
                .confidenceScore(check.getConfidenceScore())
                .baselineImageUrl(check.getBaselineImageUrl())
                .userImageUrl(check.getUserImageUrl())
                .reasoningReport(check.getReasoningReport())
                .riskFlags(check.getRiskFlags())
                .message(message)
                .createdAt(check.getCreatedAt())
                .build();
    }

    private Issue findIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));
    }

    private String address(Issue issue) {
        if (issue.getFormattedAddress() != null && !issue.getFormattedAddress().isBlank()) return issue.getFormattedAddress();
        return String.join(", ", java.util.stream.Stream.of(issue.getLocality(), issue.getCity(), issue.getState(), issue.getCountry())
                .filter(value -> value != null && !value.isBlank()).toList());
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }
}
