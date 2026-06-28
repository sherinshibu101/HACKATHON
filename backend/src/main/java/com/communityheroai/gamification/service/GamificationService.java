package com.communityheroai.gamification.service;

import com.communityheroai.gamification.dto.ContributorLeaderboardResponse;
import com.communityheroai.gamification.dto.GamificationSummaryResponse;
import com.communityheroai.issue.entity.Issue;
import com.communityheroai.issue.entity.IssueVerification;
import com.communityheroai.issue.repository.IssueRepository;
import com.communityheroai.issue.repository.IssueVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GamificationService {
    private static final int REPORT_POINTS = 20;
    private static final int VERIFICATION_POINTS = 10;
    private static final int COMMUNITY_VERIFIED_BONUS = 25;

    private final IssueRepository issueRepository;
    private final IssueVerificationRepository verificationRepository;

    @Transactional(readOnly = true)
    public List<ContributorLeaderboardResponse> leaderboard() {
        Map<String, ContributorScore> scores = scores();
        List<ContributorScore> ranked = scores.values().stream()
                .sorted(Comparator.comparingInt(ContributorScore::points).reversed()
                        .thenComparing(ContributorScore::displayName))
                .toList();
        List<ContributorLeaderboardResponse> responses = new ArrayList<>();
        for (int index = 0; index < ranked.size(); index++) {
            responses.add(toResponse(ranked.get(index), index + 1));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public GamificationSummaryResponse summary() {
        Map<String, ContributorScore> scores = scores();
        return GamificationSummaryResponse.builder()
                .contributors(scores.size())
                .pointsAwarded(scores.values().stream().mapToLong(ContributorScore::points).sum())
                .reportsSubmitted(scores.values().stream().mapToLong(ContributorScore::reportsSubmitted).sum())
                .verificationsSubmitted(scores.values().stream().mapToLong(ContributorScore::verificationsSubmitted).sum())
                .build();
    }

    private Map<String, ContributorScore> scores() {
        Map<String, ContributorScore> scores = new HashMap<>();
        List<Issue> issues = issueRepository.findAll();
        Map<Long, Long> verificationCounts = new HashMap<>();
        for (IssueVerification verification : verificationRepository.findAll()) {
            String key = contributorKey(verification.getVerifierName(), verification.getVerifierEmail());
            ContributorScore score = scores.computeIfAbsent(key,
                    ignored -> new ContributorScore(displayName(verification.getVerifierName()), verification.getVerifierEmail()));
            score.verificationsSubmitted++;
            verificationCounts.merge(verification.getIssue().getId(), 1L, Long::sum);
        }
        for (Issue issue : issues) {
            String key = contributorKey(issue.getReporterName(), issue.getReporterEmail());
            ContributorScore score = scores.computeIfAbsent(key,
                    ignored -> new ContributorScore(displayName(issue.getReporterName()), issue.getReporterEmail()));
            score.reportsSubmitted++;
            if (verificationCounts.getOrDefault(issue.getId(), 0L) >= 3) {
                score.communityVerifiedReports++;
            }
        }
        return scores;
    }

    private ContributorLeaderboardResponse toResponse(ContributorScore score, int rank) {
        return ContributorLeaderboardResponse.builder()
                .displayName(score.displayName())
                .points(score.points())
                .rank(rank)
                .reportsSubmitted(score.reportsSubmitted())
                .verificationsSubmitted(score.verificationsSubmitted())
                .communityVerifiedReports(score.communityVerifiedReports())
                .badges(badges(score))
                .build();
    }

    private List<String> badges(ContributorScore score) {
        List<String> badges = new ArrayList<>();
        if (score.reportsSubmitted() >= 1) badges.add("First Reporter");
        if (score.reportsSubmitted() >= 5) badges.add("Neighborhood Watch");
        if (score.verificationsSubmitted() >= 3) badges.add("Community Validator");
        if (score.communityVerifiedReports() >= 1) badges.add("Impact Maker");
        if (score.points() >= 100) badges.add("Civic Champion");
        return badges;
    }

    private String contributorKey(String name, String email) {
        if (email != null && !email.isBlank()) return "email:" + email.trim().toLowerCase(Locale.ROOT);
        return "name:" + displayName(name).toLowerCase(Locale.ROOT);
    }

    private String displayName(String name) {
        return name == null || name.isBlank() ? "Community Member" : name.trim();
    }

    private static class ContributorScore {
        private final String displayName;
        private final String email;
        private int reportsSubmitted;
        private int verificationsSubmitted;
        private int communityVerifiedReports;

        private ContributorScore(String displayName, String email) {
            this.displayName = displayName;
            this.email = email;
        }

        private String displayName() { return displayName; }
        private int reportsSubmitted() { return reportsSubmitted; }
        private int verificationsSubmitted() { return verificationsSubmitted; }
        private int communityVerifiedReports() { return communityVerifiedReports; }
        private int points() {
            return reportsSubmitted * REPORT_POINTS
                    + verificationsSubmitted * VERIFICATION_POINTS
                    + communityVerifiedReports * COMMUNITY_VERIFIED_BONUS;
        }
    }
}
