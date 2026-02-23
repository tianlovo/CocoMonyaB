package org.xlyo.cocomonyab.event;

import org.junit.jupiter.api.Test;
import org.xlyo.cocomonyab.event.TagFilterConfigEvent.EventType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TagFilterConfigEvent event structure.
 * Tests event type enumeration completeness and static factory methods.
 * 
 * Validates: Requirements 5.1, 5.2
 */
class TagFilterConfigEventTest {
    
    private static final Object TEST_SOURCE = new Object();
    private static final Long TEST_CHANNEL_ID = -1001234567890L;
    private static final String TEST_CONFIG_ID = "65f8a1b2c3d4e5f6a7b8c9d0";
    private static final Boolean TEST_ENABLED = true;
    
    /**
     * Test that all required event types are defined in the EventType enum.
     * 
     * Validates: Requirement 5.2 - The Filter_Event SHALL support event types: 
     * CONFIG_CREATED, CONFIG_UPDATED, CONFIG_DELETED, RELOAD_ALL
     */
    @Test
    void testEventTypeEnumCompleteness() {
        // Verify all required event types exist
        EventType[] eventTypes = EventType.values();
        
        assertEquals(4, eventTypes.length, 
            "EventType enum should have exactly 4 values");
        
        // Verify each required event type exists
        assertNotNull(EventType.valueOf("CONFIG_CREATED"), 
            "CONFIG_CREATED event type should exist");
        assertNotNull(EventType.valueOf("CONFIG_UPDATED"), 
            "CONFIG_UPDATED event type should exist");
        assertNotNull(EventType.valueOf("CONFIG_DELETED"), 
            "CONFIG_DELETED event type should exist");
        assertNotNull(EventType.valueOf("RELOAD_ALL"), 
            "RELOAD_ALL event type should exist");
    }
    
    /**
     * Test the configCreated static factory method.
     * 
     * Validates: Requirement 5.1 - The System SHALL define Filter_Event with 
     * event type, channelId, and configuration details
     */
    @Test
    void testConfigCreatedFactoryMethod() {
        TagFilterConfigEvent event = TagFilterConfigEvent.configCreated(
            TEST_SOURCE, TEST_CHANNEL_ID, TEST_CONFIG_ID, TEST_ENABLED);
        
        assertNotNull(event, "Event should not be null");
        assertEquals(EventType.CONFIG_CREATED, event.getEventType(), 
            "Event type should be CONFIG_CREATED");
        assertEquals(TEST_CHANNEL_ID, event.getChannelId(), 
            "Channel ID should match");
        assertEquals(TEST_CONFIG_ID, event.getConfigId(), 
            "Config ID should match");
        assertEquals(TEST_ENABLED, event.getEnabled(), 
            "Enabled status should match");
        assertEquals(TEST_SOURCE, event.getSource(), 
            "Event source should match");
    }
    
    /**
     * Test the configCreated factory method with null channelId (global config).
     */
    @Test
    void testConfigCreatedWithNullChannelId() {
        TagFilterConfigEvent event = TagFilterConfigEvent.configCreated(
            TEST_SOURCE, null, TEST_CONFIG_ID, TEST_ENABLED);
        
        assertNotNull(event, "Event should not be null");
        assertEquals(EventType.CONFIG_CREATED, event.getEventType(), 
            "Event type should be CONFIG_CREATED");
        assertNull(event.getChannelId(), 
            "Channel ID should be null for global config");
        assertEquals(TEST_CONFIG_ID, event.getConfigId(), 
            "Config ID should match");
        assertEquals(TEST_ENABLED, event.getEnabled(), 
            "Enabled status should match");
    }
    
    /**
     * Test the configUpdated static factory method.
     * 
     * Validates: Requirement 5.1 - The System SHALL define Filter_Event with 
     * event type, channelId, and configuration details
     */
    @Test
    void testConfigUpdatedFactoryMethod() {
        TagFilterConfigEvent event = TagFilterConfigEvent.configUpdated(
            TEST_SOURCE, TEST_CHANNEL_ID, TEST_CONFIG_ID, TEST_ENABLED);
        
        assertNotNull(event, "Event should not be null");
        assertEquals(EventType.CONFIG_UPDATED, event.getEventType(), 
            "Event type should be CONFIG_UPDATED");
        assertEquals(TEST_CHANNEL_ID, event.getChannelId(), 
            "Channel ID should match");
        assertEquals(TEST_CONFIG_ID, event.getConfigId(), 
            "Config ID should match");
        assertEquals(TEST_ENABLED, event.getEnabled(), 
            "Enabled status should match");
        assertEquals(TEST_SOURCE, event.getSource(), 
            "Event source should match");
    }
    
    /**
     * Test the configUpdated factory method with null channelId (global config).
     */
    @Test
    void testConfigUpdatedWithNullChannelId() {
        TagFilterConfigEvent event = TagFilterConfigEvent.configUpdated(
            TEST_SOURCE, null, TEST_CONFIG_ID, TEST_ENABLED);
        
        assertNotNull(event, "Event should not be null");
        assertEquals(EventType.CONFIG_UPDATED, event.getEventType(), 
            "Event type should be CONFIG_UPDATED");
        assertNull(event.getChannelId(), 
            "Channel ID should be null for global config");
        assertEquals(TEST_CONFIG_ID, event.getConfigId(), 
            "Config ID should match");
        assertEquals(TEST_ENABLED, event.getEnabled(), 
            "Enabled status should match");
    }
    
