package com.dsatracker.dsa_tracker.repository;

import com.dsatracker.dsa_tracker.model.DailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyActivityRepository extends JpaRepository<DailyActivity, Long> {


    // Find a specific day's row — used when updating after a problem is added
    Optional<DailyActivity> findByUserIdAndDate(Long userId, LocalDate date);

    // Fetch all activity for a given year — used for heatmap
    @Query("SELECT da FROM DailyActivity da WHERE da.user.id = :userId " +
            "AND da.date >= :startDate AND da.date <= :endDate")
    List<DailyActivity> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // For streak calculation — fetch all dates with activity, sorted ascending
    @Query("SELECT da.date FROM DailyActivity da WHERE da.user.id = :userId " +
            "AND da.totalCount > 0 ORDER BY da.date ASC")
    List<LocalDate> findActiveDatesByUserId(@Param("userId") Long userId);
}
