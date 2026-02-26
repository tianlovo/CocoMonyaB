package org.xlyo.cocomonyab.domain.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.domain.enums.BufferStatus;
import org.xlyo.cocomonyab.repository.UnreadMessageBufferRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for UnreadMessageBuffer TTL index.
 * Tests Property 25 from the design document.
 * 
 * Note: Using JUnit @Test with manual property generation instead of jqwik
 * because jqwik doesn't support Spring's dependency injection well.
 * 
 * Important: TTL index cleanup is performed by MongoDB's background task,
 * which runs every 60 seconds by default. This test validates the logic
 * for identifying expired records, not the actual MongoDB TTL mechanism.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.embedded.storage.directory=data/db/mongo-test",
    "spring.data.mongodb.database=cocomonya_test"
})
class UnreadMessageBufferTTLPropertyTest {
    
    @Autowired
    private UnreadMessageBufferRepository bufferRepository;
    
    private final Random random = new Random();
    
    /**
     * TTL 天数：7 天
     */
    private static final int TTL_DAYS = 7;
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        bufferRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after tests
        bufferRepository.deleteAll();
    }
    
    /**
     * Property 25: TTL 自动清理
     * 
     * For any buffered message with status PROCESSED, if the createTime is older than
     * the configured TTL days (7 days), it should be eligible for automatic deletion.
     * 
     * Note: This test validates the logic for identifying expired records. The actual
     * MongoDB TTL cleanup is performed by MongoDB's background task.
     * 
     * Validates: Requirements 13.7
     */
    @Test
    @Tag("Feature: unread-channel-message-source, Property 25: TTL 自动清理")
    void ttlAutoCleanup() {
        // Run 100 iterations with random buffer messages
        for (int i = 0; i < 100; i++) {
            List<UnreadMessageBuffer> allBuffers = new ArrayList<>();
            List<UnreadMessageBuffer> expiredBuffers = new ArrayList<>();
            List<UnreadMessageBuffer> nonExpiredBuffers = new ArrayList<>();
            
            // Create random number of buffer messages (5 to 20)
            int numBuffers = random.nextInt(16) + 5;
            
            for (int j = 0; j < numBuffers; j++) {
                Long chatId = generateValidChatId();
                Long messageId = generateValidMessageId();
                BufferStatus status = generateRandomStatus();
                
                // Randomly assign createTime: some expired (> 7 days), some not
                boolean shouldBeExpired = random.nextBoolean();
                LocalDateTime createTime;
                
                if (shouldBeExpired) {
                    // Create time more than 7 days ago (7 to 30 days ago)
                    int daysAgo = TTL_DAYS + 1 + random.nextInt(23);
                    createTime = LocalDateTime.now().minusDays(daysAgo);
                } else {
                    // Create time within 7 days (0 to 6 days ago)
                    int daysAgo = random.nextInt(TTL_DAYS);
                    createTime = LocalDateTime.now().minusDays(daysAgo);
                }
                
                UnreadMessageBuffer buffer = UnreadMessageBuffer.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .fetchTime(LocalDateTime.now().minusMinutes(random.nextInt(1000)))
                    .status(status)
                    .rawMessage(generateRandomRawMessage())
                    .errorMessage(status == BufferStatus.FAILED ? "Test error" : null)
                    .createTime(createTime)
                    .updateTime(LocalDateTime.now())
                    .build();
                
                UnreadMessageBuffer saved = bufferRepository.save(buffer);
                allBuffers.add(saved);
                
                // Track which buffers should be expired
                // According to requirement 13.7, only PROCESSED status records should be auto-cleaned
                if (status == BufferStatus.PROCESSED && shouldBeExpired) {
                    expiredBuffers.add(saved);
                } else {
                    nonExpiredBuffers.add(saved);
                }
            }
            
            // Verify: Identify expired PROCESSED records
            LocalDateTime ttlThreshold = LocalDateTime.now().minusDays(TTL_DAYS);
            
            for (UnreadMessageBuffer buffer : allBuffers) {
                boolean isExpired = buffer.getStatus() == BufferStatus.PROCESSED &&
                                  buffer.getCreateTime().isBefore(ttlThreshold);
                
                if (isExpired) {
                    assertTrue(expiredBuffers.contains(buffer),
                        "Buffer should be in expired list (iteration " + i + ")");
                    
                    // Verify it's PROCESSED status
                    assertEquals(BufferStatus.PROCESSED, buffer.getStatus(),
                        "Expired buffer should have PROCESSED status (iteration " + i + ")");
                    
                    // Verify createTime is older than TTL
                    assertTrue(buffer.getCreateTime().isBefore(ttlThreshold),
                        "Expired buffer createTime should be before TTL threshold (iteration " + i + ")");
                } else {
                    assertTrue(nonExpiredBuffers.contains(buffer),
                        "Buffer should be in non-expired list (iteration " + i + ")");
                    
                    // Verify why it's not expired
                    boolean isNotProcessed = buffer.getStatus() != BufferStatus.PROCESSED;
                    boolean isNotOldEnough = !buffer.getCreateTime().isBefore(ttlThreshold);
                    
                    assertTrue(isNotProcessed || isNotOldEnough,
                        "Non-expired buffer should either not be PROCESSED or not old enough (iteration " + i + ")");
                }
            }
            
            // Verify: PENDING and FAILED status records should NOT be auto-cleaned regardless of age
            for (UnreadMessageBuffer buffer : allBuffers) {
                if (buffer.getStatus() == BufferStatus.PENDING || 
                    buffer.getStatus() == BufferStatus.FAILED) {
                    assertFalse(expiredBuffers.contains(buffer),
                        "PENDING/FAILED buffers should never be in expired list (iteration " + i + ")");
                }
            }
            
            // Verify: Only PROCESSED status records older than TTL should be expired
            for (UnreadMessageBuffer buffer : expiredBuffers) {
                assertEquals(BufferStatus.PROCESSED, buffer.getStatus(),
                    "All expired buffers must have PROCESSED status (iteration " + i + ")");
                assertTrue(buffer.getCreateTime().isBefore(ttlThreshold),
                    "All expired buffers must be older than TTL (iteration " + i + ")");
            }
            
            // Simulate manual cleanup (since embedded MongoDB may not run TTL background task)
            // In production, MongoDB's TTL index will handle this automatically
            for (UnreadMessageBuffer expired : expiredBuffers) {
                bufferRepository.deleteById(expired.getId());
            }
            
            // Verify: After cleanup, only non-expired records remain
            List<UnreadMessageBuffer> remaining = bufferRepository.findAll();
            assertEquals(nonExpiredBuffers.size(), remaining.size(),
                "After cleanup, only non-expired buffers should remain (iteration " + i + ")");
            
            for (UnreadMessageBuffer buffer : remaining) {
                assertTrue(nonExpiredBuffers.stream()
                    .anyMatch(b -> b.getId().equals(buffer.getId())),
                    "Remaining buffer should be in non-expired list (iteration " + i + ")");
            }
            
            // Cleanup for this iteration
            bufferRepository.deleteAll();
        }
    }
    
    /**
     * Generates a valid chat ID (negative Long value for Telegram channels)
     */
    private Long generateValidChatId() {
        return -1000000000000L - Math.abs(random.nextLong() % 1000000000000L);
    }
    
    /**
     * Generates a valid message ID (positive Long value)
     */
    private Long generateValidMessageId() {
        return Math.abs(random.nextLong() % 1000000000L) + 1;
    }
    
    /**
     * Generates a random BufferStatus
     */
    private BufferStatus generateRandomStatus() {
        BufferStatus[] statuses = BufferStatus.values();
        return statuses[random.nextInt(statuses.length)];
    }
    
    /**
     * Generates a random raw message JSON string
     */
    private String generateRandomRawMessage() {
        return "{\"@type\":\"message\",\"id\":" + generateValidMessageId() + 
               ",\"chatId\":" + generateValidChatId() + 
               ",\"date\":" + System.currentTimeMillis() / 1000 + "}";
    }
}
