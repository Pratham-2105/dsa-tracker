package com.dsatracker.dsa_tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TagStatsEntry {
    private String tagName;
    private long count;
}