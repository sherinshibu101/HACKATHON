package com.communityheroai.ai.prompt;

import com.communityheroai.ai.dto.GeminiIssueAnalysisRequest;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.communityheroai.issue.entity.IssueCategory.*;

public final class GeminiPrompts {
    private GeminiPrompts() {
    }

    public static String buildIssueAnalysisPrompt(GeminiIssueAnalysisRequest issue) {
        String categories = Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
        return """
                You are the AI Civic Resolution Agent for a civic issue reporting platform.
                Analyze the report and produce practical guidance that helps officials move it toward resolution.

                Rules:
                - Return one valid JSON object only. No markdown, code fences, or commentary.
                - category must be exactly one of: %s.
                - severity must be exactly LOW, MEDIUM, HIGH, or CRITICAL.
                - impactScore must be an integer from 0 to 100.
                - Base urgency on immediate safety, public reach, service disruption, and deterioration risk.
                - recommendedDepartment must name the most suitable civic authority.
                - suggestedAction must be concrete, time-bound, and useful to an official.
                - complaintDraft must be a polite, ready-to-send citizen complaint using known location details.
                - escalationMessage must be concise and suitable for a ward-level escalation.
                - Do not invent facts. Acknowledge uncertainty when report details are limited.

                Citizen report:
                Title: %s
                Description: %s
                Citizen-selected category: %s
                Ward: %s
                Locality: %s
                State: %s
                City: %s
                Formatted address: %s
                Coordinates: %s, %s
                """.formatted(
                categories,
                safe(issue.title()), safe(issue.description()), issue.category(),
                safe(issue.ward()), safe(issue.locality()), safe(issue.state()), safe(issue.city()),
                safe(issue.formattedAddress()), issue.latitude(), issue.longitude()
        );
    }

    public static String buildVisualFactCheckPrompt(String title, String category, String address,
                                                    double latitude, double longitude) {
        return """
                You are the Community Hero AI Street-View Fact-Checker Agent.

                Compare two images for a civic infrastructure report:
                1. The citizen uploaded image.
                2. A Google Street View baseline image fetched from the reported coordinates.

                Issue title: %s
                Category: %s
                Reported address: %s
                Coordinates: %.6f, %.6f

                Your job:
                - Determine whether the citizen image plausibly shows the same location or infrastructure context as the baseline.
                - Identify visible new damage, obstruction, deterioration, or civic risk if present.
                - Detect possible mismatch, old/fake/irrelevant image risk, or uncertainty.
                - Be honest: the baseline image may not be historical and may not show the exact angle. Do not claim a year unless visible in metadata.
                - Return one valid JSON object only. No markdown, code fences, or commentary.

                JSON fields:
                verificationResult: LIKELY_VALID, NEEDS_REVIEW, POSSIBLE_MISMATCH, or UNAVAILABLE
                confidenceScore: integer 0-100
                reasoningReport: concise explanation of visual comparison and civic relevance
                riskFlags: concise list of concerns or "None observed"
                """.formatted(safe(title), safe(category), safe(address), latitude, longitude);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }
}
