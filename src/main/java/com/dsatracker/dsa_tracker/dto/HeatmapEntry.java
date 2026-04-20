package com.dsatracker.dsa_tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class HeatmapEntry {
    private LocalDate date;
    private int count;
    private int easyCount;
    private int mediumCount;
    private int hardCount;

}
