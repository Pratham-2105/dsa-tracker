package com.dsatracker.dsa_tracker.exception;

// Thrown when a problem ID / user ID doesn't exist → maps to 404
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}