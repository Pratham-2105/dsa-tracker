package com.dsatracker.dsa_tracker.dto.codeforces;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfSubmission {
    private Long id;
    private Integer contestId;
    private CfProblem problem;
    private String verdict;
    private Long creationTimeSeconds;

}
