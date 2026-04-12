package com.dsatracker.dsa_tracker.controller;

import com.dsatracker.dsa_tracker.exception.ResourceNotFoundException;
import com.dsatracker.dsa_tracker.model.User;
import com.dsatracker.dsa_tracker.repository.UserRepository;
import com.dsatracker.dsa_tracker.service.CodeforcesService;
import com.dsatracker.dsa_tracker.service.LeetCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final CodeforcesService codeforcesService;
    private final LeetCodeService leetCodeService;
    private final UserRepository userRepository;

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

    private User resolveUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}