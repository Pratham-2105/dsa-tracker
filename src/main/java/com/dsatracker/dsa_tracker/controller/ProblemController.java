package com.dsatracker.dsa_tracker.controller;

import com.dsatracker.dsa_tracker.dto.ProblemRequest;
import com.dsatracker.dsa_tracker.dto.ProblemResponse;
import com.dsatracker.dsa_tracker.enums.Difficulty;
import com.dsatracker.dsa_tracker.enums.Platform;
import com.dsatracker.dsa_tracker.service.ProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;


@RestController
@RequestMapping("/api/problem")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    /**
     * POST /api/problems — manual problem entry
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProblemResponse createProblem(
            @Valid @RequestBody ProblemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return problemService.createProblem(request, userDetails.getUsername());
    }

    /**
     * GET /api/problems?platform=&difficulty=&tagName=&from=&to=&revision=&page=&size=
     */
    @GetMapping
    public Page<ProblemResponse> getProblems(
            @RequestParam(required = false) Platform platform,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String tagName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Boolean needsRevision,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("solvedAt").descending());
        return problemService.getProblems(
                userDetails.getUsername(), platform, difficulty, tagName, from, to, needsRevision, pageable);
    }

    /**
     * PUT /api/problems/{id} — update a problem
     */
    @PutMapping("/{id}")
    public ProblemResponse updateProblem(
            @PathVariable Long id,
            @Valid @RequestBody ProblemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return problemService.updateProblem(id, request, userDetails.getUsername());
    }

    /**
     * DELETE /api/problems/{id}
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProblem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        problemService.deleteProblem(id, userDetails.getUsername());
    }
}
