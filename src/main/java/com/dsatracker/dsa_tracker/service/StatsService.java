// src/main/java/com/dsatracker/dsa_tracker/service/StatsService.java

package com.dsatracker.dsa_tracker.service;

import com.dsatracker.dsa_tracker.dto.*;
import com.dsatracker.dsa_tracker.model.DailyActivity;
import com.dsatracker.dsa_tracker.repository.DailyActivityRepository;
import com.dsatracker.dsa_tracker.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final ProblemRepository problemRepository;
    private final DailyActivityRepository dailyActivityRepository;

    // ─────────────────────────────────────────────
    // OVERVIEW
    // ─────────────────────────────────────────────

    public StatsOverviewResponse getOverview(Long userId) {
        // Platform counts
        Map<String, Long> byPlatform = new HashMap<>();
        for (Object[] row : problemRepository.countByPlatformForUser(userId)) {
            byPlatform.put(row[0].toString(), (Long) row[1]);
        }

        // Difficulty counts
        Map<String, Long> byDifficulty = new HashMap<>();
        for (Object[] row : problemRepository.countByDifficultyForUser(userId)) {
            byDifficulty.put(row[0].toString(), (Long) row[1]);
        }

        long easy   = byDifficulty.getOrDefault("EASY", 0L);
        long medium = byDifficulty.getOrDefault("MEDIUM", 0L);
        long hard   = byDifficulty.getOrDefault("HARD", 0L);

        // Streaks
        List<LocalDate> activeDates = dailyActivityRepository.findActiveDatesByUserId(userId);
        int currentStreak  = computeCurrentStreak(activeDates);
        int longestStreak  = computeLongestStreak(activeDates);

        // This week (Monday 00:00 to now)
        LocalDateTime weekStart = LocalDate.now()
                .with(java.time.DayOfWeek.MONDAY)
                .atStartOfDay();
        long thisWeek = problemRepository.countThisWeek(userId, weekStart);

        return StatsOverviewResponse.builder()
                .totalSolved(easy + medium + hard)
                .easySolved(easy)
                .mediumSolved(medium)
                .hardSolved(hard)
                .leetcodeSolved(byPlatform.getOrDefault("LEETCODE", 0L))
                .codeforcesSolved(byPlatform.getOrDefault("CODEFORCES", 0L))
                .codechefSolved(byPlatform.getOrDefault("CODECHEF", 0L))
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .thisWeekCount(thisWeek)
                .build();
    }

    // ─────────────────────────────────────────────
    // HEATMAP
    // ─────────────────────────────────────────────

    public List<HeatmapEntry> getHeatmap(Long userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end   = LocalDate.of(year, 12, 31);

        List<DailyActivity> activities =
                dailyActivityRepository.findByUserIdAndDateRange(userId, start, end);

        return activities.stream()
                .map(a -> new HeatmapEntry(
                        a.getDate(),
                        a.getTotalCount(),
                        a.getEasyCount(),
                        a.getMediumCount(),
                        a.getHardCount()
                ))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // TAG DISTRIBUTION
    // ─────────────────────────────────────────────

    public List<TagStatsEntry> getTagStats(Long userId) {
        return problemRepository.countByTagForUser(userId)
                .stream()
                .map(row -> new TagStatsEntry((String) row[0], (Long) row[1]))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // WEEKLY VELOCITY
    // ─────────────────────────────────────────────

    public List<WeeklyStatsEntry> getWeeklyVelocity(Long userId, int weeks) {
        LocalDateTime since = LocalDateTime.now().minusWeeks(weeks);

        return problemRepository.weeklyVelocity(userId, since)
                .stream()
                .map(row -> new WeeklyStatsEntry(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue(),
                        ((Number) row[4]).longValue()
                ))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // STREAK ALGORITHMS
    // ─────────────────────────────────────────────

    /**
     * Streak = consecutive days with at least one problem solved, ending today or yesterday.
     * (yesterday because you might not have solved anything yet today)
     */
    private int computeCurrentStreak(List<LocalDate> activeDates) {
        if (activeDates.isEmpty()) return 0;

        LocalDate today     = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // If last active day is neither today nor yesterday, streak is broken
        LocalDate lastActive = activeDates.get(activeDates.size() - 1);
        if (!lastActive.equals(today) && !lastActive.equals(yesterday)) return 0;

        int streak = 0;
        LocalDate expected = lastActive;

        // Walk backwards through active dates
        for (int i = activeDates.size() - 1; i >= 0; i--) {
            if (activeDates.get(i).equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break;  // Gap found — streak ends
            }
        }
        return streak;
    }

    /**
     * Longest streak ever — slide through sorted active dates tracking max consecutive run.
     */
    private int computeLongestStreak(List<LocalDate> activeDates) {
        if (activeDates.isEmpty()) return 0;

        int longest = 1;
        int current = 1;

        for (int i = 1; i < activeDates.size(); i++) {
            LocalDate prev = activeDates.get(i - 1);
            LocalDate curr = activeDates.get(i);

            if (curr.equals(prev.plusDays(1))) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }
}