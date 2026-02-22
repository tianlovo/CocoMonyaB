package org.xlyo.cocomonyab.plugin;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xlyo.cocomonyab.domain.entity.message.BaseMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.TextMessageEntity;
import org.xlyo.cocomonyab.domain.enums.MessageType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AbstractMessagePlugin.
 * Tests error handling and default behavior.
 * 
 * Validates: Requirements 3.3, 5.4
 */
class AbstractMessagePluginTest {
    
    private TestPlugin plugin;
    private PluginContext context;
    private TextMessageEntity entity;
    
    /**
     * Test plugin implementation for testing purposes
     */
    private static class TestPlugin extends AbstractMessagePlugin {
        private boolean doHandleCalled = false;
        private boolean shouldThrowException = false;
        private PluginResult resultToReturn = PluginResult.CONTINUE;
        
        @Override
        public String getName() {
            return "TestPlugin";
        }
        
        @Override
        public int getPriority() {
            return 10;
        }
        
        @Override
        protected PluginResult doHandle(BaseMessageEntity entity, PluginContext context) {
            doHandleCalled = true;
            if (shouldThrowException) {
                throw new RuntimeException("Test exception");
            }
            return resultToReturn;
        }
        
        public void reset() {
            doHandleCalled = false;
            shouldThrowException = false;
            resultToReturn = PluginResult.CONTINUE;
        }
    }
    
    /**
     * Test plugin that only supports TEXT messages
     */
    private static class TextOnlyPlugin extends AbstractMessagePlugin {
        @Override
        public String getName() {
            return "TextOnlyPlugin";
        }
        
        @Override
        public int getPriority() {
            return 5;
        }
        
        @Override
        protected boolean supports(BaseMessageEntity entity) {
            return entity.getType() == MessageType.TEXT;
        }
        
        @Override
        protected PluginResult doHandle(BaseMessageEntity entity, PluginContext context) {
            return PluginResult.CONTINUE;
        }
    }
    
    @BeforeEach
    void setUp() {
        plugin = new TestPlugin();
        
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.date = 1234567890;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Test", new TdApi.TextEntity[0]),
            null,
            null
        );
        context = new PluginContext(message);
        
        entity = new TextMessageEntity();
        entity.setMessageId(123L);
        entity.setChatId(456L);
        entity.setTextContent("Test message");
    }
    
    @Test
    void isEnabled_DefaultsToTrue() {
        assertTrue(plugin.isEnabled());
    }
    
    @Test
    void setEnabled_ChangesEnabledState() {
        plugin.setEnabled(false);
        assertFalse(plugin.isEnabled());
        
        plugin.setEnabled(true);
        assertTrue(plugin.isEnabled());
    }
    
    @Test
    void handle_CallsDoHandleWhenEnabled() {
        PluginResult result = plugin.handle(entity, context);
        
        assertTrue(plugin.doHandleCalled);
        assertEquals(PluginResult.CONTINUE, result);
    }
    
    @Test
    void handle_SkipsDoHandleWhenDisabled() {
        plugin.setEnabled(false);
        
        PluginResult result = plugin.handle(entity, context);
        
        assertFalse(plugin.doHandleCalled);
        assertEquals(PluginResult.CONTINUE, result);
    }
    
    @Test
    void handle_ReturnsResultFromDoHandle() {
        plugin.resultToReturn = PluginResult.STOP;
        
        PluginResult result = plugin.handle(entity, context);
        
        assertEquals(PluginResult.STOP, result);
    }
    
    @Test
    void handle_CatchesExceptionAndReturnsContinue() {
        plugin.shouldThrowException = true;
        
        // Should not throw exception
        PluginResult result = assertDoesNotThrow(() -> plugin.handle(entity, context));
        
        // Should return CONTINUE to allow next plugin to execute
        assertEquals(PluginResult.CONTINUE, result);
        assertTrue(plugin.doHandleCalled);
    }
    
    @Test
    void handle_LogsExceptionButContinues() {
        plugin.shouldThrowException = true;
        
        // Should handle exception gracefully
        PluginResult result = plugin.handle(entity, context);
        
        assertEquals(PluginResult.CONTINUE, result);
    }
    
    @Test
    void supports_DefaultsToTrue() {
        // Default implementation should support all message types
        assertTrue(plugin.supports(entity));
    }
    
    @Test
    void handle_SkipsDoHandleWhenNotSupported() {
        TextOnlyPlugin textOnlyPlugin = new TextOnlyPlugin();
        
        // Create a non-text entity
        BaseMessageEntity photoEntity = new BaseMessageEntity() {
            @Override
            public MessageType getType() {
                return MessageType.PHOTO;
            }
        };
        photoEntity.setMessageId(123L);
        
        PluginResult result = textOnlyPlugin.handle(photoEntity, context);
        
        // Should skip processing and return CONTINUE
        assertEquals(PluginResult.CONTINUE, result);
    }
    
    @Test
    void handle_ProcessesWhenSupported() {
        TextOnlyPlugin textOnlyPlugin = new TextOnlyPlugin();
        
        PluginResult result = textOnlyPlugin.handle(entity, context);
        
        // Should process the message
        assertEquals(PluginResult.CONTINUE, result);
    }
    
    @Test
    void initialize_DoesNotThrowException() {
        assertDoesNotThrow(() -> plugin.initialize());
    }
    
    @Test
    void destroy_DoesNotThrowException() {
        assertDoesNotThrow(() -> plugin.destroy());
    }
    
    @Test
    void handle_MultipleExceptions_AlwaysReturnsContinue() {
        plugin.shouldThrowException = true;
        
        // Call multiple times
        PluginResult result1 = plugin.handle(entity, context);
        plugin.reset();
        plugin.shouldThrowException = true;
        PluginResult result2 = plugin.handle(entity, context);
        
        assertEquals(PluginResult.CONTINUE, result1);
        assertEquals(PluginResult.CONTINUE, result2);
    }
    
    @Test
    void getName_ReturnsCorrectName() {
        assertEquals("TestPlugin", plugin.getName());
    }
    
    @Test
    void getPriority_ReturnsCorrectPriority() {
        assertEquals(10, plugin.getPriority());
    }
}
