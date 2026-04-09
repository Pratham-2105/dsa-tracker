package com.dsatracker.dsa_tracker.dto.codeforces;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfRatingChange {
    private Integer contestId;
    private String contestName;
    private Integer rank;
    private Integer oldRating;
    private Integer newRating;
    private Long ratingUpdateTimeSeconds;
}
