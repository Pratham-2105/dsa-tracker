package com.dsatracker.dsa_tracker.repository;

import com.dsatracker.dsa_tracker.enums.Platform;
import com.dsatracker.dsa_tracker.enums.SyncStatusEnum;
import com.dsatracker.dsa_tracker.model.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SyncStatusRepository extends JpaRepository<SyncStatus, Long> {

    // Used by the polling endpoint: "give me all sync statuses for this user"
    List<SyncStatus> findByUserId(Long userId);

    // Used by the orchestrator: "get/update CF status for this user"
    Optional<SyncStatus> findByUserIdAndPlatform(Long userId, Platform platform);

    // Used by the scheduler: "find all users whose CF sync completed — time to re-sync"
    List<SyncStatus> findByStatusAndPlatform(SyncStatusEnum status, Platform platform);
}

