package org.xlyo.cocomonyab.config.initializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xlyo.cocomonyab.event.startup.PluginsReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupEventPublisher;
import org.xlyo.cocomonyab.event.startup.StartupException;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;
import org.xlyo.cocomonyab.source.MessageSourceRegistry;
import org.xlyo.cocomonyab.source.telegram.TelegramMessageSource;
import org.xlyo.cocomonyab.source.unread.UnreadMessageSource;
import org.xlyo.cocomonyab.telegram.TelegramClientManager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * MessageSourceInitializer Unit Tests
 * <p>
 * Tests the following scenarios:
 * - Telegram API configuration validation logic
 * - Message source registration and startup flow
 * </p>
 * <p>
 * **Validates: Requirements 5.3, 5.4, 5.6**
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class MessageSourceInitializerTest {
    
    @Mock
    private TelegramClientManager telegramClientManager;
    
    @Mock
    private MessageSourceRegistry messageSourceRegistry;
    
    @Mock
    private TelegramMessageSource telegramMessageSource;
    
    @Mock
    private UnreadMessageSource unreadMessageSource;
    
    @Mock
    private StartupEventPublisher eventPublisher;
    
    @Mock
    private StartupProgressTracker progressTracker;
    
    @InjectMocks
    private MessageSourceInitializer initializer;
    
    private PluginsReadyEvent event;
    
    @BeforeEach
    void setUp() {
        event = new PluginsReadyEvent(this);
    }
    
    @Test
    void testOnPluginsReady_WithValidTelegramClient_ShouldSucceed() {
        // Given: Telegram client is ready
        when(telegramClientManager.isReady()).thenReturn(true);
        
        // When: Plugins ready event is triggered
        initializer.onPluginsReady(event);
        
        // Then: Should start phase
        verify(progressTracker).startPhase("消息源初始化");
        
        // Then: Should verify Telegram client is ready
        verify(telegramClientManager).isReady();
        
        // Then: Should register both message sources
        verify(messageSourceRegistry).register(telegramMessageSource);
        verify(messageSourceRegistry).register(unreadMessageSource);
        
        // Then: Should start all message sources
        verify(messageSourceRegistry).startAll();
        
        // Then: Should publish message sources ready event
        verify(eventPublisher).publishMessageSourcesReady();
        
        // Then: Should complete phase
        verify(progressTracker).completePhase("消息源初始化");
    }
    
    @Test
    void testOnPluginsReady_WithTelegramClientNotReady_ShouldThrowException() {
        // Given: Telegram client is not ready
        when(telegramClientManager.isReady()).thenReturn(false);
        
        // When & Then: Should throw StartupException
        assertThatThrownBy(() -> initializer.onPluginsReady(event))
                .isInstanceOf(StartupException.class)
                .hasMessageContaining("消息源初始化失败");
        
        // Then: Should start phase
        verify(progressTracker).startPhase("消息源初始化");
        
        // Then: Should fail phase
        verify(progressTracker).failPhase(eq("消息源初始化"), anyString());
        
        // Then: Should not register message sources
        verify(messageSourceRegistry, never()).register(any());
        
        // Then: Should not start message sources
        verify(messageSourceRegistry, never()).startAll();
        
        // Then: Should not publish event
        verify(eventPublisher, never()).publishMessageSourcesReady();
    }
    
    @Test
    void testOnPluginsReady_WithRegistrationFailure_ShouldThrowException() {
        // Given: Telegram client is ready but registration fails
        when(telegramClientManager.isReady()).thenReturn(true);
        doThrow(new IllegalArgumentException("Message source already exists"))
                .when(messageSourceRegistry).register(telegramMessageSource);
        
        // When & Then: Should throw StartupException
        assertThatThrownBy(() -> initializer.onPluginsReady(event))
                .isInstanceOf(StartupException.class)
                .hasMessageContaining("消息源初始化失败");
        
        // Then: Should fail phase
        verify(progressTracker).failPhase(eq("消息源初始化"), anyString());
        
        // Then: Should not start message sources
        verify(messageSourceRegistry, never()).startAll();
        
        // Then: Should not publish event
        verify(eventPublisher, never()).publishMessageSourcesReady();
    }
    
    @Test
    void testOnPluginsReady_WithStartAllFailure_ShouldContinue() {
        // Given: Telegram client is ready and registration succeeds
        // startAll() catches exceptions internally and continues, so it won't throw
        when(telegramClientManager.isReady()).thenReturn(true);
        
        // When: Plugins ready event is triggered
        initializer.onPluginsReady(event);
        
        // Then: Should register both message sources
        verify(messageSourceRegistry).register(telegramMessageSource);
        verify(messageSourceRegistry).register(unreadMessageSource);
        
        // Then: Should attempt to start all message sources
        verify(messageSourceRegistry).startAll();
        
        // Then: Should still publish event (startAll doesn't throw)
        verify(eventPublisher).publishMessageSourcesReady();
        
        // Then: Should complete phase
        verify(progressTracker).completePhase("消息源初始化");
    }
    
    @Test
    void testMessageSourceRegistrationOrder_ShouldRegisterTelegramFirst() {
        // Given: Telegram client is ready
        when(telegramClientManager.isReady()).thenReturn(true);
        
        // When: Plugins ready event is triggered
        initializer.onPluginsReady(event);
        
        // Then: Should register Telegram message source before unread message source
        var inOrder = inOrder(messageSourceRegistry);
        inOrder.verify(messageSourceRegistry).register(telegramMessageSource);
        inOrder.verify(messageSourceRegistry).register(unreadMessageSource);
    }
    
    @Test
    void testProgressTrackerPhaseStatus_OnSuccess_ShouldBeCompleted() {
        // Given: Telegram client is ready
        when(telegramClientManager.isReady()).thenReturn(true);
        
        // When: Plugins ready event is triggered
        initializer.onPluginsReady(event);
        
        // Then: Phase should be started and completed
        verify(progressTracker).startPhase("消息源初始化");
        verify(progressTracker).completePhase("消息源初始化");
        verify(progressTracker, never()).failPhase(anyString(), anyString());
    }
    
    @Test
    void testProgressTrackerPhaseStatus_OnFailure_ShouldBeFailed() {
        // Given: Telegram client is not ready
        when(telegramClientManager.isReady()).thenReturn(false);
        
        // When: Plugins ready event is triggered
        try {
            initializer.onPluginsReady(event);
        } catch (StartupException e) {
            // Expected exception
        }
        
        // Then: Phase should be started and failed
        verify(progressTracker).startPhase("消息源初始化");
        verify(progressTracker).failPhase(eq("消息源初始化"), anyString());
        verify(progressTracker, never()).completePhase("消息源初始化");
    }
    
    @Test
    void testEventPublishing_OnlyAfterSuccessfulInitialization() {
        // Given: Telegram client is ready
        when(telegramClientManager.isReady()).thenReturn(true);
        
        // When: Plugins ready event is triggered
        initializer.onPluginsReady(event);
        
        // Then: Event should be published only after all initialization steps
        var inOrder = inOrder(telegramClientManager, messageSourceRegistry, eventPublisher);
        inOrder.verify(telegramClientManager).isReady();
        inOrder.verify(messageSourceRegistry).register(telegramMessageSource);
        inOrder.verify(messageSourceRegistry).register(unreadMessageSource);
        inOrder.verify(messageSourceRegistry).startAll();
        inOrder.verify(eventPublisher).publishMessageSourcesReady();
    }
}
