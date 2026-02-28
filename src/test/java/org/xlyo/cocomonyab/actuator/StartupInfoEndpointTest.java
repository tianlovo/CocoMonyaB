package org.xlyo.cocomonyab.actuator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xlyo.cocomonyab.event.startup.ApplicationReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for StartupInfoEndpoint
 * <p>
 * Tests the startup info endpoint response format.
 * </p>
 * <p>
 * Requirements: 12.5
 * </p>
 */
@DisplayName("StartupInfoEndpoint Unit Tests")
class StartupInfoEndpointTest {
    
    private StartupInfoEndpoint endpoint;
    private StartupProgressTracker progressTracker;
    
    @BeforeEach
    void setUp() {
        progressTracker = new StartupProgressTracker();
        endpoint = new StartupInfoEndpoint(progressTracker);
    }
    
    @Test
    @DisplayName("Should return startup info with STARTING status when not ready")
    void shouldReturnStartingStatusWhenNotReady() {
        // Given: application is starting
        progressTracker.startPhase("配置初始化");
        progressTracker.completePhase("配置初始化");
        
        progressTracker.startPhase("数据库初始化");
        // Not completed yet
        
        // When: startup info is requested
        Map<String, Object> info = endpoint.startupInfo();
        
        // Then: should return STARTING status
        assertThat(info).containsEntry("status", "STARTING");
        assertThat(info).containsKeys("phases", "totalDuration", "status");
    }
    
    @Test
    @DisplayName("Should return startup info with READY status when ready")
    void shouldReturnReadyStatusWhenReady() {
        // Given: application is ready
        progressTracker.startPhase("配置初始化");
        progressTracker.completePhase("配置初始化");
        
        endpoint.onApplicationReady(new ApplicationReadyEvent(this));
        
        // When: startup info is requested
        Map<String, Object> info = endpoint.startupInfo();
        
        // Then: should return READY status
        assertThat(info).containsEntry("status", "READY");
    }
    
    @Test
    @DisplayName("Should include all phase information")
    void shouldIncludeAllPhaseInformation() {
        // Given: multiple phases are completed
        progressTracker.startPhase("配置初始化");
        progressTracker.completePhase("配置初始化");
        
        progressTracker.startPhase("数据库初始化");
        progressTracker.completePhase("数据库初始化");
        
        progressTracker.startPhase("集合初始化");
        progressTracker.completePhase("集合初始化");
        
        // When: startup info is requested
        Map<String, Object> info = endpoint.startupInfo();
        
        // Then: should include all phases
        assertThat(info).containsKey("phases");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> phases = (List<Map<String, Object>>) info.get("phases");
        
        assertThat(phases).hasSize(3);
        assertThat(phases).allMatch(phase -> 
            phase.containsKey("name") &&
            phase.containsKey("status") &&
            phase.containsKey("startTime") &&
            phase.containsKey("endTime") &&
            phase.containsKey("duration")
        );
    }
    
    @Test
    @DisplayName("Should include phase details with correct structure")
    void shouldIncludePhaseDetailsWithCorrectStructure() {
        // Given: a phase is completed
        progressTracker.startPhase("配置初始化");
        progressTracker.completePhase("配置初始化");
        
        // When: startup info is requested
        Map<String, Object> info = endpoint.startupInfo();
        
        // Then: phase should have correct structure
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> phases = (List<Map<String, Object>>) info.get("phases");
        
        Map<String, Object> phase = phases.get(0);
        assertThat(phase).containsEntry("name", "配置初始化");
        assertThat(phase).containsEntry("status", "COMPLETED");
        assertThat(phase.get("startTime")).isInstanceOf(Long.class);
        assertThat(phase.get("endTime")).isInstanceOf(Long.class);
        assertThat(phase.get("duration")).isInstanceOf(Long.class);
    }
    
    @Test
    @DisplayName("Should include error message for failed phases")
    void shouldIncludeErrorMessageForFailedPhases() {
        // Given: a phase has failed
        progressTracker.startPhase("数据库初始化");
        progressTracker.failPhase("数据库初始化", "Connection timeout");
        
        // When: startup info is requested
        Map<String, Object> info = endpoint.startupInfo();
        
        // Then: should include error message
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> phases = (List<Map<String, Object>>) info.get("phases");
        
        Map<String, Object> phase = phases.get(0);
        assertThat(phase).containsEntry("status", "FAILED");
        assertThat(phase).containsEntry("errorMessage", "Connection timeout");
    }
    
    @Test
    @DisplayName("Should include total duration")
    void shouldIncludeTotalDuration() {
        // Given: some phases are completed
        progressTracker.startPhase("配置初始化");
        progressTracker.completePhase("配置初始化");
        
        // When: startup info is requested
        Map<String, Object> info = endpoint.startupInfo();
        
        // Then: should include total duration
        assertThat(info).containsKey("totalDuration");
        assertThat(info.get("totalDuration")).isInstanceOf(Long.class);
        assertThat((Long) info.get("totalDuration")).isGreaterThanOrEqualTo(0L);
    }
    
    @Test
    @DisplayName("Should return empty phases list when no phases started")
    void shouldReturnEmptyPhasesListWhenNoPhases() {
        // Given: no phases have started
        
        // When: startup info is requested
        Map<String, Object> info = endpoint.startupInfo();
        
        // Then: should return empty phases list
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> phases = (List<Map<String, Object>>) info.get("phases");
        
        assertThat(phases).isEmpty();
        assertThat(info).containsEntry("status", "STARTING");
    }
}
