package com.communityheroai.gamification.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ContributorLeaderboardResponse {
    String displayName;
    Integer points;
    Integer rank;
    Integer reportsSubmitted;
    Integer verificationsSubmitted;
    Integer communityVerifiedReports;
    List<String> badges;
}
