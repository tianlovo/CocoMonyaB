package org.xlyo.cocomonyab.domain.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.repository.ChannelRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for Channel entity timestamp management.
 * Tests Properties 1 and 2 from the design document.
 * 
 * Note: Using JUnit @Test with manual property generation instead of jqwik
 * because jqwik doesn't support Spring's dependency injection well.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.embedded.storage.directory=data/db/mongo-test",
    "spring.data.mongodb.database=cocomonya"
})
class ChannelTimestampPropertyTest {
    
    @Autowired
    private ChannelRepository channelRepository;
    
    private final Random random = new Random();
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        channelRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after tests
        channelRepository.deleteAll();
    }
    
    /**
     * Property 1: Channel creation timestamp initialization
     * 
     * For any valid channel data, when a channel is created, the resulting channel
     * should have a createTime that is set and within the last few seconds of the current time.
     * 
     * Validates: Requirements 4.8, 5.5
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Property 1: Channel creation timestamp initialization")
    void channelCreationTimestampInitialization() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            Long channelId = generateValidChannelId();
            String channelUsername = generateValidChannelUsername();
            String channelTitle = generateValidChannelTitle();
            Boolean monitoringStatus = random.nextBoolean();
            
            // Record time before creation
            LocalDateTime beforeCreation = LocalDateTime.now();
            
            // Create and save channel
            Channel channel = new Channel();
            channel.setChannelId(channelId);
            channel.setChannelUsername(channelUsername);
            channel.setChannelTitle(channelTitle);
            channel.setMonitoringStatus(monitoringStatus);
            
            Channel savedChannel = channelRepository.save(channel);
            
            // Record time after creation
            LocalDateTime afterCreation = LocalDateTime.now();
            
            // Verify createTime is set
            assertNotNull(savedChannel.getCreateTime(), 
                "createTime should be set after channel creation (iteration " + i + ")");
            
            // Verify createTime is within reasonable time window (10 seconds)
            long secondsBeforeCreation = ChronoUnit.SECONDS.between(
                savedChannel.getCreateTime(), beforeCreation);
            long secondsAfterCreation = ChronoUnit.SECONDS.between(
                savedChannel.getCreateTime(), afterCreation);
            
            assertTrue(secondsBeforeCreation <= 0, 
                "createTime should not be before the creation operation started (iteration " + i + ")");
            assertTrue(secondsAfterCreation >= 0, 
                "createTime should not be after the creation operation completed (iteration " + i + ")");
            assertTrue(Math.abs(secondsAfterCreation) <= 10, 
                "createTime should be within 10 seconds of current time (iteration " + i + ")");
            
            // Verify updateTime is also set
            assertNotNull(savedChannel.getUpdateTime(), 
                "updateTime should be set after channel creation (iteration " + i + ")");
            
            // Cleanup for this iteration
            channelRepository.deleteById(savedChannel.getId());
        }
    }
    
    /**
     * Property 2: Channel update timestamp modification
     * 
     * For any existing channel, when it is updated with any changes, the resulting channel
     * should have an updateTime that is greater than the original updateTime.
     * 
     * Validates: Requirements 4.9, 6.5
     */
    @Test
    @Tag("Feature: mongodb-channel-management, Property 2: Channel update timestamp modification")
    void channelUpdateTimestampModification() throws InterruptedException {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create initial channel
            Long channelId = generateValidChannelId();
            String initialUsername = generateValidChannelUsername();
            String initialTitle = generateValidChannelTitle();
            Boolean initialStatus = random.nextBoolean();
            
            Channel channel = new Channel();
            channel.setChannelId(channelId);
            channel.setChannelUsername(initialUsername);
            channel.setChannelTitle(initialTitle);
            channel.setMonitoringStatus(initialStatus);
            
            Channel savedChannel = channelRepository.save(channel);
            LocalDateTime originalUpdateTime = savedChannel.getUpdateTime();
            
            // Wait a small amount to ensure time difference
            Thread.sleep(10);
            
            // Update the channel with new values
            savedChannel.setChannelUsername(generateValidChannelUsername());
            savedChannel.setChannelTitle(generateValidChannelTitle());
            savedChannel.setMonitoringStatus(!initialStatus);
            
            Channel updatedChannel = channelRepository.save(savedChannel);
            
            // Verify updateTime has been modified
            assertNotNull(updatedChannel.getUpdateTime(), 
                "updateTime should be set after channel update (iteration " + i + ")");
            assertTrue(updatedChannel.getUpdateTime().isAfter(originalUpdateTime),
                "updateTime should be greater than original updateTime after update (iteration " + i + ")");
            
            // Verify createTime remains unchanged
            assertEquals(savedChannel.getCreateTime(), updatedChannel.getCreateTime(),
                "createTime should remain unchanged after update (iteration " + i + ")");
            
            // Cleanup for this iteration
            channelRepository.deleteById(updatedChannel.getId());
        }
    }
    
    /**
     * Generates a valid channel ID (positive Long value)
     */
    private Long generateValidChannelId() {
        return Math.abs(random.nextLong()) + 1;
    }
    
    /**
     * Generates a valid channel username (1-100 characters)
     */
    private String generateValidChannelUsername() {
        int length = random.nextInt(100) + 1; // 1 to 100
        StringBuilder sb = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * Generates a valid channel title (1-200 characters)
     */
    private String generateValidChannelTitle() {
        int length = random.nextInt(200) + 1; // 1 to 200
        StringBuilder sb = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 -_";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
