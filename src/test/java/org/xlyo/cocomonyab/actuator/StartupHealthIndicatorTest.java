package org.xlyo.cocomonyab.actuator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.xlyo.cocomonyab.event.startup.ApplicationReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for StartupHealthIndicator
 * <p>
 * Tests the health check endpoint response format for both ready and not ready states.
 * </p>
 * <p>
 * Requirements: 12.1, 12.2, 12.3, 12.4
 * </p>
 */
@DisplayName("StartupHealthIndicator Unit Tests")
class StartupHealthIndicatorTest {
    
    private StartupHealthIndicator healthIndicator;
    private StartupProgressTracker progressTracker;
    
    @BeforeEach
    void setUp() {
        progressTracker = new StartupProgressTracker();
        healthIndicator = new StartupHealthIndicator(progressTracker);
    }
    
    @Test
    @DisplayName("Should return OUT_OF_SERVICE status when application is not ready")
    void shouldReturnOutOfServiceWhenNotReady() {
        // Given: application is not ready (initial state)
        healthIndicator.updateCurrentPhase("数据库初始化");
        
        // When: health check is performed
        Health health = healthIndicator.health();
        
        // Then: should return OUT_OF_SERVICE status
        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry("ready", false);
        assertThat(health.getDetails()).containsEntry("phase", "数据库初始化");
    }
    
    @Test
    @DisplayName("Should return UP status when application is ready")
    void shouldReturnUpWhenReady() {
        // Given: application startup phases are completed
        progressTracker.startPhase("配置初始化");
        progressTracker.completePhase("配置初始化");
        
        progressTracker.startPhase("数据库初始化");
        progressTracker.completePhase("数据库初始化");
        
        // And: application ready event is fired
        healthIndicator.onApplicationReady(new ApplicationReadyEvent(this));
        
        // When: health check is performed
        Health health = healthIndicator.health();
        
        // Then: should return UP status
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("ready", true);
        assertThat(health.getDetails()).containsEntry("phase", "应用就绪");
        assertThat(health.getDetails()).containsKey("totalTime");
        assertThat(health.getDetails()).containsKey("phases");
    }
    
    @Test
    @DisplayName("Should include phase statistics when ready")
    void shouldIncludePhaseStatisticsWhenReady() {
        // Given: multiple startup phases are completed
        progressTracker.startPhase("配置初始化");
        progressTracker.completePhase("配置初始化");
        
        progressTracker.startPhase("数据库初始化");
        progressTracker.completePhase("数据库初始化");
        
        progressTracker.startPhase("集合初始化");
        progressTracker.completePhase("集合初始化");
        
        // And: application is ready
        healthIndicator.onApplicationReady(new ApplicationReadyEvent(this));
        
        // When: health check is performed
        Health health = healthIndicator.health();
        
        // Then: should include all phase statistics
        assertThat(health.getDetails()).containsKey("phases");
        assertThat(health.getDetails().get("phases")).isInstanceOf(java.util.Map.class);
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> phases = (java.util.Map<String, Object>) health.getDetails().get("phases");
        
        assertThat(phases).containsKeys("配置初始化", "数据库初始化", "集合初始化");
    }
    
    @Test
    @DisplayName("Should update current phase correctly")
    void shouldUpdateCurrentPhaseCorrectly() {
        // Given: initial phase
        healthIndicator.updateCurrentPhase("配置初始化");
        
        // When: health check is performed
        Health health1 = healthIndicator.health();
        
        // Then: should show current phase
        assertThat(health1.getDetails()).containsEntry("phase", "配置初始化");
        
        // When: phase is updated
        healthIndicator.updateCurrentPhase("数据库初始化");
        Health health2 = healthIndicator.health();
        
        // Then: should show updated phase
        assertThat(health2.getDetails()).containsEntry("phase", "数据库初始化");
    }
    
    @Test
    @DisplayName("Should include total time in health details when ready")
    void shouldIncludeTotalTimeWhenReady() {
        // Given: application is ready
        progressTracker.startPhase("配置初始化");
        progressTracker.completePhase("配置初始化");
        healthIndicator.onApplicationReady(new ApplicationReadyEvent(this));
        
        // When: health check is performed
        Health health = healthIndicator.health();
        
        // Then: should include total time
        assertThat(health.getDetails()).containsKey("totalTime");
        assertThat(health.getDetails().get("totalTime")).isInstanceOf(Long.class);
        assertThat((Long) health.getDetails().get("totalTime")).isGreaterThanOrEqualTo(0L);
    }
}
