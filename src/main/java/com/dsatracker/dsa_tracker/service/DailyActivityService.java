package com.dsatracker.dsa_tracker.service;

import com.dsatracker.dsa_tracker.enums.Difficulty;
import com.dsatracker.dsa_tracker.model.DailyActivity;
import com.dsatracker.dsa_tracker.model.Problem;
import com.dsatracker.dsa_tracker.model.User;
import com.dsatracker.dsa_tracker.repository.DailyActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyActivityService {
    private final DailyActivityRepository dailyActivityRepository;

    /**
     * Called whenever a problem is added (either via API sync or manual entry).
     * Increments the appropriate counts for that day.
     *
     * IMPORTANT: This must be called within the same @Transactional context as
     * the problem save — so if problem save fails, this also rolls back.
     */

    public void recordProblemAdded(Problem problem) {
        User user = problem.getUser();
        LocalDate date = problem.getSolvedAt().toLocalDate();

        DailyActivity activity = dailyActivityRepository
                .findByUserIdAndDate(user.getId(), date)
                .orElseGet(() -> DailyActivity.builder()
                        .user(user)
                        .date(date)
                        .build());

        activity.setTotalCount(activity.getTotalCount() + 1);

        Difficulty difficulty = problem.getDifficulty();
        if (difficulty == Difficulty.EASY) {
            activity.setEasyCount(activity.getEasyCount() + 1);
        } else if (difficulty == Difficulty.MEDIUM) {
            activity.setMediumCount(activity.getMediumCount() + 1);
        } else if (difficulty == Difficulty.HARD) {
            activity.setHardCount(activity.getHardCount() + 1);
        }

        dailyActivityRepository.save(activity);
    }

    /**
     * Called whenever a problem is deleted.
     * Decrements counts — floors at 0 to avoid negative counts.
     */

    public void recordProblemRemoved(Problem problem) {
        User user = problem.getUser();
        LocalDate date = problem.getSolvedAt().toLocalDate();

        dailyActivityRepository.findByUserIdAndDate(user.getId(), date)
                .ifPresent(activity -> {
                    activity.setTotalCount(Math.max(0, activity.getTotalCount() - 1));

                    Difficulty difficulty = problem.getDifficulty();
                    if (difficulty == Difficulty.EASY) {
                        activity.setEasyCount(Math.max(0, activity.getEasyCount() - 1));
                    } else if (difficulty == Difficulty.MEDIUM) {
                        activity.setMediumCount(Math.max(0, activity.getMediumCount() - 1));
                    } else if (difficulty == Difficulty.HARD) {
                        activity.setHardCount(Math.max(0, activity.getHardCount() - 1));
                    }

                    dailyActivityRepository.save(activity);
                });
    }
}

