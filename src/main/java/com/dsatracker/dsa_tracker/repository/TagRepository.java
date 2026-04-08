package com.dsatracker.dsa_tracker.repository;

import com.dsatracker.dsa_tracker.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    Set<Tag> findByNameIn(Set<String> names);
}

