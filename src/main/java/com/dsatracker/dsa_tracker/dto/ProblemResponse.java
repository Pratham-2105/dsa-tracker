package com.dsatracker.dsa_tracker.dto;


import com.dsatracker.dsa_tracker.enums.Difficulty;
import com.dsatracker.dsa_tracker.enums.Platform;
import com.dsatracker.dsa_tracker.enums.ProblemSource;
import com.dsatracker.dsa_tracker.enums.ProblemStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class ProblemResponse {
    private Long id;
    private Platform platform;
    private String platformProblemId;
    private String title;
    private String url;
    private Difficulty difficulty;
    private LocalDateTime solvedAt;
    private Integer timeTakenMinutes;
    private String notes;
    private Boolean needsRevision;
    private ProblemStatus status;
    private ProblemSource source;
    private LocalDateTime createdAt;

    // Just strings, not Tag entities
    private Set<String> tagNames;
}
