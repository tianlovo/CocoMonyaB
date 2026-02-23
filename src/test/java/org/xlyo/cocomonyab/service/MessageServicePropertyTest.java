package org.xlyo.cocomonyab.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.domain.dto.MessageQueryDTO;
import org.xlyo.cocomonyab.domain.entity.RawMessage;
import org.xlyo.cocomonyab.domain.vo.MessageVO;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import java.time.LocalDateTime;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for MessageService basic query methods.
 * Tests Property 1 from the design document.
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
class MessageServicePropertyTest {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private RawMessageRepository rawMessageRepository;
    
    private final Random random = new Random();
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        rawMessageRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after tests
        rawMessageRepository.deleteAll();
    }
    
    /**
     * Property 1: MongoDB ID查询往返一致性
     * 
     * For any valid RawMessage entity, after saving it to the database and querying by its MongoDB ID,
     * the returned MessageVO should contain all fields with equivalent values.
     * 
     * Validates: Requirements 1.1, 1.4
     */
    @Test
    @Tag("Feature: message-query-api, Property 1: MongoDB ID查询往返一致性")
    void mongoIdQueryRoundTrip() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Generate random valid message data
            Long chatId = generateValidChatId();
            Long messageId = generateValidMessageId();
            Long mediaAlbumId = random.nextBoolean() ? generateValidMediaAlbumId() : null;
            Integer date = generateValidDate();
            String rawJson = generateValidRawJson();
            LocalDateTime now = LocalDateTime.now();
            
            // Create and save message entity
            RawMessage message = new RawMessage();
            message.setChatId(chatId);
            message.setMessageId(messageId);
            message.setMediaAlbumId(mediaAlbumId);
            message.setDate(date);
            message.setRawJson(rawJson);
            message.setCreateTime(now);
            message.setUpdateTime(now);
            
            RawMessage savedMessage = rawMessageRepository.save(message);
            assertNotNull(savedMessage.getId(), "Saved message should have an ID (iteration " + i + ")");
            
            // Query by MongoDB ID
            MessageVO retrievedVO = messageService.getById(savedMessage.getId());
            assertNotNull(retrievedVO, "Retrieved message should not be null (iteration " + i + ")");
            
            // Verify round-trip: all fields should match
            assertEquals(savedMessage.getId(), retrievedVO.getId(),
                "Retrieved id should match saved message (iteration " + i + ")");
            assertEquals(chatId, retrievedVO.getChatId(),
                "Retrieved chatId should match original (iteration " + i + ")");
            assertEquals(messageId, retrievedVO.getMessageId(),
                "Retrieved messageId should match original (iteration " + i + ")");
            assertEquals(mediaAlbumId, retrievedVO.getMediaAlbumId(),
                "Retrieved mediaAlbumId should match original (iteration " + i + ")");
            assertEquals(date, retrievedVO.getDate(),
                "Retrieved date should match original (iteration " + i + ")");
            assertEquals(rawJson, retrievedVO.getRawJson(),
                "Retrieved rawJson should match original (iteration " + i + ")");
            assertNotNull(retrievedVO.getCreateTime(),
                "Retrieved createTime should not be null (iteration " + i + ")");
            assertNotNull(retrievedVO.getUpdateTime(),
                "Retrieved updateTime should not be null (iteration " + i + ")");
            
            // Verify VO completeness (all required fields present)
            assertNotNull(retrievedVO.getId(), 
                "Retrieved message must have id field (iteration " + i + ")");
            assertNotNull(retrievedVO.getChatId(), 
                "Retrieved message must have chatId field (iteration " + i + ")");
            assertNotNull(retrievedVO.getMessageId(), 
                "Retrieved message must have messageId field (iteration " + i + ")");
            assertNotNull(retrievedVO.getDate(), 
                "Retrieved message must have date field (iteration " + i + ")");
            assertNotNull(retrievedVO.getRawJson(), 
                "Retrieved message must have rawJson field (iteration " + i + ")");
            
            // Cleanup for this iteration
            rawMessageRepository.deleteById(savedMessage.getId());
        }
    }
    
    /**
     * Generates a valid chat ID (negative Long value for Telegram channels)
     */
    private Long generateValidChatId() {
        return -Math.abs(random.nextLong() % 1000000000000L) - 1L;
    }
    
    /**
     * Generates a valid message ID (positive Long value)
     */
    private Long generateValidMessageId() {
        return Math.abs(random.nextLong() % 1000000000L) + 1L;
    }
    
    /**
     * Generates a valid media album ID (positive Long value)
     */
    private Long generateValidMediaAlbumId() {
        return Math.abs(random.nextLong() % 1000000L) + 1L;
    }
    
    /**
     * Generates a valid date (Unix timestamp between 2020 and 2030)
     */
    private Integer generateValidDate() {
        // Unix timestamp range: 2020-01-01 to 2030-12-31
        int minDate = 1577836800; // 2020-01-01
        int maxDate = 1924905600; // 2030-12-31
        return minDate + random.nextInt(maxDate - minDate);
    }
    
    /**
     * Generates a valid raw JSON string (10-1000 characters)
     */
    private String generateValidRawJson() {
        int length = random.nextInt(991) + 10; // 10 to 1000
        StringBuilder sb = new StringBuilder("{\"message\":\"");
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 ";
        for (int i = 0; i < length - 20; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        sb.append("\"}");
        return sb.toString();
    }
    
    /**
     * Property 3: 分页查询正确性
     * 
     * For any valid pagination parameters (current, size) and message collection,
     * the pagination query should return correct pagination structure (records, current, size, total, pages),
     * and when page number exceeds range, return empty records but maintain correct metadata.
     * 
     * Validates: Requirements 3.1, 3.3
     */
    @Test
    @Tag("Feature: message-query-api, Property 3: 分页查询正确性")
    void paginationCorrectness() {
        // Create test messages
        for (int i = 0; i < 25; i++) {
            RawMessage message = new RawMessage();
            message.setChatId(generateValidChatId());
            message.setMessageId(generateValidMessageId() + i);
            message.setDate(generateValidDate());
            message.setRawJson(generateValidRawJson());
            message.setCreateTime(LocalDateTime.now());
            message.setUpdateTime(LocalDateTime.now());
            rawMessageRepository.save(message);
        }
        
        // Test various pagination scenarios
        for (int iteration = 0; iteration < 20; iteration++) {
            Long current = (long) (random.nextInt(5) + 1); // 1-5
            Long size = (long) (random.nextInt(10) + 1);   // 1-10
            
            Page<MessageVO> result = messageService.page(current, size, new MessageQueryDTO());
            
            assertNotNull(result, "Result should not be null (iteration " + iteration + ")");
            assertEquals(current.intValue() - 1, result.getNumber(),
                "Current page number should match (iteration " + iteration + ")");
            assertEquals(size.intValue(), result.getSize(),
                "Page size should match (iteration " + iteration + ")");
            assertEquals(25, result.getTotalElements(),
                "Total elements should be 25 (iteration " + iteration + ")");
            
            // Verify records count
            if (current <= result.getTotalPages()) {
                assertTrue(result.getContent().size() <= size,
                    "Records count should not exceed page size (iteration " + iteration + ")");
            } else {
                assertTrue(result.getContent().isEmpty(),
                    "Records should be empty when page exceeds range (iteration " + iteration + ")");
            }
        }
        
        // Cleanup
        rawMessageRepository.deleteAll();
    }
    
    /**
     * Property 4: 过滤器正确性
     * 
     * For any query filter conditions (chatId, startDate, endDate, mediaAlbumId),
     * all returned messages should satisfy the specified filter conditions.
     * 
     * Validates: Requirements 3.4, 3.5, 3.6
     */
    @Test
    @Tag("Feature: message-query-api, Property 4: 过滤器正确性")
    void filterCorrectness() {
        // Create messages with different chatIds and dates
        Long targetChatId = -1001234567890L;
        Integer targetStartDate = 1708588800;
        Integer targetEndDate = 1708675200;
        
        // Messages that match filter
        for (int i = 0; i < 10; i++) {
            RawMessage message = new RawMessage();
            message.setChatId(targetChatId);
            message.setMessageId(generateValidMessageId() + i);
            message.setDate(targetStartDate + i * 1000);
            message.setRawJson(generateValidRawJson());
            message.setCreateTime(LocalDateTime.now());
            message.setUpdateTime(LocalDateTime.now());
            rawMessageRepository.save(message);
        }
        
        // Messages that don't match filter
        for (int i = 0; i < 5; i++) {
            RawMessage message = new RawMessage();
            message.setChatId(generateValidChatId());
            message.setMessageId(generateValidMessageId() + i + 100);
            message.setDate(targetEndDate + 10000);
            message.setRawJson(generateValidRawJson());
            message.setCreateTime(LocalDateTime.now());
            message.setUpdateTime(LocalDateTime.now());
            rawMessageRepository.save(message);
        }
        
        // Test chatId filter
        MessageQueryDTO chatIdQuery = new MessageQueryDTO();
        chatIdQuery.setChatId(targetChatId);
        
        Page<MessageVO> chatIdResult = messageService.page(1L, 20L, chatIdQuery);
        for (var msg : chatIdResult.getContent()) {
            assertEquals(targetChatId, msg.getChatId(),
                "All messages should have the target chatId");
        }
        
        // Test date range filter
        MessageQueryDTO dateQuery = new MessageQueryDTO();
        dateQuery.setStartDate(targetStartDate);
        dateQuery.setEndDate(targetEndDate);
        
        Page<MessageVO> dateResult = messageService.page(1L, 20L, dateQuery);
        for (var msg : dateResult.getContent()) {
            assertTrue(msg.getDate() >= targetStartDate && msg.getDate() <= targetEndDate,
                "All messages should be within date range");
        }
        
        // Test combined filter
        MessageQueryDTO combinedQuery = new MessageQueryDTO();
        combinedQuery.setChatId(targetChatId);
        combinedQuery.setStartDate(targetStartDate);
        combinedQuery.setEndDate(targetEndDate);
        
        Page<MessageVO> combinedResult = messageService.page(1L, 20L, combinedQuery);
        for (var msg : combinedResult.getContent()) {
            assertEquals(targetChatId, msg.getChatId(),
                "All messages should have the target chatId");
            assertTrue(msg.getDate() >= targetStartDate && msg.getDate() <= targetEndDate,
                "All messages should be within date range");
        }
        
        // Cleanup
        rawMessageRepository.deleteAll();
    }
    
    /**
     * Property 5: 消息列表排序正确性
     * 
     * For any pagination query result, the returned message list should be sorted
     * by date field in descending order (newest messages first).
     * 
     * Validates: Requirements 3.7
     */
    @Test
    @Tag("Feature: message-query-api, Property 5: 消息列表排序正确性")
    void messageSortingCorrectness() {
        // Create messages with different dates
        for (int i = 0; i < 20; i++) {
            RawMessage message = new RawMessage();
            message.setChatId(generateValidChatId());
            message.setMessageId(generateValidMessageId() + i);
            message.setDate(1708588800 + i * 1000); // Incrementing dates
            message.setRawJson(generateValidRawJson());
            message.setCreateTime(LocalDateTime.now());
            message.setUpdateTime(LocalDateTime.now());
            rawMessageRepository.save(message);
        }
        
        // Query messages
        Page<MessageVO> result = messageService.page(1L, 20L, new MessageQueryDTO());
        
        // Verify descending order
        var messages = result.getContent();
        for (int i = 0; i < messages.size() - 1; i++) {
            assertTrue(messages.get(i).getDate() >= messages.get(i + 1).getDate(),
                "Messages should be sorted by date in descending order");
        }
        
        // Cleanup
        rawMessageRepository.deleteAll();
    }
    
    /**
     * Property 8: 分页大小限制
     * 
     * For any pagination request, when size parameter exceeds maximum limit (100),
     * the system should return validation error.
     * 
     * Validates: Requirements 9.3
     */
    @Test
    @Tag("Feature: message-query-api, Property 8: 分页大小限制")
    void pageSizeLimitEnforcement() {
        // Test size exceeding limit
        Long[] invalidSizes = {101L, 200L, 1000L, Long.MAX_VALUE};
        
        for (Long invalidSize : invalidSizes) {
            var exception = assertThrows(org.xlyo.cocomonyab.common.exception.BusinessException.class,
                () -> messageService.page(1L, invalidSize, new MessageQueryDTO()),
                "Should throw exception for size > 100: " + invalidSize);
            
            assertTrue(exception.getCode() < 0,
                "Error code should be negative for size: " + invalidSize);
            assertTrue(exception.getMessage().contains("100") || exception.getMessage().contains("超过"),
                "Error message should mention the limit for size: " + invalidSize);
        }
        
        // Test valid sizes
        Long[] validSizes = {1L, 10L, 50L, 100L};
        
        for (Long validSize : validSizes) {
            assertDoesNotThrow(
                () -> messageService.page(1L, validSize, new MessageQueryDTO()),
                "Should not throw exception for valid size: " + validSize);
        }
    }
}
