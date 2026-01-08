package com.nexaria.launcher.updater;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests unitaires pour le système de mise à jour
 */
public class GitHubUpdaterTest {
    
    private GitHubUpdater updater;
    
    @Before
    public void setUp() {
        // Utilise un repo de test (change selon tes besoins)
        updater = new GitHubUpdater("Nexaria/nexaria-launcher", "1.0.0");
    }
    
    @Test
    public void testUpdateCheckResultStructure() {
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
    public void testUpdateCheckResultWithValues() {
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
        assertNull(result.error);
    }
    
    @Test
    public void testGitHubReleaseStructure() {
        GitHubUpdater.GitHubRelease release = new GitHubUpdater.GitHubRelease(
            "v1.0.5",
            "https://github.com/Nexaria/nexaria-launcher/releases/download/v1.0.5/launcher.jar",
            "Bug fixes and improvements"
        );
        
        assertEquals("v1.0.5", release.tagName);
        assertTrue(release.downloadUrl.contains("launcher.jar"));
        assertEquals("Bug fixes and improvements", release.changelog);
    }
    
    @Test
    public void testUpdateCheckResultWithError() {
        GitHubUpdater.UpdateCheckResult result = new GitHubUpdater.UpdateCheckResult(
            false,
            null,
            "Repo GitHub inexistant"
        );
        
        assertFalse(result.hasUpdate);
        assertNull(result.release);
        assertEquals("Repo GitHub inexistant", result.error);
    }
}
