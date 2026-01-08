package com.nexaria.launcher.updater;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests d'intégration pour le système de mise à jour
 */
public class UpdateManagerTest {
    
    @Test
    public void testUpdateManagerInitialization() {
        // Test la création d'un UpdateManager
        UpdateManager manager = new UpdateManager("Nexaria/nexaria-launcher", "1.0.0");
        assertNotNull(manager);
    }
    
    @Test
    public void testUpdateCheckResultCreation() {
        // Test la création d'un résultat de vérification
        GitHubUpdater.UpdateCheckResult result = new GitHubUpdater.UpdateCheckResult(
            false,
            null,
            null
        );
        
        assertFalse(result.hasUpdate);
        assertNull(result.release);
        assertNull(result.error);
    }
    
    @Test
    public void testUpdateCheckResultWithUpdate() {
        // Test le résultat avec une mise à jour disponible
        GitHubUpdater.GitHubRelease release = new GitHubUpdater.GitHubRelease(
            "v1.0.5",
            "https://github.com/Nexaria/nexaria-launcher/releases/download/v1.0.5/launcher.jar",
            "Bug fixes"
        );
        
        GitHubUpdater.UpdateCheckResult result = new GitHubUpdater.UpdateCheckResult(
            true,
            release,
            null
        );
        
        assertTrue(result.hasUpdate);
        assertNotNull(result.release);
        assertEquals("v1.0.5", result.release.tagName);
    }
    
    @Test
    public void testUpdateCheckResultWithError() {
        // Test le résultat avec une erreur
        GitHubUpdater.UpdateCheckResult result = new GitHubUpdater.UpdateCheckResult(
            false,
            null,
            "Repo inexistant"
        );
        
        assertFalse(result.hasUpdate);
        assertNull(result.release);
        assertNotNull(result.error);
        assertEquals("Repo inexistant", result.error);
    }
    
    @Test
    public void testUpdateExceptionCreation() {
        // Test la création d'une exception de mise à jour
        GitHubUpdater.UpdateException ex = new GitHubUpdater.UpdateException("Test error");
        assertEquals("Test error", ex.getMessage());
    }
    
    @Test
    public void testUpdateExceptionWithCause() {
        // Test la création d'une exception avec cause
        Exception cause = new Exception("Root cause");
        GitHubUpdater.UpdateException ex = new GitHubUpdater.UpdateException("Test error", cause);
        
        assertEquals("Test error", ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals("Root cause", ex.getCause().getMessage());
    }
    
    @Test
    public void testGitHubReleaseCreation() {
        // Test la création d'une release GitHub
        GitHubUpdater.GitHubRelease release = new GitHubUpdater.GitHubRelease(
            "v1.2.3",
            "https://example.com/launcher.jar",
            "Version 1.2.3 with new features"
        );
        
        assertEquals("v1.2.3", release.tagName);
        assertEquals("https://example.com/launcher.jar", release.downloadUrl);
        assertEquals("Version 1.2.3 with new features", release.changelog);
    }
}
