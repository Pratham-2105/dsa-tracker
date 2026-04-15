package com.dsatracker.dsa_tracker.service;

import com.dsatracker.dsa_tracker.dto.SyncStatusResponse;
import com.dsatracker.dsa_tracker.enums.Platform;
import com.dsatracker.dsa_tracker.enums.SyncStatusEnum;
import com.dsatracker.dsa_tracker.model.SyncStatus;
import com.dsatracker.dsa_tracker.model.User;
import com.dsatracker.dsa_tracker.repository.SyncStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncOrchestrator {

    private final CodeforcesService codeforcesService;
    private final LeetCodeService leetCodeService;
    private final SyncStatusRepository syncStatusRepository;

    // Self-injection: needed so that @Async calls go through Spring's proxy.
    // Without this, calling this.syncPlatformAsync() from within the same class
    // bypasses the proxy and runs SYNCHRONOUSLY on the calling thread.
    @Lazy
    @Autowired
    private SyncOrchestrator self;

    /**
     * Called after registration. Creates status rows for each platform,
     * then kicks off async sync for platforms that are ready.
     */
    @Transactional
    public void triggerInitialSync(User user) {
        initializeSyncStatuses(user);

        // Codeforces: can auto-sync if handle is provided
        if (hasValue(user.getCodeforcesHandle())) {
            self.syncPlatformAsync(user, Platform.CODEFORCES);
        }

        // LeetCode: requires session cookie — user must save it via Settings first.
        // If they somehow already have a cookie (e.g., re-registration), sync it.
        if (hasValue(user.getLeetcodeUsername()) && hasValue(user.getLeetcodeSessionCookie())) {
            self.syncPlatformAsync(user, Platform.LEETCODE);
        }
    }

    /**
     * The async workhorse. Runs on the "syncExecutor" thread pool.
     * Returns immediately to the caller — actual work happens in background.
     */
    @Async("syncExecutor")
    public void syncPlatformAsync(User user, Platform platform) {
        // Guard: don't start a new sync if one is already running
        SyncStatus current = syncStatusRepository
                .findByUserIdAndPlatform(user.getId(), platform)
                .orElse(null);

        if (current != null && current.getStatus() == SyncStatusEnum.SYNCING) {
            log.info("Sync already in progress for user {} on {} — skipping",
                    user.getId(), platform);
            return;
        }

        log.info("Starting async sync for user {} on {}", user.getId(), platform);
        updateStatus(user.getId(), platform, SyncStatusEnum.SYNCING, null);

        try {
            int importedCount = switch (platform) {
                case CODEFORCES -> codeforcesService.syncSubmissions(user);
                case LEETCODE -> leetCodeService.syncSubmissions(user);
                case CODECHEF -> throw new UnsupportedOperationException(
                        "CodeChef integration not implemented yet");
            };

            log.info("Sync completed for user {} on {} — imported {} problems",
                    user.getId(), platform, importedCount);
            updateStatus(user.getId(), platform, SyncStatusEnum.COMPLETED, null);

        } catch (Exception e) {
            log.error("Sync failed for user {} on {}: {}",
                    user.getId(), platform, e.getMessage(), e);
            updateStatus(user.getId(), platform, SyncStatusEnum.FAILED, e.getMessage());
        }
    }

    /**
     * Returns all sync statuses for a user — used by the polling endpoint.
     */
    public List<SyncStatusResponse> getSyncStatuses(Long userId) {
        return syncStatusRepository.findByUserId(userId).stream()
                .map(s -> SyncStatusResponse.builder()
                        .platform(s.getPlatform())
                        .status(s.getStatus())
                        .lastSyncedAt(s.getLastSyncedAt())
                        .errorMessage(s.getErrorMessage())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== PRIVATE HELPERS ====================

    private void initializeSyncStatuses(User user) {
        createStatusIfAbsent(user, Platform.CODEFORCES, hasValue(user.getCodeforcesHandle()));
        createStatusIfAbsent(user, Platform.LEETCODE, hasValue(user.getLeetcodeUsername()));
        createStatusIfAbsent(user, Platform.CODECHEF, hasValue(user.getCodechefUsername()));
    }

    private void createStatusIfAbsent(User user, Platform platform, boolean isConfigured) {
        if (syncStatusRepository.findByUserIdAndPlatform(user.getId(), platform).isEmpty()) {
            SyncStatus status = SyncStatus.builder()
                    .user(user)
                    .platform(platform)
                    .status(isConfigured ? SyncStatusEnum.PENDING : SyncStatusEnum.NOT_CONFIGURED)
                    .build();
            syncStatusRepository.save(status);
        }
    }

    @Transactional
    public void updateStatus(Long userId, Platform platform,
                             SyncStatusEnum status, String errorMessage) {
        SyncStatus syncStatus = syncStatusRepository
                .findByUserIdAndPlatform(userId, platform)
                .orElseThrow(() -> new IllegalStateException(
                        "SyncStatus not found for user " + userId + " platform " + platform));

        syncStatus.setStatus(status);
        syncStatus.setErrorMessage(errorMessage);

        if (status == SyncStatusEnum.COMPLETED || status == SyncStatusEnum.FAILED) {
            syncStatus.setLastSyncedAt(LocalDateTime.now());
        }

        syncStatusRepository.save(syncStatus);
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}