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
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for UnreadMessageBuffer entity.
 * Tests Property 10 from the design document.
 * 
 * Note: Using JUnit @Test with manual property generation instead of jqwik
 * because jqwik doesn't support Spring's dependency injection well.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.embedded.storage.directory=data/db/mongo-test",
    "spring.data.mongodb.database=cocomonya_test"
})
class UnreadMessageBufferPropertyTest {
    
    @Autowired
    private UnreadMessageBufferRepository bufferRepository;
    
    private final Random random = new Random();
    
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
     * Property 10: 缓冲消息数据完整性
     * 
     * For any buffered message, it should contain chatId (Long), messageId (Long),
     * fetchTime (DateTime), and status (String) fields.
     * 
     * Validates: Requirements 4.2, 4.3, 13.1, 13.2, 13.3, 13.4
     */
    @Test
    @Tag("Feature: unread-channel-message-source, Property 10: 缓冲消息数据完整性")
    void bufferMessageDataIntegrity() {
        // Run 100 iterations with random buffer messages
        for (int i = 0; i < 100; i++) {
            // Generate random buffer message data
            Long chatId = generateValidChatId();
            Long messageId = generateValidMessageId();
            LocalDateTime fetchTime = LocalDateTime.now().minusMinutes(random.nextInt(1000));
            BufferStatus status = generateRandomStatus();
            String rawMessage = generateRandomRawMessage();
            String errorMessage = status == BufferStatus.FAILED ? generateRandomErrorMessage() : null;
            
            // Create buffer message
            UnreadMessageBuffer buffer = UnreadMessageBuffer.builder()
                .chatId(chatId)
                .messageId(messageId)
                .fetchTime(fetchTime)
                .status(status)
                .rawMessage(rawMessage)
                .errorMessage(errorMessage)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
            
            // Save to repository
            UnreadMessageBuffer saved = bufferRepository.save(buffer);
            
            // Verify all required fields are present and correct
            assertNotNull(saved, "Saved buffer should not be null (iteration " + i + ")");
            assertNotNull(saved.getId(), 
                "Buffer should have id field (iteration " + i + ")");
            
            // Verify chatId field (Long type)
            assertNotNull(saved.getChatId(), 
                "Buffer should have chatId field (iteration " + i + ")");
            assertTrue(saved.getChatId() instanceof Long, 
                "chatId should be Long type (iteration " + i + ")");
            assertEquals(chatId, saved.getChatId(), 
                "chatId should match original value (iteration " + i + ")");
            
            // Verify messageId field (Long type)
            assertNotNull(saved.getMessageId(), 
                "Buffer should have messageId field (iteration " + i + ")");
            assertTrue(saved.getMessageId() instanceof Long, 
                "messageId should be Long type (iteration " + i + ")");
            assertEquals(messageId, saved.getMessageId(), 
                "messageId should match original value (iteration " + i + ")");
            
            // Verify fetchTime field (DateTime type)
            assertNotNull(saved.getFetchTime(), 
                "Buffer should have fetchTime field (iteration " + i + ")");
            assertTrue(saved.getFetchTime() instanceof LocalDateTime, 
                "fetchTime should be LocalDateTime type (iteration " + i + ")");
            assertEquals(fetchTime, saved.getFetchTime(), 
                "fetchTime should match original value (iteration " + i + ")");
            
            // Verify status field (String/Enum type)
            assertNotNull(saved.getStatus(), 
                "Buffer should have status field (iteration " + i + ")");
            assertTrue(saved.getStatus() instanceof BufferStatus, 
                "status should be BufferStatus enum type (iteration " + i + ")");
            assertEquals(status, saved.getStatus(), 
                "status should match original value (iteration " + i + ")");
            
            // Verify status is one of the valid values
            assertTrue(
                saved.getStatus() == BufferStatus.PENDING ||
                saved.getStatus() == BufferStatus.PROCESSED ||
                saved.getStatus() == BufferStatus.FAILED,
                "status should be one of PENDING, PROCESSED, or FAILED (iteration " + i + ")"
            );
            
            // Verify rawMessage field
            assertNotNull(saved.getRawMessage(), 
                "Buffer should have rawMessage field (iteration " + i + ")");
            assertEquals(rawMessage, saved.getRawMessage(), 
                "rawMessage should match original value (iteration " + i + ")");
            
            // Verify errorMessage field (nullable)
            if (status == BufferStatus.FAILED) {
                assertNotNull(saved.getErrorMessage(), 
                    "Buffer with FAILED status should have errorMessage (iteration " + i + ")");
                assertEquals(errorMessage, saved.getErrorMessage(), 
                    "errorMessage should match original value (iteration " + i + ")");
            }
            
            // Verify createTime and updateTime fields
            assertNotNull(saved.getCreateTime(), 
                "Buffer should have createTime field (iteration " + i + ")");
            assertNotNull(saved.getUpdateTime(), 
                "Buffer should have updateTime field (iteration " + i + ")");
            
            // Retrieve from repository and verify again
            UnreadMessageBuffer retrieved = bufferRepository.findById(saved.getId()).orElse(null);
            assertNotNull(retrieved, "Retrieved buffer should not be null (iteration " + i + ")");
            
            // Verify all fields after retrieval
            assertEquals(saved.getChatId(), retrieved.getChatId(), 
                "Retrieved chatId should match saved (iteration " + i + ")");
            assertEquals(saved.getMessageId(), retrieved.getMessageId(), 
                "Retrieved messageId should match saved (iteration " + i + ")");
            assertEquals(saved.getFetchTime(), retrieved.getFetchTime(), 
                "Retrieved fetchTime should match saved (iteration " + i + ")");
            assertEquals(saved.getStatus(), retrieved.getStatus(), 
                "Retrieved status should match saved (iteration " + i + ")");
            assertEquals(saved.getRawMessage(), retrieved.getRawMessage(), 
                "Retrieved rawMessage should match saved (iteration " + i + ")");
            assertEquals(saved.getErrorMessage(), retrieved.getErrorMessage(), 
                "Retrieved errorMessage should match saved (iteration " + i + ")");
            
            // Cleanup for this iteration
            bufferRepository.deleteById(saved.getId());
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
    
    /**
     * Generates a random error message
     */
    private String generateRandomErrorMessage() {
        String[] errors = {
            "Failed to process message",
            "Network timeout",
            "Invalid message format",
            "Database error",
            "Unknown error"
        };
        return errors[random.nextInt(errors.length)];
    }
}
