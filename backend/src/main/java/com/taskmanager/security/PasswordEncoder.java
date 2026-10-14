package com.taskmanager.security;

/**
 * Extension point for future password hashing.
 * Currently stores/compares passwords as plain text per homework requirements.
 */
public interface PasswordEncoder {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String storedPassword);
}
