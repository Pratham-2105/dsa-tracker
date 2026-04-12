package com.dsatracker.dsa_tracker.service;

import com.dsatracker.dsa_tracker.dto.codeforces.*;
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
import lombok.RequiredArgsConstructor;
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
public class CodeforcesService {

    private final WebClient codeforcesWebClient;
    private final ProblemRepository problemRepository;
    private final ContestRepository contestRepository;
    private final ProblemService problemService;

    public CodeforcesService(
            @Qualifier("codeforcesWebClient") WebClient codeforcesWebClient,
            ProblemRepository problemRepository,
            ContestRepository contestRepository,
            ProblemService problemService) {
        this.codeforcesWebClient = codeforcesWebClient;
        this.problemRepository = problemRepository;
        this.contestRepository = contestRepository;
        this.problemService = problemService;
    }

    /**
     * Main sync method — fetches submissions + contests from CF and saves them.
     * Returns the number of NEW problems imported (not duplicates).
     */
    @Transactional
    public int syncSubmissions(User user) {
        String handle = user.getCodeforcesHandle();
        if (handle == null || handle.isBlank()) {
            throw new PlatformSyncException("No Codeforces handle set for user: " + user.getEmail());
        }

        log.info("Starting Codeforces sync for handle: {}", handle);

        // FETCH stage
        List<CfSubmission> submissions = fetchAllSubmissions(handle);
        List<CfRatingChange> ratingChanges = fetchRatingChanges(handle);

        // TRANSFORM + LOAD stage (submissions)
        int importedCount = processSubmissions(submissions, user);

        // TRANSFORM + LOAD stage (contests)
        processContests(ratingChanges, user);

        log.info("Codeforces syn complete for {}. Imported {} new problems.", handle, importedCount);
        return importedCount;
    }


    // ==================== FETCH STAGE ====================

    private List<CfSubmission> fetchAllSubmissions(String handle) {
        try {
            CfSubmissionResponse response = codeforcesWebClient
                    .get()
                    .uri("/user.status?handle={handle}", handle)
                    .retrieve()
                    .bodyToMono(CfSubmissionResponse.class)
                    .block();

            if (response == null || !"OK".equals(response.getStatus())) {
                throw new PlatformSyncException(
                        "Codeforces API returned non-OK status for handle: " + handle);

            }
            return response.getResult() != null ? response.getResult() : Collections.emptyList();

        } catch (Exception e) {
            // Rating fetch is non-critical - log and continue
            log.warn("Failed to fetch CF rating history for {}: {}", handle, e.getMessage());
            return Collections.emptyList();
        }
    }

    private int processSubmissions(List<CfSubmission> submissions, User user) {
        // Step 1: Filter to only ACCEPTED submissions
        List<CfSubmission> accepted = submissions.stream()
                .filter(s -> "OK".equals(s.getVerdict()))
                .collect(Collectors.toList());

        // Step 2: Deduplicate within the batch itself
        // A user might have multiple AC submissions for the same problem
        // (different languages, re-submissions). Keep only the FIRST (earliest).
        Map<String, CfSubmission> uniqueByProblemId = new LinkedHashMap<>();
        for (CfSubmission sub : accepted) {
            String problemId = buildPlatformProblemId(sub);
            if (problemId != null && !uniqueByProblemId.containsKey(problemId)) {
                uniqueByProblemId.put(problemId, sub);
            }
        }

        // Step 3: Filter out problems that already exists in our DB
        int importedCount = 0;
        for (CfSubmission sub : uniqueByProblemId.values()) {
            String platformProblemId = buildPlatformProblemId(sub);

            boolean alreadyExists = problemRepository
                    .existsByUserIdAndPlatformAndPlatformProblemId(
                            user.getId(), Platform.CODEFORCES, platformProblemId
                    );

            if (alreadyExists) continue;

            // Step 4: Transform and save
            Problem problem = mapToProblem(sub, user);
            problemRepository.save(problem);
            importedCount++;
        }

        return importedCount;
    }

