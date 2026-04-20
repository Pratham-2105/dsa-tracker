package com.dsatracker.dsa_tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WeeklyStatsEntry {
    private String weekLabel;   // e.g., "2025-W03"
    private long count;
    private long easyCount;
    private long mediumCount;
    private long hardCount;
}