package com.dsatracker.dsa_tracker.dto.leetcode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LcContestDetail {
    private String title;
    private Long startTime;
}
