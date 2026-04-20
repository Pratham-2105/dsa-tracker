package com.dsatracker.dsa_tracker.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatsOverviewResponse {
    // Total counts
    private long totalSolved;
    private long easySolved;
    private long mediumSolved;
    private long hardSolved;

    // Per-platform breakdown
    private long leetcodeSolved;
    private long codeforcesSolved;
    private long codechefSolved;

    // Streak info (delegated from StatsService)
    private int currentStreak;
    private int longestStreak;

    // This week
    private long thisWeekCount;
}