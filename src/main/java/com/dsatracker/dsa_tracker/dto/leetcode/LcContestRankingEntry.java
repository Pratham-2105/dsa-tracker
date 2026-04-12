package com.dsatracker.dsa_tracker.dto.leetcode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LcContestRankingEntry {
    private Boolean attended;
    private Double rating;
    private Integer ranking;
    private Integer problemsSolved;
    private LcContestDetail contest;
}