    private Problem mapToProblem(CfSubmission submission, User user) {
        CfProblem cfProblem = submission.getProblem();

        // Resolve tags (reuse the find-or-create logic from ProblemService)
        Set<String> tagNames = cfProblem.getTags() != null
                ? new HashSet<>(cfProblem.getTags())
                : new HashSet<>();
        Set<Tag> tags = problemService.resolveOrCreateTags(tagNames);

        // Convert epoch seconds → LocalDateTime
        LocalDateTime solvedAt = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(submission.getCreationTimeSeconds()),
                ZoneId.of("UTC")
        );

        // Build the url
        String url = builderProblemUrl(submission);

        Problem problem = new Problem();
        problem.setUser(user);
        problem.setPlatform(Platform.CODEFORCES);
        problem.setPlatformProblemId(buildPlatformProblemId(submission));
        problem.setTitle(cfProblem.getName());
        problem.setUrl(url);
        problem.setDifficulty(mapDifficulty(cfProblem.getRating()));
        problem.setSolvedAt(solvedAt);
        problem.setNeedsRevision(false);
        problem.setStatus(ProblemStatus.SOLVED);
        problem.setSource(ProblemSource.API);
        problem.setTags(tags);

        return problem;
    }

    // ==================== TRANSFORM + LOAD: CONTESTS ====================

    private void processContests(List<CfRatingChange> ratingChanges, User user) {
        for (CfRatingChange change : ratingChanges) {
            boolean exists = contestRepository.existsByUserIdAndPlatformAndContestName(
                    user.getId(), Platform.CODEFORCES, change.getContestName()
            );

            if (exists) continue;

            Contest contest = new Contest();
            contest.setUser(user);
            contest.setPlatform(Platform.CODEFORCES);
            contest.setContestName(change.getContestName());
            contest.setContestDate(LocalDate.ofInstant(
                    Instant.ofEpochSecond(change.getRatingUpdateTimeSeconds()),
                    ZoneId.of("UTC")));
            contest.setRank(change.getRank());
            contest.setRatingBefore(change.getOldRating());
            contest.setRatingAfter(change.getNewRating());

            contestRepository.save(contest);
        }
    }

    private List<CfRatingChange> fetchRatingChanges(String handle) {
        try {
            CfRatingResponse response = codeforcesWebClient
                    .get()
                    .uri("/user.rating?handle={handle}", handle)
                    .retrieve()
                    .bodyToMono(CfRatingResponse.class)
                    .block();

            if (response == null || !"OK".equals(response.getStatus())) {
                log.warn("Could not fetch rating history for handle: {}", handle);
                return Collections.emptyList();
            }
            return response.getResult() != null ? response.getResult() : Collections.emptyList();

        } catch (WebClientResponseException.NotFound e) {
            throw new PlatformSyncException("Codeforces handle not found: " + handle);
        } catch (WebClientResponseException e) {
            throw new PlatformSyncException(
                    "Codeforces API error (HTTP " + e.getStatusCode().value() + "): " + e.getMessage());
        } catch (Exception e) {
            if (e instanceof PlatformSyncException) throw e;
            throw new PlatformSyncException("Failed to fetch from Codeforces: " + e.getMessage());
        }
    }

    // ==================== HELPER METHODS ====================

    private String buildPlatformProblemId(CfSubmission submission) {
        CfProblem p = submission.getProblem();
        if (p.getContestId() != null && p.getIndex() != null) {
            return p.getContestId() + p.getIndex();
        }

        // Gym or practice problems might not have contestId
        return p.getName();  // fallback to problem name
    }

    private String builderProblemUrl(CfSubmission submission) {
        CfProblem p = submission.getProblem();
        if (p.getContestId() != null && p.getIndex() != null) {
            return "https://codeforces.com/contest/" + p.getContestId()
                    + "/problem/" + p.getIndex();
        }

        return null;  // can't construct URL without contestId
    }

    private Difficulty mapDifficulty(Integer rating) {
        if (rating == null) return Difficulty.MEDIUM;  // unrated → default
        if (rating <= 1200) return Difficulty.EASY;
        if (rating <= 1800) return Difficulty.MEDIUM;
        return Difficulty.HARD;
    }
}
