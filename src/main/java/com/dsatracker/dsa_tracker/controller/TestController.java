package com.dsatracker.dsa_tracker.controller;

import com.dsatracker.dsa_tracker.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> publicEndpoint() {
        return ResponseEntity.ok(Map.of("message", "This is public — no JWT needed."));
    }

    @GetMapping("/protected")
    public ResponseEntity<Map<String, String>> protectedEndpoint(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(Map.of(
                "message", "JWT valid! You are authenticated.",
                "email", user.getEmail(),
                "syncStatus", user.getSyncStatus().name()
        ));
    }
}