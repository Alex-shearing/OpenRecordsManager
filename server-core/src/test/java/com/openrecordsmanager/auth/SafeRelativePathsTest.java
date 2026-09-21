package com.openrecordsmanager.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafeRelativePathsTest {

    @Test
    void acceptsSafeRelativePaths() {
        assertTrue(SafeRelativePaths.isSafe("/"));
        assertTrue(SafeRelativePaths.isSafe("/records"));
        assertTrue(SafeRelativePaths.isSafe("/records/123"));
    }

    @Test
    void rejectsOpenRedirects() {
        assertFalse(SafeRelativePaths.isSafe(null));
        assertFalse(SafeRelativePaths.isSafe(""));
        assertFalse(SafeRelativePaths.isSafe("https://evil.example"));
        assertFalse(SafeRelativePaths.isSafe("//evil.example"));
        assertFalse(SafeRelativePaths.isSafe("/\\evil"));
        assertFalse(SafeRelativePaths.isSafe("%2f%2fevil"));
    }

    @Test
    void orElseFallsBack() {
        assertEquals("/", SafeRelativePaths.orElse("//evil", "/"));
        assertEquals("/home", SafeRelativePaths.orElse("/home", "/"));
    }
}
