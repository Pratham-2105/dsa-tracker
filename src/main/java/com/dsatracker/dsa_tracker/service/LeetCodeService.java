package com.dsatracker.dsa_tracker.service;

import com.dsatracker.dsa_tracker.dto.leetcode.*;
import com.dsatracker.dsa_tracker.enums.Difficulty;
import com.dsatracker.dsa_tracker.enums.Platform;
import com.dsatracker.dsa_tracker.enums.ProblemSource;
import com.dsatracker.dsa_tracker.enums.ProblemStatus;
import com.dsatracker.dsa_tracker.exception.PlatformSyncException;
import com.dsatracker.dsa_tracker.model.Contest;
import com.dsatracker.dsa_tracker.model.Problem;
import com.dsatracker.dsa_tracker.model.Tag;
import com.dsatracker.dsa_tracker.model.User;
import com.dsatracker.dsa_tracker.repository.ContestRepository;
import com.dsatracker.dsa_tracker.repository.ProblemRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LeetCodeService {

    private final WebClient leetcodeWebClient;
    private final ProblemRepository problemRepository;
    private final ContestRepository contestRepository;
    private final ProblemService problemService;
    private final ObjectMapper objectMapper;
    private final DailyActivityService dailyActivityService;

    public LeetCodeService(
            @Qualifier("leetcodeWebClient") WebClient leetcodeWebClient,
            ProblemRepository problemRepository,
            ContestRepository contestRepository,
            ProblemService problemService,
            ObjectMapper objectMapper,
            DailyActivityService dailyActivityService) {
        this.leetcodeWebClient = leetcodeWebClient;
        this.problemRepository = problemRepository;
        this.contestRepository = contestRepository;
        this.problemService = problemService;
        this.objectMapper = objectMapper;
        this.dailyActivityService = dailyActivityService;
    }

    // ==================== MAIN ENTRY POINT ====================

    @Transactional
    public int syncSubmissions(User user) {
        String username = user.getLeetcodeUsername();
        if (username == null || username.isBlank()) {
            throw new PlatformSyncException("No LeetCode username set for user: " + user.getEmail());
        }

        String sessionCookie = user.getLeetcodeSessionCookie();
        if (sessionCookie == null || sessionCookie.isBlank()) {
            throw new PlatformSyncException(
                    "No LeetCode session cookie set. Please add your LEETCODE_SESSION cookie in Settings to enable full sync.");
        }

        log.info("Starting LeetCode sync for username: {} (authenticated mode)", username);

        /*
         * ===== THE NEW APPROACH: ONE QUERY DOES EVERYTHING =====
         *
         * OLD (unauthenticated, Thread 4 v1):
         *   Step 1: recentSubmissionList → capped at 20, gives titleSlug + timestamp only
         *   Step 2: problemsetQuestionList → paginate 3300 problems to find tags + difficulty
         *   Step 3: Cross-reference the two
         *   Result: 20 problems, most without tags
         *
         * NEW (authenticated):
         *   Step 1: userProgressQuestionList → paginate with skip/limit
         *   That's it. Returns title, slug, difficulty, tags, timestamp, status.
         *   Result: ALL 279 problems with complete metadata
         *
         * This is possible because authenticated queries return the user's
         * personal data enriched with problem metadata in a single response.
         */

        // STEP 1: Fetch all solved problems via authenticated paginated query
        List<LcProgressQuestion> solvedProblems = fetchAllSolvedProblems(sessionCookie);

        // STEP 2: Deduplicate and save
        int importedCount = processProblems(solvedProblems, user);

        // STEP 3: Fetch and save contest history (non-critical, uses username not cookie)
        processContests(username, user);

        log.info("LeetCode sync complete for {}. Imported {} new problems.", username, importedCount);
        return importedCount;
    }

    // ==================== FETCH STAGE: AUTHENTICATED SUBMISSIONS ====================

    /*
     * Paginates through userProgressQuestionList with skip/limit.
     * Each page returns up to 50 questions with full metadata.
     * Filters for questionStatus == "ac" (accepted) only.
     *
     * For a user with 279 solved problems: 6 pages × 50 = done.
     * Compare to the old approach: 20 problems max, period.
     */
    private List<LcProgressQuestion> fetchAllSolvedProblems(String sessionCookie) {
        String query = """
                query userProgressQuestionList($filters: UserProgressQuestionListInput) {
                    userProgressQuestionList(filters: $filters) {
                        totalNum
                        questions {
                            frontendId
                            title
                            titleSlug
                            difficulty
                            lastSubmittedAt
                            numSubmitted
                            questionStatus
                            lastResult
                            topicTags {
                                name
                                slug
                            }
                        }
                    }
                }
                """;

        List<LcProgressQuestion> allSolved = new ArrayList<>();
        int skip = 0;
        int limit = 50;       // matches what LC's own frontend uses
        boolean hasMore = true;

        try {
            while (hasMore) {
                Map<String, Object> variables = Map.of(
                        "filters", Map.of("skip", skip, "limit", limit)
                );

                JsonNode root = executeAuthenticatedQuery(query, variables, sessionCookie);
                JsonNode progressNode = root.path("data").path("userProgressQuestionList");

                if (progressNode.isMissingNode() || progressNode.isNull()) {
                    throw new PlatformSyncException(
                            "LeetCode session cookie may be expired or invalid. Please update it in Settings.");
                }

                int totalNum = progressNode.path("totalNum").asInt(0);
                JsonNode questionsNode = progressNode.path("questions");

                if (questionsNode.isArray() && questionsNode.size() > 0) {
                    // DEBUG — remove after fixing
                    log.info("SAMPLE questionStatus values: {}",
                            questionsNode.get(0).path("questionStatus").asText() + ", " +
                                    questionsNode.get(0).path("lastResult").asText());

                    List<LcProgressQuestion> page = objectMapper.convertValue(
                            questionsNode,
                            new TypeReference<List<LcProgressQuestion>>() {}
                    );

                    // Filter for accepted only — the query returns all attempted problems too
                    List<LcProgressQuestion> accepted = page.stream()
                            .filter(q -> "SOLVED".equalsIgnoreCase(q.getQuestionStatus()))
                            .collect(Collectors.toList());

                    allSolved.addAll(accepted);

                    log.info("LC progress page: skip={}, fetched={}, accepted={}, total so far={}",
                            skip, page.size(), accepted.size(), allSolved.size());

                    skip += limit;
                    hasMore = skip < totalNum;
                } else {
                    hasMore = false;
                }
            }

            log.info("Fetched {} total accepted problems from LeetCode.", allSolved.size());
            return allSolved;

        } catch (PlatformSyncException e) {
            throw e;
        } catch (Exception e) {
            throw new PlatformSyncException("Failed to fetch LeetCode submissions: " + e.getMessage());
        }
    }

    // ==================== FETCH STAGE: CONTESTS (unchanged, uses username) ====================

    private List<LcContestRankingEntry> fetchContestHistory(String username) {
        String query = """
                query userContestRankingInfo($username: String!) {
                    userContestRankingHistory(username: $username) {
                        attended
                        rating
                        ranking
                        problemsSolved
                        contest {
                            title
                            startTime
                        }
                    }
                }
                """;

        Map<String, Object> variables = Map.of("username", username);

        try {
            // Contest history is PUBLIC — no cookie needed
            JsonNode root = executeGraphQlQuery(query, variables);
            JsonNode historyNode = root.path("data").path("userContestRankingHistory");

            if (historyNode.isMissingNode() || historyNode.isNull()) {
                log.warn("No contest history found for LeetCode user: {}", username);
                return Collections.emptyList();
            }

            List<LcContestRankingEntry> history = objectMapper.convertValue(
                    historyNode,
                    new TypeReference<List<LcContestRankingEntry>>() {}
            );

            return history.stream()
                    .filter(entry -> Boolean.TRUE.equals(entry.getAttended()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Failed to fetch LC contest history for {}: {}", username, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== TRANSFORM + LOAD: PROBLEMS ====================

    private int processProblems(List<LcProgressQuestion> solvedProblems, User user) {
        int importedCount = 0;

        for (LcProgressQuestion question : solvedProblems) {
            if (question.getTitleSlug() == null) continue;

            String platformProblemId = question.getTitleSlug();

            boolean alreadyExists = problemRepository
                    .existsByUserIdAndPlatformAndPlatformProblemId(
                            user.getId(), Platform.LEETCODE, platformProblemId);

            if (alreadyExists) continue;

            Problem problem = mapToProblem(question, user);
            problemRepository.save(problem);
            dailyActivityService.recordProblemAdded(problem);
            importedCount++;
        }

        return importedCount;
    }

    /*
     * Notice how much simpler this mapping is compared to the old version:
     *
     * OLD: mapToProblem(LcSubmission, Map<String, LcProblemDetail> catalog, User)
     *      — needed the catalog map to look up difficulty and tags
     *      — catalog often missed problems → fallback to MEDIUM, empty tags
     *
     * NEW: mapToProblem(LcProgressQuestion, User)
     *      — everything is in ONE object, no lookups needed
     *      — difficulty and tags come directly from the query
     */
    private Problem mapToProblem(LcProgressQuestion question, User user) {
        // Difficulty comes directly — no heuristic, no catalog lookup
        Difficulty difficulty = mapDifficulty(question.getDifficulty());

        // Tags come directly — no separate catalog query
        Set<String> tagNames = new HashSet<>();
        if (question.getTopicTags() != null) {
            tagNames = question.getTopicTags().stream()
                    .map(LcTopicTag::getName)
                    .collect(Collectors.toSet());
        }
        Set<Tag> tags = problemService.resolveOrCreateTags(tagNames);

        LocalDateTime solvedAt = LocalDateTime.now(); // default
        if (question.getLastSubmittedAt() != null && !question.getLastSubmittedAt().isBlank()) {
            try {
                solvedAt = LocalDateTime.ofInstant(
                        java.time.OffsetDateTime.parse(question.getLastSubmittedAt()).toInstant(),
                        ZoneId.of("UTC"));
            } catch (Exception e) {
                log.warn("Could not parse timestamp '{}', using current time", question.getLastSubmittedAt());
            }
        }

        String url = "https://leetcode.com/problems/" + question.getTitleSlug() + "/";

        Problem problem = new Problem();
        problem.setUser(user);
        problem.setPlatform(Platform.LEETCODE);
        problem.setPlatformProblemId(question.getTitleSlug());
        problem.setTitle(question.getTitle());
        problem.setUrl(url);
        problem.setDifficulty(difficulty);
        problem.setSolvedAt(solvedAt);
        problem.setNeedsRevision(false);
        problem.setStatus(ProblemStatus.SOLVED);
        problem.setSource(ProblemSource.API);
        problem.setTags(tags);

        return problem;
    }

    // ==================== TRANSFORM + LOAD: CONTESTS ====================

    private void processContests(String username, User user) {
        List<LcContestRankingEntry> contests = fetchContestHistory(username);

        int previousRating = 1500;

        for (LcContestRankingEntry entry : contests) {
            if (entry.getContest() == null || entry.getContest().getTitle() == null) continue;

            String contestName = entry.getContest().getTitle();

            boolean exists = contestRepository.existsByUserIdAndPlatformAndContestName(
                    user.getId(), Platform.LEETCODE, contestName);

            if (exists) continue;

            int ratingAfter = entry.getRating() != null ? entry.getRating().intValue() : previousRating;

            Contest contest = new Contest();
            contest.setUser(user);
            contest.setPlatform(Platform.LEETCODE);
            contest.setContestName(contestName);
            contest.setRank(entry.getRanking());
            contest.setRatingBefore(previousRating);
            contest.setRatingAfter(ratingAfter);
            contest.setProblemsSolved(entry.getProblemsSolved());

            if (entry.getContest().getStartTime() != null) {
                contest.setContestDate(LocalDate.ofInstant(
                        Instant.ofEpochSecond(entry.getContest().getStartTime()),
                        ZoneId.of("UTC")));
            }

            contestRepository.save(contest);
            previousRating = ratingAfter;
        }
    }

    // ==================== HTTP HELPERS ====================

    /*
     * Authenticated GraphQL call — sends the LEETCODE_SESSION cookie.
     *
     * Why a separate method from executeGraphQlQuery?
     * Not all queries need auth. Contest history is public (uses username).
     * Problem progress is private (uses cookie). Keeping them separate
     * makes it clear which queries need credentials and which don't.
     */
    private JsonNode executeAuthenticatedQuery(String query, Map<String, Object> variables, String sessionCookie) {
        Map<String, Object> requestBody = Map.of(
                "query", query,
                "variables", variables
        );

        try {
            return leetcodeWebClient
                    .post()
                    .uri("/graphql")
                    .header("Cookie", "LEETCODE_SESSION=" + sessionCookie)
                    .header("Referer", "https://leetcode.com/progress/")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

        } catch (WebClientResponseException.Forbidden e) {
            throw new PlatformSyncException(
                    "LeetCode returned 403 — session cookie may be expired. Please update it in Settings.");
        } catch (WebClientResponseException e) {
            throw new PlatformSyncException(
                    "LeetCode API error (HTTP " + e.getStatusCode().value() + "): " + e.getMessage());
        } catch (Exception e) {
            if (e instanceof PlatformSyncException) throw e;
            throw new PlatformSyncException("Failed to call LeetCode API: " + e.getMessage());
        }
    }

    /*
     * Unauthenticated GraphQL call — for public queries like contest history.
     */
    private JsonNode executeGraphQlQuery(String query, Map<String, Object> variables) {
        Map<String, Object> requestBody = Map.of(
                "query", query,
                "variables", variables
        );

        try {
            return leetcodeWebClient
                    .post()
                    .uri("/graphql")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

        } catch (WebClientResponseException e) {
            throw new PlatformSyncException(
                    "LeetCode API error (HTTP " + e.getStatusCode().value() + "): " + e.getMessage());
        } catch (Exception e) {
            if (e instanceof PlatformSyncException) throw e;
            throw new PlatformSyncException("Failed to call LeetCode API: " + e.getMessage());
        }
    }

    // ==================== HELPERS ====================

    private Difficulty mapDifficulty(String lcDifficulty) {
        if (lcDifficulty == null) return Difficulty.MEDIUM;
        return switch (lcDifficulty.toUpperCase()) {
            case "EASY" -> Difficulty.EASY;
            case "MEDIUM" -> Difficulty.MEDIUM;
            case "HARD" -> Difficulty.HARD;
            default -> Difficulty.MEDIUM;
        };
    }
}