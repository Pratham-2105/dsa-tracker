package com.dsatracker.dsa_tracker.model;

import com.dsatracker.dsa_tracker.enums.Platform;
import com.dsatracker.dsa_tracker.enums.SyncStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_status", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "platform"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SyncStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncStatusEnum status;

    private LocalDateTime lastSyncedAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

}
