package com.dsatracker.dsa_tracker.dto.leetcode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/*
 * This single DTO replaces both LcSubmission AND LcProblemDetail.
 *
 * The old approach needed TWO separate API calls:
 *   1. recentSubmissionList → gave us titleSlug + timestamp (no tags, no difficulty)
 *   2. problemsetQuestionList → gave us difficulty + tags (catalog lookup)
 *
 * The new userProgressQuestionList query returns EVERYTHING in one shot:
 *   title, slug, difficulty, tags, submission timestamp, status — all in one object.
 *
 * This is only possible because it's an AUTHENTICATED query — LeetCode knows
 * who is asking and returns their personal submission data enriched with
 * problem metadata. The unauthenticated queries intentionally split this data
 * to limit what anonymous users can access.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LcProgressQuestion {
    private String frontendId;        // "1", "239", etc. — LC's problem number
    private String title;             // "Two Sum"
    private String titleSlug;         // "two-sum"
    private String difficulty;        // "Easy", "Medium", "Hard"
    private String lastSubmittedAt;
    private Integer numSubmitted;     // how many times user submitted
    private String questionStatus;    // "ac" = accepted, "notac" = attempted
    private String lastResult;        // "Accepted", "Wrong Answer", etc.
    private List<LcTopicTag> topicTags;
}