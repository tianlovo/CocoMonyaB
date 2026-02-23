package org.xlyo.cocomonyab.controller;

import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.xlyo.cocomonyab.domain.entity.RawMessage;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MessageController 属性测试
 * 使用属性测试验证通用属性在所有输入下都成立
 * 
 * Property 9: 响应码正确性
 * Property 10: 参数校验错误信息完整性
 * Property 11: 数据不存在错误信息明确性
 * Property 12: 错误响应安全性
 * 
 * Validates: Requirements 7.3, 7.4, 10.1, 10.2, 10.5
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.embedded.storage.directory=data/db/mongo-test",
    "spring.data.mongodb.database=cocomonya"
})
class MessageControllerPropertyTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private RawMessageRepository rawMessageRepository;
    
    private MockMvc mockMvc;
    
    /**
     * Property 9: 响应码正确性
     * 
     * For any API操作，成功时应该返回code=200，失败时应该返回负数code且data为null
     * 
     * Validates: Requirements 7.3, 7.4
     */
    @Property(tries = 100)
    @Label("Feature: message-query-api, Property 9: 响应码正确性")
    void responseCodeCorrectness(
            @ForAll @LongRange(min = -9999999999999L, max = -1L) Long chatId,
            @ForAll @Positive Long messageId) throws Exception {
        
        setupMockMvc();
        rawMessageRepository.deleteAll();
        
        // 创建并保存消息
        RawMessage message = new RawMessage();
        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setDate(1708588800);
        message.setRawJson("{\"test\":\"data\"}");
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        
        RawMessage saved = rawMessageRepository.save(message);
        
        // 测试成功场景 - 应该返回code=200且data不为null
        mockMvc.perform(get("/api/message/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data").isNotEmpty());
        
        // 测试失败场景 - 不存在的ID应该返回负数code且data不存在
        String nonExistentId = "507f1f77bcf86cd799439011";
        mockMvc.perform(get("/api/message/{id}", nonExistentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.lessThan(0)))
                .andExpect(jsonPath("$.data").doesNotExist());
        
        rawMessageRepository.deleteAll();
    }
    
    /**
     * Property 10: 参数校验错误信息完整性
     * 
     * For any 参数校验失败的请求，响应应该包含详细的校验错误信息，说明哪些参数不符合要求
     * 
     * Validates: Requirements 10.1
     */
    @Property(tries = 100)
    @Label("Feature: message-query-api, Property 10: 参数校验错误信息完整性")
    void validationErrorMessageCompleteness(
            @ForAll @LongRange(min = -100, max = 0) Long invalidCurrent,
            @ForAll @LongRange(min = -100, max = 0) Long invalidSize) throws Exception {
        
        setupMockMvc();
        
        // 测试无效的current参数
        if (invalidCurrent <= 0) {
            mockMvc.perform(get("/api/message/page")
                    .param("current", String.valueOf(invalidCurrent))
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.lessThan(0)))
                    .andExpect(jsonPath("$.msg").exists())
                    .andExpect(jsonPath("$.msg").isString())
                    .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
        
        // 测试无效的size参数
        if (invalidSize <= 0) {
            mockMvc.perform(get("/api/message/page")
                    .param("current", "1")
                    .param("size", String.valueOf(invalidSize)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.lessThan(0)))
                    .andExpect(jsonPath("$.msg").exists())
                    .andExpect(jsonPath("$.msg").isString())
                    .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }
    
    /**
     * Property 11: 数据不存在错误信息明确性
     * 
     * For any 查询不存在数据的请求，错误消息应该包含查询条件信息，帮助定位问题
     * 
     * Validates: Requirements 10.2
     */
    @Property(tries = 100)
    @Label("Feature: message-query-api, Property 11: 数据不存在错误信息明确性")
    void dataNotFoundErrorMessageClarity(
            @ForAll @LongRange(min = -9999999999999L, max = -1L) Long chatId,
            @ForAll @Positive Long messageId) throws Exception {
        
        setupMockMvc();
        rawMessageRepository.deleteAll();
        
        // 查询不存在的ChatId+MessageId组合
        mockMvc.perform(get("/api/message/by-tg-id")
                .param("chatId", String.valueOf(chatId))
                .param("messageId", String.valueOf(messageId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-60002))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString(String.valueOf(chatId))))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString(String.valueOf(messageId))))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
    
    /**
     * Property 12: 错误响应安全性
     * 
     * For any 触发错误的请求，响应中不应该包含敏感的系统信息（如堆栈跟踪、数据库连接信息等）
     * 
     * Validates: Requirements 10.5
     */
    @Property(tries = 100)
    @Label("Feature: message-query-api, Property 12: 错误响应安全性")
    void errorResponseSecurity(@ForAll String randomInvalidId) throws Exception {
        
        setupMockMvc();
        
        // 尝试使用无效ID查询
        String response = mockMvc.perform(get("/api/message/{id}", randomInvalidId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.lessThan(0)))
                .andExpect(jsonPath("$.msg").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // 验证响应中不包含敏感信息
        org.junit.jupiter.api.Assertions.assertFalse(
            response.toLowerCase().contains("exception"),
            "Error response should not contain exception details");
        org.junit.jupiter.api.Assertions.assertFalse(
            response.toLowerCase().contains("stacktrace"),
            "Error response should not contain stack trace");
        org.junit.jupiter.api.Assertions.assertFalse(
            response.toLowerCase().contains("mongodb"),
            "Error response should not contain database information");
        org.junit.jupiter.api.Assertions.assertFalse(
            response.toLowerCase().contains("connection"),
            "Error response should not contain connection information");
        org.junit.jupiter.api.Assertions.assertFalse(
            response.contains("java."),
            "Error response should not contain Java class names");
    }
    
    /**
     * 附加属性测试：分页参数边界验证
     * 
     * 验证分页参数在边界值时的行为
     */
    @Property(tries = 50)
    @Label("Feature: message-query-api, Additional Property: 分页参数边界验证")
    void paginationParameterBoundaryValidation(
            @ForAll @LongRange(min = 1, max = 100) Long validCurrent,
            @ForAll @LongRange(min = 1, max = 100) Long validSize) throws Exception {
        
        setupMockMvc();
        rawMessageRepository.deleteAll();
        
        // 创建一些测试数据
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
        
        // 测试有效的分页参数应该返回成功
        mockMvc.perform(get("/api/message/page")
                .param("current", String.valueOf(validCurrent))
                .param("size", String.valueOf(validSize)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.current").value(validCurrent))
                .andExpect(jsonPath("$.data.size").value(validSize));
        
        rawMessageRepository.deleteAll();
    }
    
    /**
     * 附加属性测试：过滤条件组合验证
     * 
     * 验证多个过滤条件组合时的正确性
     */
    @Property(tries = 50)
    @Label("Feature: message-query-api, Additional Property: 过滤条件组合验证")
    void filterCombinationValidation(
            @ForAll @LongRange(min = -9999999999999L, max = -1L) Long chatId,
            @ForAll @LongRange(min = 1000000000, max = 2000000000) Integer startDate,
            @ForAll @LongRange(min = 1000000000, max = 2000000000) Integer endDate) throws Exception {
        
        setupMockMvc();
        rawMessageRepository.deleteAll();
        
        // 确保startDate <= endDate
        if (startDate > endDate) {
            Integer temp = startDate;
            startDate = endDate;
            endDate = temp;
        }
        
        // 创建测试数据
        RawMessage message = new RawMessage();
        message.setChatId(chatId);
        message.setMessageId(123456L);
        message.setDate(startDate + (endDate - startDate) / 2); // 在范围内
        message.setRawJson("{\"test\":\"data\"}");
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        rawMessageRepository.save(message);
        
        // 测试组合过滤条件
        mockMvc.perform(get("/api/message/page")
                .param("chatId", String.valueOf(chatId))
                .param("startDate", String.valueOf(startDate))
                .param("endDate", String.valueOf(endDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.records").isArray());
        
        rawMessageRepository.deleteAll();
    }
    
    /**
     * 附加属性测试：媒体组查询一致性
     * 
     * 验证媒体组查询返回的所有消息都属于同一媒体组
     */
    @Property(tries = 50)
    @Label("Feature: message-query-api, Additional Property: 媒体组查询一致性")
    void mediaAlbumQueryConsistency(
            @ForAll @LongRange(min = -9999999999999L, max = -1L) Long chatId,
            @ForAll @Positive Long mediaAlbumId) throws Exception {
        
        setupMockMvc();
        rawMessageRepository.deleteAll();
        
        // 创建媒体组消息
        for (int i = 0; i < 3; i++) {
            RawMessage message = new RawMessage();
            message.setChatId(chatId);
            message.setMessageId(100L + i);
            message.setMediaAlbumId(mediaAlbumId);
            message.setDate(1708588800);
            message.setRawJson("{\"test\":\"data\"}");
            message.setCreateTime(LocalDateTime.now());
            message.setUpdateTime(LocalDateTime.now());
            rawMessageRepository.save(message);
        }
        
        // 查询媒体组
        mockMvc.perform(get("/api/message/media-album")
                .param("chatId", String.valueOf(chatId))
                .param("mediaAlbumId", String.valueOf(mediaAlbumId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].mediaAlbumId", 
                    org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(mediaAlbumId.intValue()))));
        
        rawMessageRepository.deleteAll();
    }
    
    // ==================== Helper Methods ====================
    
    private void setupMockMvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }
    }
}
