package com.dsatracker.dsa_tracker.dto.codeforces;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfSubmissionResponse {
    private String status;
    private List<CfSubmission> result;
}