    /**
     * Test the configDeleted static factory method.
     * 
     * Validates: Requirement 5.1 - The System SHALL define Filter_Event with 
     * event type, channelId, and configuration details
     */
    @Test
    void testConfigDeletedFactoryMethod() {
        TagFilterConfigEvent event = TagFilterConfigEvent.configDeleted(
            TEST_SOURCE, TEST_CHANNEL_ID, TEST_CONFIG_ID);
        
        assertNotNull(event, "Event should not be null");
        assertEquals(EventType.CONFIG_DELETED, event.getEventType(), 
            "Event type should be CONFIG_DELETED");
        assertEquals(TEST_CHANNEL_ID, event.getChannelId(), 
            "Channel ID should match");
        assertEquals(TEST_CONFIG_ID, event.getConfigId(), 
            "Config ID should match");
        assertNull(event.getEnabled(), 
            "Enabled status should be null for deleted event");
        assertEquals(TEST_SOURCE, event.getSource(), 
            "Event source should match");
    }
    
    /**
     * Test the configDeleted factory method with null channelId (global config).
     */
    @Test
    void testConfigDeletedWithNullChannelId() {
        TagFilterConfigEvent event = TagFilterConfigEvent.configDeleted(
            TEST_SOURCE, null, TEST_CONFIG_ID);
        
        assertNotNull(event, "Event should not be null");
        assertEquals(EventType.CONFIG_DELETED, event.getEventType(), 
            "Event type should be CONFIG_DELETED");
        assertNull(event.getChannelId(), 
            "Channel ID should be null for global config");
        assertEquals(TEST_CONFIG_ID, event.getConfigId(), 
            "Config ID should match");
        assertNull(event.getEnabled(), 
            "Enabled status should be null for deleted event");
    }
    
    /**
     * Test the reloadAll static factory method.
     * 
     * Validates: Requirement 5.1 - The System SHALL define Filter_Event with 
     * event type, channelId, and configuration details
     */
    @Test
    void testReloadAllFactoryMethod() {
        TagFilterConfigEvent event = TagFilterConfigEvent.reloadAll(TEST_SOURCE);
        
        assertNotNull(event, "Event should not be null");
        assertEquals(EventType.RELOAD_ALL, event.getEventType(), 
            "Event type should be RELOAD_ALL");
        assertNull(event.getChannelId(), 
            "Channel ID should be null for reload all event");
        assertNull(event.getConfigId(), 
            "Config ID should be null for reload all event");
        assertNull(event.getEnabled(), 
            "Enabled status should be null for reload all event");
        assertEquals(TEST_SOURCE, event.getSource(), 
            "Event source should match");
    }
    
    /**
     * Test the constructor directly to ensure all fields are properly set.
     */
    @Test
    void testConstructor() {
        TagFilterConfigEvent event = new TagFilterConfigEvent(
            TEST_SOURCE, EventType.CONFIG_CREATED, 
            TEST_CHANNEL_ID, TEST_CONFIG_ID, TEST_ENABLED);
        
        assertNotNull(event, "Event should not be null");
        assertEquals(EventType.CONFIG_CREATED, event.getEventType(), 
            "Event type should match");
        assertEquals(TEST_CHANNEL_ID, event.getChannelId(), 
            "Channel ID should match");
        assertEquals(TEST_CONFIG_ID, event.getConfigId(), 
            "Config ID should match");
        assertEquals(TEST_ENABLED, event.getEnabled(), 
            "Enabled status should match");
        assertEquals(TEST_SOURCE, event.getSource(), 
            "Event source should match");
    }
    
    /**
     * Test the toString method for proper formatting.
     */
    @Test
    void testToString() {
        TagFilterConfigEvent event = TagFilterConfigEvent.configCreated(
            TEST_SOURCE, TEST_CHANNEL_ID, TEST_CONFIG_ID, TEST_ENABLED);
        
        String result = event.toString();
        
        assertNotNull(result, "toString should not return null");
        assertTrue(result.contains("CONFIG_CREATED"), 
            "toString should contain event type");
        assertTrue(result.contains(TEST_CHANNEL_ID.toString()), 
            "toString should contain channel ID");
        assertTrue(result.contains(TEST_CONFIG_ID), 
            "toString should contain config ID");
        assertTrue(result.contains(TEST_ENABLED.toString()), 
            "toString should contain enabled status");
    }
    
    /**
     * Test toString with null values.
     */
    @Test
    void testToStringWithNullValues() {
        TagFilterConfigEvent event = TagFilterConfigEvent.reloadAll(TEST_SOURCE);
        
        String result = event.toString();
        
        assertNotNull(result, "toString should not return null");
        assertTrue(result.contains("RELOAD_ALL"), 
            "toString should contain event type");
        assertTrue(result.contains("null"), 
            "toString should handle null values");
    }
    
    /**
     * Test that events with different enabled states are created correctly.
     */
    @Test
    void testDifferentEnabledStates() {
        // Test with enabled = true
        TagFilterConfigEvent enabledEvent = TagFilterConfigEvent.configCreated(
            TEST_SOURCE, TEST_CHANNEL_ID, TEST_CONFIG_ID, true);
        assertTrue(enabledEvent.getEnabled(), 
            "Enabled should be true");
        
        // Test with enabled = false
        TagFilterConfigEvent disabledEvent = TagFilterConfigEvent.configCreated(
            TEST_SOURCE, TEST_CHANNEL_ID, TEST_CONFIG_ID, false);
        assertFalse(disabledEvent.getEnabled(), 
            "Enabled should be false");
        
        // Test with enabled = null (for deleted event)
        TagFilterConfigEvent deletedEvent = TagFilterConfigEvent.configDeleted(
            TEST_SOURCE, TEST_CHANNEL_ID, TEST_CONFIG_ID);
        assertNull(deletedEvent.getEnabled(), 
            "Enabled should be null for deleted event");
    }
}
