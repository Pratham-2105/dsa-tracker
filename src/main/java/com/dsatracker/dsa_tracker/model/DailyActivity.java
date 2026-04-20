package com.dsatracker.dsa_tracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "daily_activity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Builder.Default
    private int totalCount = 0;

    @Builder.Default
    private int easyCount = 0;

    @Builder.Default
    private int mediumCount = 0;

    @Builder.Default
    private int hardCount = 0;
}
