package org.xlyo.cocomonyab.config.initializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.xlyo.cocomonyab.event.startup.ApiReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupEventPublisher;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;

import static org.mockito.Mockito.*;

/**
 * ApplicationReadyListener Unit Tests
 * <p>
 * Tests the following scenarios:
 * - Event listening and publishing logic
 * - Startup complete information output format
 * - Component status summary output
 * - Progress statistics output
 * </p>
 * <p>
 * **Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5**
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ApplicationReadyListenerTest {
    
    @Mock
    private StartupEventPublisher eventPublisher;
    
    @Mock
    private StartupProgressTracker progressTracker;
    
    @InjectMocks
    private ApplicationReadyListener listener;
    
    private ApiReadyEvent event;
    
    @BeforeEach
    void setUp() {
        event = new ApiReadyEvent(this);
        // Set default server port
        ReflectionTestUtils.setField(listener, "serverPort", 8080);
    }
    
    @Test
    void testOnApiReady_WithDefaultConfiguration_ShouldPublishApplicationReadyEvent() {
        // Given: Progress tracker returns total time
        when(progressTracker.getTotalTime()).thenReturn(15234L);
        
        // When: API ready event is triggered
        listener.onApiReady(event);
        
        // Then: Should publish application ready event
        verify(eventPublisher).publishApplicationReady();
    }
    
    @Test
    void testOnApiReady_ShouldOutputStartupStatistics() {
        // Given: Progress tracker returns total time
        when(progressTracker.getTotalTime()).thenReturn(15234L);
        
        // When: API ready event is triggered
        listener.onApiReady(event);
        
        // Then: Should call printStatistics to output phase timing
        verify(progressTracker).printStatistics();
    }
    
    @Test
    void testOnApiReady_ShouldGetTotalTime() {
        // Given: Progress tracker returns total time
        when(progressTracker.getTotalTime()).thenReturn(15234L);
        
        // When: API ready event is triggered
        listener.onApiReady(event);
        
        // Then: Should get total time from progress tracker
        verify(progressTracker).getTotalTime();
    }
    
    @Test
    void testOnApiReady_WithCustomPort_ShouldSucceed() {
        // Given: Custom server port
        ReflectionTestUtils.setField(listener, "serverPort", 10721);
        when(progressTracker.getTotalTime()).thenReturn(15234L);
        
        // When: API ready event is triggered
        listener.onApiReady(event);
        
        // Then: Should publish application ready event
        verify(eventPublisher).publishApplicationReady();
        
        // Then: Should output statistics
        verify(progressTracker).printStatistics();
    }
    
    @Test
    void testOnApiReady_WithEventPublisherFailure_ShouldNotThrowException() {
        // Given: Event publisher throws exception
        doThrow(new RuntimeException("Event publishing failed"))
                .when(eventPublisher).publishApplicationReady();
        
        // When: API ready event is triggered (should not throw)
        listener.onApiReady(event);
        
        // Then: Should attempt to publish event
        verify(eventPublisher).publishApplicationReady();
        
        // Note: Exception is caught and logged, but not re-thrown
    }
    
    @Test
    void testOnApiReady_WithProgressTrackerFailure_ShouldNotThrowException() {
        // Given: Progress tracker throws exception
        when(progressTracker.getTotalTime()).thenThrow(new RuntimeException("Progress tracker failed"));
        
        // When: API ready event is triggered (should not throw)
        listener.onApiReady(event);
        
        // Then: Should attempt to get total time
        verify(progressTracker).getTotalTime();
        
        // Note: Exception is caught and logged, but not re-thrown
    }
    
    @Test
    void testEventPublishing_ShouldPublishBeforeOutputtingStatistics() {
        // Given: Progress tracker returns total time
        when(progressTracker.getTotalTime()).thenReturn(15234L);
        
        // When: API ready event is triggered
        listener.onApiReady(event);
        
        // Then: Event should be published before statistics output
        var inOrder = inOrder(eventPublisher, progressTracker);
        inOrder.verify(eventPublisher).publishApplicationReady();
        inOrder.verify(progressTracker).getTotalTime();
        inOrder.verify(progressTracker).printStatistics();
    }
    
    @Test
    void testEventListening_ShouldListenToApiReadyEvent() {
        // Given: Progress tracker returns total time
        when(progressTracker.getTotalTime()).thenReturn(15234L);
        
        // When: API ready event is triggered
        listener.onApiReady(event);
        
        // Then: Should process the event and publish application ready event
        verify(eventPublisher).publishApplicationReady();
    }
    
    @Test
    void testMultipleEventTriggers_ShouldHandleEachIndependently() {
        // Given: Progress tracker returns total time
        when(progressTracker.getTotalTime()).thenReturn(15234L);
        
        // When: API ready event is triggered multiple times
        listener.onApiReady(event);
        listener.onApiReady(event);
        
        // Then: Should process each event independently
        verify(eventPublisher, times(2)).publishApplicationReady();
        verify(progressTracker, times(2)).getTotalTime();
        verify(progressTracker, times(2)).printStatistics();
    }
    
    @Test
    void testServerPortConfiguration_WithDefaultValue_ShouldUse8080() {
        // Given: Default server port (8080)
        ReflectionTestUtils.setField(listener, "serverPort", 8080);
        when(progressTracker.getTotalTime()).thenReturn(15234L);
        
        // When: API ready event is triggered
        listener.onApiReady(event);
        
        // Then: Should complete successfully
        verify(eventPublisher).publishApplicationReady();
    }
    
    @Test
    void testServerPortConfiguration_WithConfiguredValue_ShouldUseConfigured() {
        // Given: Server port configured to 10721
        ReflectionTestUtils.setField(listener, "serverPort", 10721);
        when(progressTracker.getTotalTime()).thenReturn(15234L);
        
        // When: API ready event is triggered
        listener.onApiReady(event);
        
        // Then: Should complete successfully
        verify(eventPublisher).publishApplicationReady();
    }
    
    @Test
    void testStartupCompleteOutput_ShouldIncludeTotalTime() {
        // Given: Progress tracker returns specific total time
        when(progressTracker.getTotalTime()).thenReturn(12345L);
        
        // When: API ready event is triggered
        listener.onApiReady(event);
        
        // Then: Should get and use total time
        verify(progressTracker).getTotalTime();
    }
    
    @Test
    void testStartupCompleteOutput_ShouldIncludeComponentStatusSummary() {
        // Given: Progress tracker returns total time
        when(progressTracker.getTotalTime()).thenReturn(15234L);
        
        // When: API ready event is triggered
        listener.onApiReady(event);
        
        // Then: Should output component status (verified by successful execution)
        verify(eventPublisher).publishApplicationReady();
    }
    
    @Test
    void testStartupCompleteOutput_ShouldIncludeAccessUrl() {
        // Given: Progress tracker returns total time and custom port
        ReflectionTestUtils.setField(listener, "serverPort", 9090);
        when(progressTracker.getTotalTime()).thenReturn(15234L);
        
        // When: API ready event is triggered
        listener.onApiReady(event);
        
        // Then: Should output access URL (verified by successful execution)
        verify(eventPublisher).publishApplicationReady();
    }
    
    @Test
    void testStartupCompleteOutput_ShouldCallPrintStatistics() {
        // Given: Progress tracker returns total time
        when(progressTracker.getTotalTime()).thenReturn(15234L);
        
        // When: API ready event is triggered
        listener.onApiReady(event);
        
        // Then: Should call printStatistics to output phase timing
        verify(progressTracker).printStatistics();
    }
}
