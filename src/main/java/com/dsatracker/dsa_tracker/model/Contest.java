package com.dsatracker.dsa_tracker.model;

import com.dsatracker.dsa_tracker.enums.Platform;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "contest",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_platform_contest",
                        columnNames = {"user_id", "platform", "contest_name"}
                )
        })
@Data
@NoArgsConstructor @AllArgsConstructor
public class Contest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private Platform platform;

    @Column(name = "contest_name", length = 200)
    private String contestName;

    @Column(name = "contest_date")
    private LocalDate contestDate;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "rating_before")
    private Integer ratingBefore;

    @Column(name = "rating_after")
    private Integer ratingAfter;

    @Column(name = "problems_solved")
    private Integer problemsSolved;
}
