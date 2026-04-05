package com.dsatracker.dsa_tracker.model;

import com.dsatracker.dsa_tracker.enums.SyncStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;


@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String codeforcesHandle;
    @Builder.Default
    private boolean codeforcesVerified = false;

    private String leetcodeUsername;
    @Builder.Default
    private boolean leetcodeVerified = false;

    private String codechefUsername;
    @Builder.Default
    private boolean codechefVerified = false;

    // Temporary codes stored during the platform verification flow
    private String codeforcesVerificationCode;
    private String leetcodeVerificationCode;
    private String codechefVerificationCode;

    @Enumerated(EnumType.STRING)
    // EnumType.STRING stores "PENDING" as text. Never use EnumType.ORDINAL —
    // inserting a new enum value in the middle breaks all existing DB rows.
    @Builder.Default
    private SyncStatus syncStatus = SyncStatus.PENDING;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }



    // ── UserDetails interface ──────────────────────────────────────────────

    @Override
    public String getUsername() { return email; }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // No roles yet
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
