package com.dsatracker.dsa_tracker.model;

import com.dsatracker.dsa_tracker.enums.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "problem",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_platform_problem",
                        columnNames = {"user_id", "platform", "platform_problem_id"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many problems belong to one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private Platform platform;

    @Column(name = "platform_problem_id", length = 100)
    private String platformProblemId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "url", length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 10)
    private Difficulty difficulty;

    @Column(name = "solved_at", nullable = false)
    private LocalDateTime solvedAt;

    @Column(name = "time_taken_minutes")
    private Integer timeTakenMinutes;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "needs_revision")
    private Boolean needsRevision = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ProblemStatus status = ProblemStatus.SOLVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20)
    private ProblemSource source = ProblemSource.MANUAL;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Many-to-many with Tag via problem_tags join table
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "problem_tags",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();
}
