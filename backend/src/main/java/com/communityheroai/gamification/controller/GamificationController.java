package com.communityheroai.gamification.controller;

import com.communityheroai.gamification.dto.ContributorLeaderboardResponse;
import com.communityheroai.gamification.dto.GamificationSummaryResponse;
import com.communityheroai.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
@CrossOrigin
public class GamificationController {
    private final GamificationService gamificationService;

    @GetMapping("/leaderboard")
    public List<ContributorLeaderboardResponse> leaderboard() {
        return gamificationService.leaderboard();
    }

    @GetMapping("/summary")
    public GamificationSummaryResponse summary() {
        return gamificationService.summary();
    }
}
