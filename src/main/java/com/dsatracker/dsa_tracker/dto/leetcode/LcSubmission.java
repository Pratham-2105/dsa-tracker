package com.dsatracker.dsa_tracker.dto.leetcode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LcSubmission {
    private String id;
    private String title;
    private String titleSlug;
    private String timestamp;   // LeetCode sends this as a String, not a Long
    private String statusDisplay;
}
