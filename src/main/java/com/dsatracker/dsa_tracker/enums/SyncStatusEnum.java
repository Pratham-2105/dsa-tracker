package com.dsatracker.dsa_tracker.enums;

public enum SyncStatusEnum {
    NOT_CONFIGURED,  // user hasn't linked this platform
    PENDING,    // Just registered, sync not started
    SYNCING,    // Background import running
    COMPLETED,  // All platforms imported
    FAILED      // At least one platform failed
}
