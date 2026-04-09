package com.dsatracker.dsa_tracker.repository;

import com.dsatracker.dsa_tracker.enums.Platform;
import com.dsatracker.dsa_tracker.model.Contest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContestRepository extends JpaRepository<Contest, Long> {
    List<Contest> findByUserIdAndPlatformOrderByContestDateDesc(Long userId, Platform platform);

    List<Contest> findByUserIdOrderByContestDateDesc(Long userId);

    boolean existsByUserIdAndPlatformAndContestName(Long userId, Platform platform, String contestName);
}
