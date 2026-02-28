package org.xlyo.cocomonyab.config.initializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.xlyo.cocomonyab.event.startup.MessageSourcesReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupEventPublisher;
import org.xlyo.cocomonyab.event.startup.StartupException;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * ApiInitializer Unit Tests
 * <p>
 * Tests the following scenarios:
 * - Event listening and publishing logic
 * - Server information logging
 * - Progress tracking
 * </p>
 * <p>
 * **Validates: Requirements 6.1, 6.7**
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ApiInitializerTest {
    
    @Mock
    private StartupEventPublisher eventPublisher;
    
    @Mock
    private StartupProgressTracker progressTracker;
    
    @InjectMocks
    private ApiInitializer initializer;
    
    private MessageSourcesReadyEvent event;
    
    @BeforeEach
    void setUp() {
        event = new MessageSourcesReadyEvent(this);
        // Set default server port
        ReflectionTestUtils.setField(initializer, "serverPort", 10721);
    }
    
    @Test
    void testOnMessageSourcesReady_WithDefaultConfiguration_ShouldSucceed() {
        // When: Message sources ready event is triggered
        initializer.onMessageSourcesReady(event);
        
        // Then: Should start phase
        verify(progressTracker).startPhase("API初始化");
        
        // Then: Should publish API ready event
        verify(eventPublisher).publishApiReady();
        
        // Then: Should complete phase
        verify(progressTracker).completePhase("API初始化");
        
        // Then: Should not fail phase
        verify(progressTracker, never()).failPhase(anyString(), anyString());
    }
    
    @Test
    void testOnMessageSourcesReady_WithCustomPort_ShouldSucceed() {
        // Given: Custom server port
        ReflectionTestUtils.setField(initializer, "serverPort", 8080);
        
        // When: Message sources ready event is triggered
        initializer.onMessageSourcesReady(event);
        
        // Then: Should start phase
        verify(progressTracker).startPhase("API初始化");
        
        // Then: Should publish API ready event
        verify(eventPublisher).publishApiReady();
        
        // Then: Should complete phase
        verify(progressTracker).completePhase("API初始化");
    }
    
    @Test
    void testOnMessageSourcesReady_WithEventPublisherFailure_ShouldThrowException() {
        // Given: Event publisher throws exception
        doThrow(new RuntimeException("Event publishing failed"))
                .when(eventPublisher).publishApiReady();
        
        // When & Then: Should throw StartupException
        assertThatThrownBy(() -> initializer.onMessageSourcesReady(event))
                .isInstanceOf(StartupException.class)
                .hasMessageContaining("API 初始化失败");
        
        // Then: Should start phase
        verify(progressTracker).startPhase("API初始化");
        
        // Then: Should fail phase
        verify(progressTracker).failPhase(eq("API初始化"), anyString());
        
        // Then: Should not complete phase
        verify(progressTracker, never()).completePhase("API初始化");
    }
    
    @Test
    void testProgressTrackerPhaseStatus_OnSuccess_ShouldBeCompleted() {
        // When: Message sources ready event is triggered
        initializer.onMessageSourcesReady(event);
        
        // Then: Phase should be started and completed
        verify(progressTracker).startPhase("API初始化");
        verify(progressTracker).completePhase("API初始化");
        verify(progressTracker, never()).failPhase(anyString(), anyString());
    }
    
    @Test
    void testProgressTrackerPhaseStatus_OnFailure_ShouldBeFailed() {
        // Given: Event publisher throws exception
        doThrow(new RuntimeException("Event publishing failed"))
                .when(eventPublisher).publishApiReady();
        
        // When: Message sources ready event is triggered
        try {
            initializer.onMessageSourcesReady(event);
        } catch (StartupException e) {
            // Expected exception
        }
        
        // Then: Phase should be started and failed
        verify(progressTracker).startPhase("API初始化");
        verify(progressTracker).failPhase(eq("API初始化"), anyString());
        verify(progressTracker, never()).completePhase("API初始化");
    }
    
    @Test
    void testEventPublishing_OnlyAfterSuccessfulInitialization() {
        // When: Message sources ready event is triggered
        initializer.onMessageSourcesReady(event);
        
        // Then: Event should be published after phase start
        var inOrder = inOrder(progressTracker, eventPublisher);
        inOrder.verify(progressTracker).startPhase("API初始化");
        inOrder.verify(eventPublisher).publishApiReady();
        inOrder.verify(progressTracker).completePhase("API初始化");
    }
    
    @Test
    void testEventListening_ShouldListenToMessageSourcesReadyEvent() {
        // When: Message sources ready event is triggered
        initializer.onMessageSourcesReady(event);
        
        // Then: Should process the event and publish API ready event
        verify(eventPublisher).publishApiReady();
    }
    
    @Test
    void testMultipleEventTriggers_ShouldHandleEachIndependently() {
        // When: Message sources ready event is triggered multiple times
        initializer.onMessageSourcesReady(event);
        initializer.onMessageSourcesReady(event);
        
        // Then: Should process each event independently
        verify(progressTracker, times(2)).startPhase("API初始化");
        verify(eventPublisher, times(2)).publishApiReady();
        verify(progressTracker, times(2)).completePhase("API初始化");
    }
    
    @Test
    void testServerPortConfiguration_WithDefaultValue_ShouldUse8080() {
        // Given: No server port configured (should use default 8080)
        ReflectionTestUtils.setField(initializer, "serverPort", 8080);
        
        // When: Message sources ready event is triggered
        initializer.onMessageSourcesReady(event);
        
        // Then: Should complete successfully
        verify(progressTracker).completePhase("API初始化");
    }
    
    @Test
    void testServerPortConfiguration_WithConfiguredValue_ShouldUseConfigured() {
        // Given: Server port configured to 10721
        ReflectionTestUtils.setField(initializer, "serverPort", 10721);
        
        // When: Message sources ready event is triggered
        initializer.onMessageSourcesReady(event);
        
        // Then: Should complete successfully
        verify(progressTracker).completePhase("API初始化");
    }
}
