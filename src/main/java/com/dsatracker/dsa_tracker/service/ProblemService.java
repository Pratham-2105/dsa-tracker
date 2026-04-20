package com.dsatracker.dsa_tracker.service;

import com.dsatracker.dsa_tracker.dto.ProblemRequest;
import com.dsatracker.dsa_tracker.dto.ProblemResponse;
import com.dsatracker.dsa_tracker.enums.Difficulty;
import com.dsatracker.dsa_tracker.enums.Platform;
import com.dsatracker.dsa_tracker.enums.ProblemSource;
import com.dsatracker.dsa_tracker.exception.ResourceNotFoundException;
import com.dsatracker.dsa_tracker.model.Problem;
import com.dsatracker.dsa_tracker.model.Tag;
import com.dsatracker.dsa_tracker.model.User;
import com.dsatracker.dsa_tracker.repository.ProblemRepository;
import com.dsatracker.dsa_tracker.repository.TagRepository;
import com.dsatracker.dsa_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    private final DailyActivityService dailyActivityService;

    /**
     * Create a new problem (manual entry by user)
     */

    @Transactional
    public ProblemResponse createProblem(ProblemRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found : " + userEmail));


        Set<Tag> tags = resolveOrCreateTags(request.getTagNames());

        Problem problem = new Problem();
        problem.setUser(user);
        problem.setPlatform((request.getPlatform()));
        problem.setPlatformProblemId(request.getPlatformProblemId());
        problem.setTitle(request.getTitle());
        problem.setUrl(request.getUrl());
        problem.setDifficulty(request.getDifficulty());
        problem.setSolvedAt(request.getSolvedAt());
        problem.setTimeTakenMinutes(request.getTimeTakenMinutes());
        problem.setNotes(request.getNotes());
        problem.setNeedsRevision(request.getNeedsRevision());
        problem.setStatus(request.getStatus());
        problem.setSource(ProblemSource.MANUAL);
        problem.setTags(tags);

        Problem saved = problemRepository.save(problem);
        dailyActivityService.recordProblemAdded(saved);
        return toResponse(saved);
    }

    /**
     * Get all problems for a user with optional filters
     */
    @Transactional(readOnly = true)
    public Page<ProblemResponse> getProblems(
            String userEmail,
            Platform platform,
            Difficulty difficulty,
            String tagName,
            LocalDateTime from,
            LocalDateTime to,
            Boolean needsRevision,
            Pageable pageable) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(("User not found: " + userEmail)));

        return problemRepository.findFilteredProblems(
                user.getId(), platform, difficulty, tagName, from, to, needsRevision, pageable
        ).map(this::toResponse);
    }

    /**
     * Update a problem — only the owner can update
     */
    @Transactional
    public ProblemResponse updateProblem(Long problemId, ProblemRequest request, String userEmail) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found: " + problemId));

        // Security check: only the owner can update their problem
        if (!problem.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You don't own this problem");
        }

        Set<Tag> tags = resolveOrCreateTags(request.getTagNames());

        problem.setTitle(request.getTitle());
        problem.setUrl(request.getUrl());
        problem.setDifficulty(request.getDifficulty());
        problem.setSolvedAt(request.getSolvedAt());
        problem.setTimeTakenMinutes(request.getTimeTakenMinutes());
        problem.setNotes(request.getNotes());
        problem.setNeedsRevision(request.getNeedsRevision());
        problem.setStatus(request.getStatus());
        problem.setTags(tags);

        Problem saved = problemRepository.save(problem);
        return toResponse(saved);
    }

    /**
     * Delete a problem — only the owner can delete
     */
    @Transactional
    public void deleteProblem(Long problemId, String userEmail) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found: " + problemId));

        if (!problem.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You don't own this problem");
        }

        dailyActivityService.recordProblemRemoved(problem);
        problemRepository.delete(problem);
    }

    /**
     * Given a set of tag names, find existing tags or create new ones.
     * This is the "find-or-create" pattern.
     */

    @Transactional
    public Set<Tag> resolveOrCreateTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }

        // Normalize tag names: trim whitespace, title-case
        Set<String> normalizedNames = tagNames.stream()
                .map(String::trim)
                .collect(Collectors.toSet());

        // Find all already-existing tags in one query
        Set<Tag> existingTags = tagRepository.findByNameIn(normalizedNames);
        Set<String> existingNames = existingTags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        // For any tag name that doesn't exist yet, create it
        Set<Tag> newTags = normalizedNames.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> {
                    Tag tag = new Tag();
                    tag.setName(name);
                    return tagRepository.save(tag);
                })
                .collect(Collectors.toSet());

        existingTags.addAll(newTags);
        return existingTags;
    }

    /**
     * Convert Problem entity to ProblemResponse DTO
     */
    private ProblemResponse toResponse(Problem problem) {
        Set<String> tagNames = problem.getTags() == null
                ? new HashSet<>()
                : problem.getTags().stream().map(Tag::getName).collect(Collectors.toSet());

        return ProblemResponse.builder()
                .id(problem.getId())
                .platform(problem.getPlatform())
                .platformProblemId(problem.getPlatformProblemId())
                .title(problem.getTitle())
                .url(problem.getUrl())
                .difficulty(problem.getDifficulty())
                .solvedAt(problem.getSolvedAt())
                .timeTakenMinutes(problem.getTimeTakenMinutes())
                .notes(problem.getNotes())
                .needsRevision(problem.getNeedsRevision())
                .status(problem.getStatus())
                .source(problem.getSource())
                .createdAt(problem.getCreatedAt())
                .tagNames(tagNames)
                .build();
    }
}
