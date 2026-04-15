package com.dsatracker.dsa_tracker.service;

import com.dsatracker.dsa_tracker.dto.LoginRequest;
import com.dsatracker.dsa_tracker.dto.LoginResponse;
import com.dsatracker.dsa_tracker.dto.RegisterRequest;
import com.dsatracker.dsa_tracker.exception.UserAlreadyExistsException;
import com.dsatracker.dsa_tracker.model.User;
import com.dsatracker.dsa_tracker.repository.UserRepository;
import com.dsatracker.dsa_tracker.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SyncOrchestrator syncOrchestrator;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "An account with email " + request.getEmail() + " already exists."
            );
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .codeforcesHandle(trimOrNull(request.getCodeforcesHandle()))
                .leetcodeUsername(trimOrNull(request.getLeetcodeUsername()))
                .codechefUsername(trimOrNull(request.getCodechefUsername()))
                .codeforcesVerified(false)
                .leetcodeVerified(false)
                .codechefVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        // Kick off async sync for any platforms the user configured.
        // This returns almost instantly — actual sync runs in background.
        syncOrchestrator.triggerInitialSync(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String token = jwtTokenProvider.generateToken(authentication);

        return new LoginResponse(token, request.getEmail());
    }

    private String trimOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}