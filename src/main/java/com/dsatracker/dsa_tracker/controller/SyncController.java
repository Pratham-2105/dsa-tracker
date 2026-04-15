package com.dsatracker.dsa_tracker.controller;

import com.dsatracker.dsa_tracker.dto.SyncStatusResponse;
import com.dsatracker.dsa_tracker.enums.Platform;
import com.dsatracker.dsa_tracker.exception.ResourceNotFoundException;
import com.dsatracker.dsa_tracker.model.User;
import com.dsatracker.dsa_tracker.repository.UserRepository;
import com.dsatracker.dsa_tracker.service.CodeforcesService;
import com.dsatracker.dsa_tracker.service.LeetCodeService;
import com.dsatracker.dsa_tracker.service.SyncOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final CodeforcesService codeforcesService;
    private final LeetCodeService leetCodeService;
    private final SyncOrchestrator syncOrchestrator;
    private final UserRepository userRepository;

    // ==================== SYNCHRONOUS SYNC (existing — direct feedback) ====================

    @PostMapping("/codeforces")
    public ResponseEntity<Map<String, Object>> syncCodeforces(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        int imported = codeforcesService.syncSubmissions(user);

        return ResponseEntity.ok(Map.of(
                "message", "Codeforces sync completed",
                "importedCount", imported
        ));
    }

    @PostMapping("/leetcode")
    public ResponseEntity<Map<String, Object>> syncLeetCode(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        int imported = leetCodeService.syncSubmissions(user);

        return ResponseEntity.ok(Map.of(
                "message", "LeetCode sync completed",
                "importedCount", imported
        ));
    }

    /*
     * ===== WHY A SEPARATE ENDPOINT FOR THE SESSION COOKIE? =====
     *
     * The session cookie is NOT part of registration. A user might:
     *   1. Register → add LC username → sync later
     *   2. Register → sync → cookie expires → need to update cookie only
     *   3. Register without LC → add LC details weeks later
     *
     * A dedicated PUT endpoint keeps this decoupled from registration.
     * The frontend Settings page will call this when the user pastes
     * their cookie value.
     *
     * PUT (not POST) because we're updating an existing user's data,
     * and the operation is idempotent — calling it twice with the same
     * cookie produces the same result.
     */
    @PutMapping("/leetcode/session")
    public ResponseEntity<Map<String, String>> setLeetCodeSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {

        User user = resolveUser(userDetails);

        String cookie = body.get("sessionCookie");
        if (cookie == null || cookie.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "sessionCookie field is required"));
        }

        user.setLeetcodeSessionCookie(cookie);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "LeetCode session cookie saved. You can now sync your LeetCode data."
        ));
    }

    // ==================== ASYNC SYNC (new — fire-and-forget, poll /status) ====================

    /*
     * ===== WHY BOTH SYNC AND ASYNC ENDPOINTS? =====
     *
     * Sync endpoints (above): User clicks "Sync Now" in Settings, wants to see
     * "Imported 14 problems" immediately. Simple, blocking, direct feedback.
     *
     * Async endpoint (below): Used by registration flow and frontend dashboard.
     * Returns 202 instantly, user polls GET /status for progress.
     * Essential when syncing multiple platforms — you don't want the user
     * staring at a blank screen for 30+ seconds.
     *
     * Both coexist because they serve different UX needs.
     */
    @PostMapping("/async/{platform}")
    public ResponseEntity<Map<String, String>> triggerAsyncSync(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String platform) {

        User user = resolveUser(userDetails);

        Platform platformEnum;
        try {
            platformEnum = Platform.valueOf(platform.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid platform: " + platform
                            + ". Valid values: CODEFORCES, LEETCODE, CODECHEF"));
        }

        // Validate platform-specific prerequisites
        switch (platformEnum) {
            case CODEFORCES -> {
                if (user.getCodeforcesHandle() == null || user.getCodeforcesHandle().isBlank()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "No Codeforces handle configured"));
                }
            }
            case LEETCODE -> {
                if (user.getLeetcodeSessionCookie() == null || user.getLeetcodeSessionCookie().isBlank()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "No LeetCode session cookie found. "
                                    + "Save it via PUT /api/sync/leetcode/session first."));
                }
            }
            case CODECHEF -> {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "CodeChef integration not available yet"));
            }
        }

        syncOrchestrator.syncPlatformAsync(user, platformEnum);

        // 202 Accepted — "I've accepted your request, processing in background"
        return ResponseEntity.accepted()
                .body(Map.of("message", "Sync started for " + platformEnum));
    }

    // ==================== STATUS POLLING ====================

    @GetMapping("/status")
    public ResponseEntity<List<SyncStatusResponse>> getSyncStatus(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        List<SyncStatusResponse> statuses = syncOrchestrator.getSyncStatuses(user.getId());
        return ResponseEntity.ok(statuses);
    }

    // ==================== HELPER ====================

    private User resolveUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}