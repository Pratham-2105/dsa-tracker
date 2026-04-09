package com.dsatracker.dsa_tracker.controller;

import com.dsatracker.dsa_tracker.exception.ResourceNotFoundException;
import com.dsatracker.dsa_tracker.model.User;
import com.dsatracker.dsa_tracker.repository.UserRepository;
import com.dsatracker.dsa_tracker.service.CodeforcesService;
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
    private final UserRepository userRepository;

    @PostMapping("/codeforces")
    public ResponseEntity<Map<String, Object>> syncCodeforces(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int imported = codeforcesService.syncSubmissions(user);

        return ResponseEntity.ok(Map.of(
                "message", "Codeforces sync completed",
                "importedCount", imported
        ));
    }
}
