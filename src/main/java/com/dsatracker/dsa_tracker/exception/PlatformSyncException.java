package com.dsatracker.dsa_tracker.exception;

// Thrown when CF/LC API fails during sync → maps to 502 Bad Gateway
public class PlatformSyncException extends RuntimeException {
    public PlatformSyncException(String message) { super(message); }
    public PlatformSyncException(String message, Throwable cause) { super(message, cause); }
}
