package com.dsatracker.dsa_tracker.repository;

import com.dsatracker.dsa_tracker.enums.Difficulty;
import com.dsatracker.dsa_tracker.enums.Platform;
import com.dsatracker.dsa_tracker.model.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    // Used for checking duplicates before API sync insert
    boolean existsByUserIdAndPlatformAndPlatformProblemId(
            Long userId, Platform platform, String platformProblemId);

    // Core filtering query — handles all optional filters
    @Query(
            value = """
        SELECT DISTINCT p FROM Problem p
        LEFT JOIN FETCH p.tags t
        WHERE p.user.id = :userId
          AND (:platform IS NULL OR p.platform = :platform)
          AND (:difficulty IS NULL OR p.difficulty = :difficulty)
          AND (:tagName IS NULL OR t.name = :tagName)
          AND (:from IS NULL OR p.solvedAt >= :from)
          AND (:to IS NULL OR p.solvedAt <= :to)
          AND (:needsRevision IS NULL OR p.needsRevision = :needsRevision)
        ORDER BY p.solvedAt DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT p) FROM Problem p
        LEFT JOIN p.tags t
        WHERE p.user.id = :userId
          AND (:platform IS NULL OR p.platform = :platform)
          AND (:difficulty IS NULL OR p.difficulty = :difficulty)
          AND (:tagName IS NULL OR t.name = :tagName)
          AND (:from IS NULL OR p.solvedAt >= :from)
          AND (:to IS NULL OR p.solvedAt <= :to)
          AND (:needsRevision IS NULL OR p.needsRevision = :needsRevision)
        """
    )
    Page<Problem> findFilteredProblems(
            @Param("userId") Long userId,
            @Param("platform") Platform platform,
            @Param("difficulty") Difficulty difficulty,
            @Param("tagName") String tagName,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("needsRevision") Boolean needsRevision,
            Pageable pageable);
}