// src/main/java/com/dsatracker/dsa_tracker/controller/StatsController.java

package com.dsatracker.dsa_tracker.controller;

import com.dsatracker.dsa_tracker.dto.*;
import com.dsatracker.dsa_tracker.model.User;
import com.dsatracker.dsa_tracker.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final com.dsatracker.dsa_tracker.repository.UserRepository userRepository;

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow()
                .getId();
    }

    @GetMapping("/overview")
    public ResponseEntity<StatsOverviewResponse> getOverview(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(statsService.getOverview(getUserId(userDetails)));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<List<HeatmapEntry>> getHeatmap(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "#{T(java.time.Year).now().getValue()}") int year) {
        return ResponseEntity.ok(statsService.getHeatmap(getUserId(userDetails), year));
    }

    @GetMapping("/tags")
    public ResponseEntity<List<TagStatsEntry>> getTagStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(statsService.getTagStats(getUserId(userDetails)));
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<WeeklyStatsEntry>> getWeekly(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "12") int weeks) {
        return ResponseEntity.ok(statsService.getWeeklyVelocity(getUserId(userDetails), weeks));
    }
}