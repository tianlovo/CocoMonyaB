package org.xlyo.cocomonyab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.domain.entity.RawMessage;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MessageService basic query methods.
 * Tests error scenarios and edge cases.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.embedded.storage.directory=data/db/mongo-test",
    "spring.data.mongodb.database=cocomonya"
})
class MessageServiceTest {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private RawMessageRepository rawMessageRepository;
    
    @BeforeEach
    void setUp() {
        // Clean up before each test
        rawMessageRepository.deleteAll();
    }
    
    /**
     * Test: 不存在的MongoDB ID返回DATA_NOT_FOUND错误
     * Validates: Requirements 1.2
     */
    @Test
    void getById_NonExistentId_ThrowsDataNotFound() {
        // Valid MongoDB ID format but doesn't exist
        String nonExistentId = "507f1f77bcf86cd799439011";
        
        BusinessException exception = assertThrows(BusinessException.class,
            () -> messageService.getById(nonExistentId),
            "Should throw BusinessException for non-existent ID");
        
        assertEquals(ResponseCode.DATA_NOT_FOUND.getCode(), exception.getCode(),
            "Should return DATA_NOT_FOUND error code");
        assertTrue(exception.getMessage().contains(nonExistentId),
            "Error message should contain the ID");
    }
    
    /**
     * Test: 无效格式的MongoDB ID返回VALIDATION_ERROR错误
     * Validates: Requirements 1.3
     */
    @Test
    void getById_InvalidIdFormat_ThrowsValidationError() {
        // Invalid MongoDB ID formats
        String[] invalidIds = {
            "invalid",           // Too short
            "12345",             // Too short
            "xxxxxxxxxxxxxxxxxxxxxxxx", // Invalid characters
            "507f1f77bcf86cd79943901g", // Invalid character 'g'
            ""                   // Empty string
        };
        
        for (String invalidId : invalidIds) {
            BusinessException exception = assertThrows(BusinessException.class,
                () -> messageService.getById(invalidId),
                "Should throw BusinessException for invalid ID format: " + invalidId);
            
            assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), exception.getCode(),
                "Should return VALIDATION_ERROR for invalid ID: " + invalidId);
            assertTrue(exception.getMessage().contains(invalidId) || exception.getMessage().contains("无效"),
                "Error message should indicate invalid format");
        }
    }
    
    /**
     * Test: 不存在的ChatId+MessageId返回DATA_NOT_FOUND错误
     * Validates: Requirements 2.2, 2.3
     */
    @Test
    void getByTgId_NonExistentCombination_ThrowsDataNotFound() {
        Long nonExistentChatId = -1001234567890L;
        Long nonExistentMessageId = 999999L;
        
        BusinessException exception = assertThrows(BusinessException.class,
            () -> messageService.getByTgId(nonExistentChatId, nonExistentMessageId),
            "Should throw BusinessException for non-existent chat/message combination");
        
        assertEquals(ResponseCode.DATA_NOT_FOUND.getCode(), exception.getCode(),
            "Should return DATA_NOT_FOUND error code");
        assertTrue(exception.getMessage().contains(nonExistentChatId.toString()),
            "Error message should contain chatId");
        assertTrue(exception.getMessage().contains(nonExistentMessageId.toString()),
            "Error message should contain messageId");
    }
    
    /**
     * Test: getById成功场景 - 验证返回的数据完整性
     * Validates: Requirements 1.1, 1.4
     */
    @Test
    void getById_ExistingMessage_ReturnsCompleteData() {
        // Create and save a test message
        RawMessage message = new RawMessage();
        message.setChatId(-1001234567890L);
        message.setMessageId(123456L);
        message.setMediaAlbumId(789012L);
        message.setDate(1708588800);
        message.setRawJson("{\"test\":\"data\"}");
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        
        RawMessage saved = rawMessageRepository.save(message);
        
        // Query by ID
        var result = messageService.getById(saved.getId());
        
        // Verify all fields
        assertNotNull(result);
        assertEquals(saved.getId(), result.getId());
        assertEquals(message.getChatId(), result.getChatId());
        assertEquals(message.getMessageId(), result.getMessageId());
        assertEquals(message.getMediaAlbumId(), result.getMediaAlbumId());
        assertEquals(message.getDate(), result.getDate());
        assertEquals(message.getRawJson(), result.getRawJson());
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());
    }
    
    /**
     * Test: getByTgId成功场景 - 验证返回的数据完整性
     * Validates: Requirements 2.1
     */
    @Test
    void getByTgId_ExistingMessage_ReturnsCompleteData() {
        // Create and save a test message
        RawMessage message = new RawMessage();
        message.setChatId(-1001234567890L);
        message.setMessageId(123456L);
        message.setDate(1708588800);
        message.setRawJson("{\"test\":\"data\"}");
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        
        rawMessageRepository.save(message);
        
        // Query by ChatId and MessageId
        var result = messageService.getByTgId(message.getChatId(), message.getMessageId());
        
        // Verify all fields
        assertNotNull(result);
        assertEquals(message.getChatId(), result.getChatId());
        assertEquals(message.getMessageId(), result.getMessageId());
        assertEquals(message.getDate(), result.getDate());
        assertEquals(message.getRawJson(), result.getRawJson());
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());
    }
    
    /**
     * Test: 默认分页参数的应用
     * Validates: Requirements 3.2
     */
    @Test
    void page_DefaultParameters_AppliesCorrectly() {
        // Create test messages
        for (int i = 0; i < 15; i++) {
            RawMessage message = new RawMessage();
            message.setChatId(-1001234567890L);
            message.setMessageId(100L + i);
            message.setDate(1708588800 + i * 1000);
            message.setRawJson("{\"test\":\"data\"}");
            message.setCreateTime(LocalDateTime.now());
            message.setUpdateTime(LocalDateTime.now());
            rawMessageRepository.save(message);
        }
        
        // Query with default parameters (current=1, size=10)
        var result = messageService.page(1L, 10L, new org.xlyo.cocomonyab.domain.dto.MessageQueryDTO());
        
        assertNotNull(result);
        assertEquals(0, result.getNumber(), "Page number should be 0 (Spring Data uses 0-based indexing)");
        assertEquals(10, result.getSize(), "Page size should be 10");
        assertEquals(15, result.getTotalElements(), "Total elements should be 15");
        assertEquals(10, result.getContent().size(), "Should return 10 records");
    }
    
    /**
     * Test: 超出范围的页码返回空列表
     * Validates: Requirements 3.3
     */
    @Test
    void page_PageNumberExceedsRange_ReturnsEmptyList() {
        // Create test messages
        for (int i = 0; i < 5; i++) {
            RawMessage message = new RawMessage();
            message.setChatId(-1001234567890L);
            message.setMessageId(100L + i);
            message.setDate(1708588800);
            message.setRawJson("{\"test\":\"data\"}");
            message.setCreateTime(LocalDateTime.now());
            message.setUpdateTime(LocalDateTime.now());
            rawMessageRepository.save(message);
        }
        
        // Query page 10 when only 1 page exists
        var result = messageService.page(10L, 10L, new org.xlyo.cocomonyab.domain.dto.MessageQueryDTO());
        
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty(), "Should return empty list");
        assertEquals(5, result.getTotalElements(), "Total elements should still be 5");
        assertEquals(1, result.getTotalPages(), "Total pages should be 1");
    }
    
    /**
     * Test: 无效的分页参数返回VALIDATION_ERROR错误
     * Validates: Requirements 3.8
     */
    @Test
    void page_InvalidPaginationParameters_ThrowsValidationError() {
        var query = new org.xlyo.cocomonyab.domain.dto.MessageQueryDTO();
        
        // Test current < 1
        BusinessException exception1 = assertThrows(BusinessException.class,
            () -> messageService.page(0L, 10L, query),
            "Should throw exception for current=0");
        assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), exception1.getCode());
        assertTrue(exception1.getMessage().contains("页码"));
        
        // Test current < 0
        BusinessException exception2 = assertThrows(BusinessException.class,
            () -> messageService.page(-1L, 10L, query),
            "Should throw exception for negative current");
        assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), exception2.getCode());
        
        // Test size < 1
        BusinessException exception3 = assertThrows(BusinessException.class,
            () -> messageService.page(1L, 0L, query),
            "Should throw exception for size=0");
        assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), exception3.getCode());
        assertTrue(exception3.getMessage().contains("每页大小"));
        
        // Test size > 100
        BusinessException exception4 = assertThrows(BusinessException.class,
            () -> messageService.page(1L, 101L, query),
            "Should throw exception for size>100");
        assertEquals(ResponseCode.BAD_REQUEST.getCode(), exception4.getCode());
        assertTrue(exception4.getMessage().contains("100"));
    }
}
