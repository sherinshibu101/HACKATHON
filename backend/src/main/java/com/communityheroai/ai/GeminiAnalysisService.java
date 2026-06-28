package com.communityheroai.ai;

import com.communityheroai.ai.dto.GeminiIssueAnalysisRequest;
import com.communityheroai.ai.dto.GeminiIssueAnalysisResponse;
import com.communityheroai.ai.dto.GeminiVisualFactCheckResponse;
import com.communityheroai.ai.prompt.GeminiPrompts;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GeminiAnalysisService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final String apiKey;
    private final String model;

    public GeminiAnalysisService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            Validator validator,
            @Value("${gemini.base-url}") String baseUrl,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model}") String model
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.apiKey = apiKey;
        this.model = model;
    }

    public GeminiIssueAnalysisResponse analyze(GeminiIssueAnalysisRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiAnalysisException("GEMINI_API_KEY is not configured");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of(
                        "text", GeminiPrompts.buildIssueAnalysisPrompt(request)
                )))),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json",
                        "responseJsonSchema", responseSchema()
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return parseAndValidate(response);
        } catch (GeminiAnalysisException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GeminiAnalysisException("Gemini request failed", ex);
        }
    }

    public GeminiVisualFactCheckResponse visualFactCheck(
            String prompt,
            byte[] userImage,
            String userImageMimeType,
            byte[] baselineImage,
            String baselineImageMimeType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GeminiAnalysisException("GEMINI_API_KEY is not configured");
        }
        if (userImage == null || userImage.length == 0 || baselineImage == null || baselineImage.length == 0) {
            throw new GeminiAnalysisException("Both user image and baseline image are required");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(
                        Map.of("text", prompt),
                        inlineImagePart(userImageMimeType, userImage),
                        inlineImagePart(baselineImageMimeType, baselineImage)
                ))),
                "generationConfig", Map.of(
                        "temperature", 0.15,
                        "responseMimeType", "application/json",
                        "responseJsonSchema", visualFactCheckSchema()
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return parseAndValidateVisualFactCheck(response);
        } catch (GeminiAnalysisException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GeminiAnalysisException("Gemini visual fact-check request failed", ex);
        }
    }

    private GeminiIssueAnalysisResponse parseAndValidate(JsonNode response) throws JsonProcessingException {
        JsonNode text = response == null ? null : response.at("/candidates/0/content/parts/0/text");
        if (text == null || !text.isTextual() || text.asText().isBlank()) {
            throw new GeminiAnalysisException("Gemini returned no analysis");
        }

        GeminiIssueAnalysisResponse analysis = objectMapper.readValue(
                stripCodeFence(text.asText()), GeminiIssueAnalysisResponse.class);
        Set<ConstraintViolation<GeminiIssueAnalysisResponse>> violations = validator.validate(analysis);
        if (!violations.isEmpty()) {
            throw new GeminiAnalysisException("Gemini returned incomplete or invalid analysis");
        }
        return analysis;
    }

    private GeminiVisualFactCheckResponse parseAndValidateVisualFactCheck(JsonNode response) throws JsonProcessingException {
        JsonNode text = response == null ? null : response.at("/candidates/0/content/parts/0/text");
        if (text == null || !text.isTextual() || text.asText().isBlank()) {
            throw new GeminiAnalysisException("Gemini returned no visual fact-check");
        }
        GeminiVisualFactCheckResponse factCheck = objectMapper.readValue(
                stripCodeFence(text.asText()), GeminiVisualFactCheckResponse.class);
        Set<ConstraintViolation<GeminiVisualFactCheckResponse>> violations = validator.validate(factCheck);
        if (!violations.isEmpty()) {
            throw new GeminiAnalysisException("Gemini returned incomplete or invalid visual fact-check");
        }
        return factCheck;
    }

    private Map<String, Object> inlineImagePart(String mimeType, byte[] image) {
        return Map.of("inlineData", Map.of(
                "mimeType", mimeType == null || mimeType.isBlank() ? "image/jpeg" : mimeType,
                "data", java.util.Base64.getEncoder().encodeToString(image)
        ));
    }

    private String stripCodeFence(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> stringField = Map.of("type", "string");
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "category", Map.of("type", "string", "enum", List.of("POTHOLE", "WATER_LEAKAGE", "STREETLIGHT_DAMAGE", "WASTE_MANAGEMENT", "DRAINAGE_ISSUE")),
                        "severity", Map.of("type", "string", "enum", List.of("LOW", "MEDIUM", "HIGH", "CRITICAL")),
                        "recommendedDepartment", stringField,
                        "impactScore", Map.of("type", "integer", "minimum", 0, "maximum", 100),
                        "riskExplanation", stringField,
                        "suggestedAction", stringField,
                        "complaintDraft", stringField,
                        "escalationMessage", stringField,
                        "resolutionUrgency", stringField
                ),
                "required", List.of("category", "severity", "recommendedDepartment", "impactScore",
                        "riskExplanation", "suggestedAction", "complaintDraft", "escalationMessage",
                        "resolutionUrgency")
        );
    }

    private Map<String, Object> visualFactCheckSchema() {
        Map<String, Object> stringField = Map.of("type", "string");
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "verificationResult", Map.of("type", "string", "enum", List.of("LIKELY_VALID", "NEEDS_REVIEW", "POSSIBLE_MISMATCH", "UNAVAILABLE")),
                        "confidenceScore", Map.of("type", "integer", "minimum", 0, "maximum", 100),
                        "reasoningReport", stringField,
                        "riskFlags", stringField
                ),
                "required", List.of("verificationResult", "confidenceScore", "reasoningReport", "riskFlags")
        );
    }

    public static class GeminiAnalysisException extends RuntimeException {
        public GeminiAnalysisException(String message) { super(message); }
        public GeminiAnalysisException(String message, Throwable cause) { super(message, cause); }
    }
}
