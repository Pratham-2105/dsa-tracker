package com.dsatracker.dsa_tracker.dto;

import com.dsatracker.dsa_tracker.enums.Platform;
import com.dsatracker.dsa_tracker.enums.SyncStatusEnum;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SyncStatusResponse {
    private Platform platform;
    private SyncStatusEnum status;
    private LocalDateTime lastSyncedAt;
    private String errorMessage;
}
