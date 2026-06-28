package com.communityheroai.issue.service;

import com.communityheroai.issue.dto.IssueRequest;
import com.communityheroai.issue.dto.PossibleDuplicateIssueResponse;
import com.communityheroai.issue.entity.Issue;
import com.communityheroai.issue.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {
    private static final double MAX_DISTANCE_METERS = 300.0;
    private static final double MIN_TEXT_SIMILARITY = 0.15;
    private final IssueRepository issueRepository;

    public List<PossibleDuplicateIssueResponse> findDuplicates(IssueRequest request) {
        return findDuplicates(request.getCategory(), request.getTitle(), request.getDescription(),
                request.getLatitude(), request.getLongitude(), null);
    }

    public List<PossibleDuplicateIssueResponse> findDuplicates(Issue issue) {
        return findDuplicates(issue.getCategory(), issue.getTitle(), issue.getDescription(),
                issue.getLatitude(), issue.getLongitude(), issue.getId());
    }

    private List<PossibleDuplicateIssueResponse> findDuplicates(
            com.communityheroai.issue.entity.IssueCategory category, String title, String description,
            double latitude, double longitude, Long excludedId) {
        String sourceText = title + " " + description;
        return issueRepository.findByCategory(category).stream()
                .filter(candidate -> excludedId == null || !candidate.getId().equals(excludedId))
                .map(candidate -> new Candidate(candidate,
                        distanceMeters(latitude, longitude, candidate.getLatitude(), candidate.getLongitude())))
                .filter(candidate -> candidate.distance <= MAX_DISTANCE_METERS)
                .filter(candidate -> similarity(sourceText,
                        candidate.issue.getTitle() + " " + candidate.issue.getDescription()) >= MIN_TEXT_SIMILARITY)
                .sorted((left, right) -> Double.compare(left.distance, right.distance))
                .limit(5)
                .map(candidate -> PossibleDuplicateIssueResponse.builder()
                        .id(candidate.issue.getId())
                        .title(candidate.issue.getTitle())
                        .distanceMeters(Math.round(candidate.distance * 10.0) / 10.0)
                        .category(candidate.issue.getCategory())
                        .status(candidate.issue.getStatus())
                        .build())
                .toList();
    }

    private double similarity(String first, String second) {
        Set<String> left = tokens(first);
        Set<String> right = tokens(second);
        if (left.isEmpty() || right.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        return (double) intersection.size() / union.size();
    }

    private Set<String> tokens(String value) {
        Set<String> stopWords = Set.of("the", "and", "for", "with", "this", "that", "near", "from", "has");
        Set<String> result = new HashSet<>(Arrays.asList(value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ").split("\\s+")));
        result.removeIf(token -> token.length() < 3 || stopWords.contains(token));
        return result;
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private record Candidate(Issue issue, double distance) { }
}
