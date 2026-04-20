package com.dsatracker.dsa_tracker.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class ContestResponse {
    private Long id;
    private String platform;
    private String contestName;
    private LocalDate contestDate;
    private Integer rank;
    private Integer ratingBefore;
    private Integer ratingAfter;
    private Integer ratingChange;   // computed: after - before
    private Integer problemsSolved;
}