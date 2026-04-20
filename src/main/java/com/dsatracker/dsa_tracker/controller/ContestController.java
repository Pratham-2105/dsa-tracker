// src/main/java/com/dsatracker/dsa_tracker/controller/ContestController.java

package com.dsatracker.dsa_tracker.controller;

import com.dsatracker.dsa_tracker.dto.ContestResponse;
import com.dsatracker.dsa_tracker.model.Contest;
import com.dsatracker.dsa_tracker.repository.ContestRepository;
import com.dsatracker.dsa_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contests")
@RequiredArgsConstructor
public class ContestController {

    private final ContestRepository contestRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ContestResponse>> getContests(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String platform) {

        Long userId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow().getId();

        List<Contest> contests = (platform != null)
                ? contestRepository.findByUserIdAndPlatformOrderByContestDateDesc(userId, platform.toUpperCase())
                : contestRepository.findByUserIdOrderByContestDateDesc(userId);

        List<ContestResponse> response = contests.stream()
                .map(c -> ContestResponse.builder()
                        .id(c.getId())
                        .platform(c.getPlatform().name())
                        .contestName(c.getContestName())
                        .contestDate(c.getContestDate())
                        .rank(c.getRank())
                        .ratingBefore(c.getRatingBefore())
                        .ratingAfter(c.getRatingAfter())
                        .ratingChange(c.getRatingAfter() != null && c.getRatingBefore() != null
                                ? c.getRatingAfter() - c.getRatingBefore() : null)
                        .problemsSolved(c.getProblemsSolved())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}