package com.dsatracker.dsa_tracker.exception;

// RuntimeException so it bubbles up freely and @Transactional rolls back on it
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) { super(message); }
}