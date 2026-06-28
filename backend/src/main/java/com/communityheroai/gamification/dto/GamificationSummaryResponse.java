package com.communityheroai.gamification.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GamificationSummaryResponse {
    long contributors;
    long pointsAwarded;
    long reportsSubmitted;
    long verificationsSubmitted;
}
