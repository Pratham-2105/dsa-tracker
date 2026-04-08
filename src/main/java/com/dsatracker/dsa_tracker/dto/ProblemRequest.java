package com.dsatracker.dsa_tracker.dto;

import com.dsatracker.dsa_tracker.enums.Difficulty;
import com.dsatracker.dsa_tracker.enums.Platform;
import com.dsatracker.dsa_tracker.enums.ProblemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class ProblemRequest {

    @NotNull(message = "Platform is required")
    private Platform platform;

    private String platformProblemId;

    @NotBlank(message = "Title is required")
    private String title;

    private String url;

    @NotNull(message = "Difficulty is required")
    private Difficulty difficulty;

    @NotNull(message = "Solved date is required")
    private LocalDateTime solvedAt;

    private Integer timeTakenMinutes;

    private String notes;

    private Boolean needsRevision = false;

    private ProblemStatus status = ProblemStatus.SOLVED;

    private Set<String> tagNames; // e.g., ["Graph", "BFS", "DFS"]
}
