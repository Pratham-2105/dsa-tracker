package com.dsatracker.dsa_tracker.enums;

public enum SyncStatus {
    PENDING,    // Just registered, sync not started
    SYNCING,    // Background import running
    COMPLETED,  // All platforms imported
    FAILED      // At least one platform failed
}
