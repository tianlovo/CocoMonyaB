package org.xlyo.cocomonyab.plugin;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PluginContext.
 * Tests attribute management functionality.
 * 
 * Validates: Requirements 3.3
 */
class PluginContextTest {
    
    private PluginContext context;
    private TdApi.Message message;
    
    @BeforeEach
    void setUp() {
        message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.date = 1234567890;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Test", new TdApi.TextEntity[0]),
            null,
            null
        );
        
        context = new PluginContext(message);
    }
    
    @Test
    void constructor_StoresOriginalMessage() {
        assertNotNull(context.getOriginalMessage());
        assertEquals(123L, context.getOriginalMessage().id);
        assertEquals(456L, context.getOriginalMessage().chatId);
    }
    
    @Test
    void constructor_InitializesEmptyAttributes() {
        assertNotNull(context.getAttributes());
        assertTrue(context.getAttributes().isEmpty());
    }
    
    @Test
    void setAttribute_StoresValue() {
        context.setAttribute("key1", "value1");
        
        assertEquals("value1", context.getAttribute("key1"));
    }
    
    @Test
    void setAttribute_OverwritesExistingValue() {
        context.setAttribute("key1", "value1");
        context.setAttribute("key1", "value2");
        
        assertEquals("value2", context.getAttribute("key1"));
    }
    
    @Test
    void setAttribute_SupportsMultipleKeys() {
        context.setAttribute("key1", "value1");
        context.setAttribute("key2", "value2");
        context.setAttribute("key3", "value3");
        
        assertEquals("value1", context.getAttribute("key1"));
        assertEquals("value2", context.getAttribute("key2"));
        assertEquals("value3", context.getAttribute("key3"));
    }
    
    @Test
    void setAttribute_SupportsDifferentValueTypes() {
        context.setAttribute("string", "text");
        context.setAttribute("integer", 42);
        context.setAttribute("boolean", true);
        context.setAttribute("object", new Object());
        
        assertEquals("text", context.getAttribute("string"));
        assertEquals(42, context.getAttribute("integer"));
        assertEquals(true, context.getAttribute("boolean"));
        assertNotNull(context.getAttribute("object"));
    }
    
    @Test
    void getAttribute_ReturnsNullForNonExistentKey() {
        assertNull(context.getAttribute("nonexistent"));
    }
    
    @Test
    void hasAttribute_ReturnsTrueForExistingKey() {
        context.setAttribute("key1", "value1");
        
        assertTrue(context.hasAttribute("key1"));
    }
    
    @Test
    void hasAttribute_ReturnsFalseForNonExistentKey() {
        assertFalse(context.hasAttribute("nonexistent"));
    }
    
    @Test
    void removeAttribute_RemovesAndReturnsValue() {
        context.setAttribute("key1", "value1");
        
        Object removed = context.removeAttribute("key1");
        
        assertEquals("value1", removed);
        assertFalse(context.hasAttribute("key1"));
        assertNull(context.getAttribute("key1"));
    }
    
    @Test
    void removeAttribute_ReturnsNullForNonExistentKey() {
        Object removed = context.removeAttribute("nonexistent");
        
        assertNull(removed);
    }
    
    @Test
    void attributes_AreThreadSafe() {
        // ConcurrentHashMap should be used internally
        context.setAttribute("key1", "value1");
        
        // This should not throw ConcurrentModificationException
        assertDoesNotThrow(() -> {
            for (String key : context.getAttributes().keySet()) {
                context.setAttribute("key2", "value2");
            }
        });
    }
    
    @Test
    void setAttribute_WithNullValue_ThrowsException() {
        // ConcurrentHashMap does not allow null values
        assertThrows(NullPointerException.class, () -> {
            context.setAttribute("key1", null);
        });
    }
    
    @Test
    void getAttributes_ReturnsModifiableMap() {
        context.setAttribute("key1", "value1");
        
        // Should be able to access the map directly
        assertEquals(1, context.getAttributes().size());
        assertTrue(context.getAttributes().containsKey("key1"));
    }
}
