package com.dsatracker.dsa_tracker.scheduler;

import com.dsatracker.dsa_tracker.enums.Platform;
import com.dsatracker.dsa_tracker.enums.SyncStatusEnum;
import com.dsatracker.dsa_tracker.model.SyncStatus;
import com.dsatracker.dsa_tracker.model.User;
import com.dsatracker.dsa_tracker.repository.SyncStatusRepository;
import com.dsatracker.dsa_tracker.service.SyncOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PeriodicSyncJob {

    private final SyncStatusRepository syncStatusRepository;
    private final SyncOrchestrator syncOrchestrator;

    /**
     * Runs every 6 hours. Finds all users with COMPLETED sync status
     * and re-triggers their sync to pick up newly solved problems.
     *
     * Cron: second minute hour day-of-month month day-of-week
     * "0 0 *//*6 * * *" = at 00:00, 06:00, 12:00, 18:00 every day*/
    @Scheduled(cron = "0 0 */6 * * *")
    public void periodicResync() {
        log.info("=== Periodic re-sync job started ===");

        resyncPlatform(Platform.CODEFORCES);
        resyncPlatform(Platform.LEETCODE);

        log.info("=== Periodic re-sync job — all tasks dispatched ===");
    }

    private void resyncPlatform(Platform platform) {
        List<SyncStatus> completedSyncs = syncStatusRepository
                .findByStatusAndPlatform(SyncStatusEnum.COMPLETED, platform);

        log.info("Found {} users to re-sync for {}", completedSyncs.size(), platform);

        for (SyncStatus syncStatus : completedSyncs) {
            User user = syncStatus.getUser();

            // LeetCode needs a session cookie — skip users who don't have one
            if (platform == Platform.LEETCODE
                    && (user.getLeetcodeSessionCookie() == null
                    || user.getLeetcodeSessionCookie().isBlank())) {
                log.debug("Skipping LC re-sync for user {} — no session cookie", user.getId());
                continue;
            }

            syncOrchestrator.syncPlatformAsync(user, platform);
        }
    }
}