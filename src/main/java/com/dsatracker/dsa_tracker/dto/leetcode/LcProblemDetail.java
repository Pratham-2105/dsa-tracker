package com.dsatracker.dsa_tracker.dto.leetcode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LcProblemDetail {
    private String questionId;
    private String title;
    private String titleSlug;
    private String difficulty;
    private List<LcTopicTag> topicTags;
}
