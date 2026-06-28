package com.communityheroai.issue.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.communityheroai.issue.entity.ImageValidationStatus;
import com.communityheroai.issue.entity.IssueCategory;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageValidationService {
    private final RestClient.Builder restClientBuilder;

    @Value("${google.cloud-vision.api-key:}")
    private String googleVisionApiKey;

    @Value("${google.cloud-vision.base-url:https://vision.googleapis.com/v1/images:annotate}")
    private String googleVisionBaseUrl;

    @Value("${google.cloud-vision.auth-mode:auto}")
    private String googleVisionAuthMode;

    public ValidationResult validate(MultipartFile file, IssueCategory category) {
        if (file == null || file.isEmpty()) {
            return new ValidationResult(ImageValidationStatus.FAILED, 0,
                    "Select an image before validating.", "", LocalDateTime.now());
        }
        try {
            return validateBytes(file.getBytes(), category);
        } catch (Exception ex) {
            return failed("Google Cloud Vision validation failed. Manual review recommended: " + safeMessage(ex));
        }
    }

    public ValidationResult validate(Path imagePath, IssueCategory category) {
        try {
            return validateBytes(Files.readAllBytes(imagePath), category);
        } catch (Exception ex) {
            return failed("Google Cloud Vision validation failed. Manual review recommended: " + safeMessage(ex));
        }
    }

    private ValidationResult validateBytes(byte[] bytes, IssueCategory category) {
        String apiKey = cleanToken(googleVisionApiKey);
        boolean keyConfigured = apiKey != null && !apiKey.isBlank();
        if (!keyConfigured && !usesOAuth()) {
            return new ValidationResult(
                    ImageValidationStatus.UNAVAILABLE,
                    0,
                    "Google Cloud Vision is not configured. Enable Cloud Run service-account auth or add GOOGLE_CLOUD_VISION_API_KEY for local fallback.",
                    "",
                    LocalDateTime.now()
            );
        }
        try {
            JsonNode response = callVision(bytes, apiKey, keyConfigured);
            return analyzeVisionResponse(response, category);
        } catch (Exception ex) {
            return failed("Google Cloud Vision validation failed. Manual review recommended: " + safeMessage(ex));
        }
    }

    private JsonNode callVision(byte[] bytes, String apiKey, boolean keyConfigured) {
        if (usesOAuth()) {
            try {
                String accessToken = accessToken();
                return restClientBuilder.build().post()
                        .uri(googleVisionBaseUrl)
                        .headers(headers -> headers.setBearerAuth(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(visionRequest(bytes))
                        .retrieve()
                        .body(JsonNode.class);
            } catch (Exception ex) {
                if (!keyConfigured || forcesOAuth()) {
                    throw new IllegalStateException("Google Cloud Vision OAuth authentication failed: " + safeMessage(ex), ex);
                }
            }
        }

        return restClientBuilder.build().post()
                .uri(googleVisionBaseUrl + "?key={key}", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(visionRequest(bytes))
                .retrieve()
                .body(JsonNode.class);
    }

    private boolean usesOAuth() {
        String mode = normalize(googleVisionAuthMode);
        return mode.isBlank() || mode.equals("auto") || mode.equals("oauth") || mode.equals("service_account");
    }

    private boolean forcesOAuth() {
        String mode = normalize(googleVisionAuthMode);
        return mode.equals("oauth") || mode.equals("service_account");
    }

    private String accessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        credentials.refreshIfExpired();
        if (credentials.getAccessToken() == null || credentials.getAccessToken().getTokenValue() == null) {
            credentials.refresh();
        }
        return credentials.getAccessToken().getTokenValue();
    }

    private Map<String, Object> visionRequest(byte[] bytes) {
        return Map.of(
                "requests", List.of(Map.of(
                        "image", Map.of("content", Base64.getEncoder().encodeToString(bytes)),
                        "features", List.of(
                                Map.of("type", "LABEL_DETECTION", "maxResults", 12),
                                Map.of("type", "OBJECT_LOCALIZATION", "maxResults", 8)
                        )
                ))
        );
    }

    private ValidationResult analyzeVisionResponse(JsonNode response, IssueCategory selectedCategory) {
        JsonNode first = response == null ? null : response.path("responses").path(0);
        if (first == null || first.isMissingNode()) {
            return new ValidationResult(ImageValidationStatus.SUSPECT, 0,
                    "Google Cloud Vision returned no image labels. Manual review recommended.",
                    "", LocalDateTime.now());
        }
        if (first.has("error")) {
            return failed("Google Cloud Vision returned an error: " + first.path("error").path("message").asText("Unknown error"));
        }

        List<DetectedLabel> labels = new ArrayList<>();
        first.path("labelAnnotations").forEach(node -> labels.add(new DetectedLabel(
                node.path("description").asText(""),
                node.path("score").asDouble(0.0)
        )));
        first.path("localizedObjectAnnotations").forEach(node -> labels.add(new DetectedLabel(
                node.path("name").asText(""),
                node.path("score").asDouble(0.0)
        )));

        if (labels.isEmpty()) {
            return new ValidationResult(ImageValidationStatus.SUSPECT, 0,
                    "Google Cloud Vision found no useful labels. Manual review recommended.",
                    "", LocalDateTime.now());
        }

        Map<IssueCategory, Double> categoryScores = categoryScores(labels);
        double selectedScore = categoryScores.getOrDefault(selectedCategory, 0.0);
        Map.Entry<IssueCategory, Double> bestCategory = categoryScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(Map.entry(selectedCategory, 0.0));
        IssueCategory predictedCategory = bestCategory.getKey();
        double predictedScore = bestCategory.getValue();
        int confidence = (int) Math.round(selectedScore * 100);
        String labelText = labels.stream()
                .sorted(Comparator.comparingDouble(DetectedLabel::score).reversed())
                .limit(8)
                .map(label -> "%s %.0f%%".formatted(label.description(), label.score() * 100))
                .reduce((left, right) -> left + ", " + right)
                .orElse("");

        boolean selectedWins = predictedCategory == selectedCategory && selectedScore >= 0.40;
        boolean selectedCloseWinner = predictedCategory == selectedCategory && selectedScore >= 0.30
                && (predictedScore - selectedScore) <= 0.05;
        if (selectedWins || selectedCloseWinner) {
            return new ValidationResult(
                    ImageValidationStatus.VALID,
                    confidence,
                    "Google Cloud Vision found visual evidence consistent with the selected civic category.",
                    labelText,
                    LocalDateTime.now()
            );
        }

        return new ValidationResult(
                ImageValidationStatus.SUSPECT,
                confidence,
                mismatchSummary(selectedCategory, predictedCategory, predictedScore),
                labelText,
                LocalDateTime.now()
        );
    }

    private Map<IssueCategory, Double> categoryScores(List<DetectedLabel> labels) {
        Map<IssueCategory, Double> scores = new EnumMap<>(IssueCategory.class);
        for (DetectedLabel label : labels) {
            labelCategories(label.description()).forEach(category ->
                    scores.merge(category, label.score(), Math::max));
        }
        return scores;
    }

    private List<IssueCategory> labelCategories(String label) {
        String normalized = normalize(label);
        List<IssueCategory> categories = new ArrayList<>();
        if (containsAny(normalized, "pothole", "sinkhole")
                || (containsAny(normalized, "hole", "crack", "damage", "broken")
                && containsAny(normalized, "road", "street", "asphalt", "pavement", "lane", "highway"))) {
            categories.add(IssueCategory.POTHOLE);
        }
        if (containsAny(normalized, "water", "pipe", "plumbing", "leak", "fluid", "flood", "hose")) {
            categories.add(IssueCategory.WATER_LEAKAGE);
        }
        if (containsAny(normalized, "street light", "streetlight", "lamp", "lighting", "light fixture", "electricity", "pole")) {
            categories.add(IssueCategory.STREETLIGHT_DAMAGE);
        }
        if (containsAny(normalized, "trash", "garbage", "waste", "rubbish", "litter", "plastic", "dump", "bin")) {
            categories.add(IssueCategory.WASTE_MANAGEMENT);
        }
        if (containsAny(normalized, "drain", "drainage", "sewer", "gutter", "ditch", "canal", "manhole")) {
            categories.add(IssueCategory.DRAINAGE_ISSUE);
        }
        return categories;
    }

    private String mismatchSummary(IssueCategory selectedCategory, IssueCategory predictedCategory, double predictedScore) {
        if (predictedScore <= 0.0) {
            return "Google Cloud Vision labels did not strongly match the selected category. Manual review recommended.";
        }
        return "Google Cloud Vision suggests this image looks more like %s than %s. Manual review recommended."
                .formatted(displayCategory(predictedCategory), displayCategory(selectedCategory));
    }

    private String displayCategory(IssueCategory category) {
        return category == null ? "another issue category" : category.name().replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) return true;
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private ValidationResult failed(String summary) {
        return new ValidationResult(ImageValidationStatus.FAILED, 0, summary, "", LocalDateTime.now());
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("API_KEY_SERVICE_BLOCKED")) {
            return "Cloud Vision is blocked for this API key. Enable the Cloud Vision API for the Google Cloud project and make sure the API key restrictions allow vision.googleapis.com.";
        }
        if (message != null && (message.contains("API keys are not supported")
                || message.contains("CREDENTIALS_MISSING")
                || message.contains("UNAUTHENTICATED"))) {
            return "Cloud Vision rejected API-key authentication. On Cloud Run, grant the service account access to Cloud Vision and use OAuth/Application Default Credentials.";
        }
        if (message != null && message.contains("PERMISSION_DENIED")) {
            return "Google rejected this API key for Cloud Vision. Check that Cloud Vision API is enabled, billing is active if required, and the key restrictions allow Cloud Vision.";
        }
        if (message != null && message.contains("API key not valid")) {
            return "The Google Cloud Vision API key is invalid. Check GOOGLE_CLOUD_VISION_API_KEY in backend/.env.";
        }
        return message == null || message.isBlank()
                ? ex.getClass().getSimpleName()
                : message;
    }

    private String cleanToken(String token) {
        if (token == null) return "";
        String cleaned = token.trim();
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private record DetectedLabel(String description, double score) { }

    public record ValidationResult(
            ImageValidationStatus status,
            Integer confidence,
            String summary,
            String labels,
            LocalDateTime validatedAt
    ) { }
}
