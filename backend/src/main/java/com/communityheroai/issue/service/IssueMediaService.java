package com.communityheroai.issue.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.communityheroai.exception.ResourceNotFoundException;
import com.communityheroai.issue.dto.IssueMediaResponse;
import com.communityheroai.issue.entity.ImageValidationStatus;
import com.communityheroai.issue.entity.Issue;
import com.communityheroai.issue.entity.IssueMedia;
import com.communityheroai.issue.entity.IssueMediaType;
import com.communityheroai.issue.entity.MediaProcessingStatus;
import com.communityheroai.issue.repository.IssueMediaRepository;
import com.communityheroai.issue.repository.IssueRepository;
import com.communityheroai.upload.MediaStorageService;
import com.communityheroai.agent.entity.AgentTrigger;
import com.communityheroai.agent.service.CivicCaseAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueMediaService {
    private static final long MAX_IMAGES_PER_ISSUE = 5;
    private static final long MAX_VIDEOS_PER_ISSUE = 1;
    private final IssueRepository issueRepository;
    private final IssueMediaRepository mediaRepository;
    private final MediaStorageService storageService;
    private final ObjectMapper objectMapper;
    private final CivicCaseAgentService civicCaseAgentService;

    @Transactional
    public List<IssueMediaResponse> upload(Long issueId, List<MultipartFile> files, String baseUrl,
                                           String validationResultsJson) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Select at least one image or video.");
        }
        List<ClientValidationResult> validationResults = parseValidationResults(validationResultsJson);

        long incomingImages = files.stream().filter(file -> isType(file, "image/")).count();
        long incomingVideos = files.stream().filter(file -> isType(file, "video/")).count();
        if (incomingImages + incomingVideos != files.size()) {
            throw new IllegalArgumentException("Only image and video files are supported.");
        }
        if (mediaRepository.countByIssueIdAndMediaType(issueId, IssueMediaType.IMAGE) + incomingImages
                > MAX_IMAGES_PER_ISSUE) {
            throw new IllegalArgumentException("An issue can contain at most 5 images.");
        }
        if (mediaRepository.countByIssueIdAndMediaType(issueId, IssueMediaType.VIDEO) + incomingVideos
                > MAX_VIDEOS_PER_ISSUE) {
            throw new IllegalArgumentException("An issue can contain at most 1 video.");
        }

        List<String> storedKeys = new ArrayList<>();
        try {
            List<IssueMedia> media = new ArrayList<>();
            for (int index = 0; index < files.size(); index++) {
                MultipartFile file = files.get(index);
                MediaStorageService.StoredMedia stored = storageService.store(file);
                storedKeys.add(stored.storageKey());
                IssueMedia.IssueMediaBuilder builder = IssueMedia.builder()
                        .issue(issue)
                        .mediaType(stored.type())
                        .mediaUrl(baseUrl + "/uploads/" + stored.storageKey())
                        .storageKey(stored.storageKey())
                        .originalFilename(safeFilename(file.getOriginalFilename()))
                        .contentType(file.getContentType())
                        .fileSize(file.getSize())
                        .processingStatus(MediaProcessingStatus.READY);
                if (stored.type() == IssueMediaType.IMAGE) {
                    ClientValidationResult validation = validationResults.size() > index ? validationResults.get(index) : null;
                    builder.validationStatus(validationStatus(validation))
                            .validationConfidence(validation == null ? 0 : validation.validationConfidence())
                            .validationSummary(validation == null || isBlank(validation.validationSummary())
                                    ? "No client-side image validation result was provided."
                                    : validation.validationSummary())
                            .validationLabels(validation == null ? "" : validation.validationLabels())
                            .validatedAt(validatedAt(validation));
                } else {
                    builder.validationStatus(com.communityheroai.issue.entity.ImageValidationStatus.NOT_APPLICABLE)
                            .validationSummary("Vision label validation applies to images only.");
                }
                media.add(builder.build());
            }
            List<IssueMediaResponse> responses = mediaRepository.saveAll(media).stream().map(this::toResponse).toList();
            civicCaseAgentService.run(issueId, AgentTrigger.EVIDENCE_UPDATED);
            return responses;
        } catch (RuntimeException ex) {
            storedKeys.forEach(this::deleteQuietly);
            throw ex;
        }
    }

    public List<IssueMediaResponse> findByIssue(Long issueId) {
        if (!issueRepository.existsById(issueId)) {
            throw new ResourceNotFoundException("Issue not found: " + issueId);
        }
        return responsesForIssue(issueId);
    }

    public List<IssueMediaResponse> responsesForIssue(Long issueId) {
        if (issueId == null) return List.of();
        return mediaRepository.findByIssueIdOrderByCreatedAtAsc(issueId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long issueId, Long mediaId) {
        IssueMedia media = mediaRepository.findById(mediaId)
                .filter(item -> item.getIssue().getId().equals(issueId))
                .orElseThrow(() -> new ResourceNotFoundException("Media not found: " + mediaId));
        mediaRepository.delete(media);
        storageService.delete(media.getStorageKey());
    }

    @Transactional
    public void deleteAllForIssue(Long issueId) {
        List<IssueMedia> media = mediaRepository.findByIssueIdOrderByCreatedAtAsc(issueId);
        mediaRepository.deleteAll(media);
        media.forEach(item -> deleteQuietly(item.getStorageKey()));
    }

    private IssueMediaResponse toResponse(IssueMedia media) {
        return IssueMediaResponse.builder()
                .id(media.getId())
                .issueId(media.getIssue().getId())
                .mediaType(media.getMediaType())
                .mediaUrl(media.getMediaUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .originalFilename(media.getOriginalFilename())
                .contentType(media.getContentType())
                .fileSize(media.getFileSize())
                .processingStatus(media.getProcessingStatus())
                .validationStatus(media.getValidationStatus())
                .validationConfidence(media.getValidationConfidence())
                .validationSummary(media.getValidationSummary())
                .validationLabels(media.getValidationLabels())
                .validatedAt(media.getValidatedAt())
                .createdAt(media.getCreatedAt())
                .build();
    }

    private boolean isType(MultipartFile file, String prefix) {
        return file != null && file.getContentType() != null && file.getContentType().startsWith(prefix);
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) return "media";
        String filename = Path.of(originalFilename).getFileName().toString();
        return filename.length() <= 255 ? filename : filename.substring(filename.length() - 255);
    }

    private List<ClientValidationResult> parseValidationResults(String json) {
        if (isBlank(json)) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private ImageValidationStatus validationStatus(ClientValidationResult validation) {
        return validation == null || validation.validationStatus() == null
                ? ImageValidationStatus.UNAVAILABLE
                : validation.validationStatus();
    }

    private LocalDateTime validatedAt(ClientValidationResult validation) {
        if (validation == null || isBlank(validation.validatedAt())) return LocalDateTime.now();
        try {
            return Instant.parse(validation.validatedAt()).atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (RuntimeException ignored) {
            return LocalDateTime.now();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void deleteQuietly(String storageKey) {
        try {
            storageService.delete(storageKey);
        } catch (RuntimeException ignored) {
            // Database consistency is more important than an orphaned local file.
        }
    }

    private record ClientValidationResult(
            ImageValidationStatus validationStatus,
            Integer validationConfidence,
            String validationSummary,
            String validationLabels,
            String validatedAt
    ) {}
}
