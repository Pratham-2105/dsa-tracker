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
import java.util.List;

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
          AND (CAST(:from AS timestamp) IS NULL OR p.solvedAt >= :from)
          AND (CAST(:to AS timestamp) IS NULL OR p.solvedAt <= :to)
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
          AND (CAST(:from AS timestamp) IS NULL OR p.solvedAt >= :from)
          AND (CAST(:to AS timestamp) IS NULL OR p.solvedAt <= :to)
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

    // Count by platform for a user
    @Query("SELECT p.platform, COUNT(p) FROM Problem p WHERE p.user.id = :userId GROUP BY p.platform")
    List<Object[]> countByPlatformForUser(@Param("userId") Long userId);

    // Count by difficulty for a user
    @Query("SELECT p.difficulty, COUNT(p) FROM Problem p WHERE p.user.id = :userId GROUP BY p.difficulty")
    List<Object[]> countByDifficultyForUser(@Param("userId") Long userId);

    // Tag distribution — count of problems per tag for a user
    @Query("SELECT t.name, COUNT(p) FROM Problem p JOIN p.tags t WHERE p.user.id = :userId GROUP BY t.name ORDER BY COUNT(p) DESC")
    List<Object[]> countByTagForUser(@Param("userId") Long userId);

    // Weekly velocity — problems per ISO week for last N weeks
    @Query(value = """
    SELECT TO_CHAR(DATE_TRUNC('week', solved_at), 'IYYY-"W"IW') AS week_label,
           COUNT(*) AS total,
           SUM(CASE WHEN difficulty = 'EASY' THEN 1 ELSE 0 END) AS easy,
           SUM(CASE WHEN difficulty = 'MEDIUM' THEN 1 ELSE 0 END) AS medium,
           SUM(CASE WHEN difficulty = 'HARD' THEN 1 ELSE 0 END) AS hard
    FROM problem
    WHERE user_id = :userId AND solved_at >= :since
    GROUP BY week_label
    ORDER BY week_label ASC
    """, nativeQuery = true)
    List<Object[]> weeklyVelocity(@Param("userId") Long userId, @Param("since") java.time.LocalDateTime since);

    // This week's count
    @Query("SELECT COUNT(p) FROM Problem p WHERE p.user.id = :userId AND p.solvedAt >= :weekStart")
    long countThisWeek(@Param("userId") Long userId, @Param("weekStart") java.time.LocalDateTime weekStart);
}