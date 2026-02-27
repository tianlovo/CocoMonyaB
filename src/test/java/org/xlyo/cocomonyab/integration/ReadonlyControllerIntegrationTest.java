package org.xlyo.cocomonyab.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.xlyo.cocomonyab.domain.entity.ChannelMessage;
import org.xlyo.cocomonyab.domain.entity.ProcessedMessage;
import org.xlyo.cocomonyab.domain.entity.UnreadMessageBuffer;
import org.xlyo.cocomonyab.domain.entity.ChannelMessage.MessageStatus;
import org.xlyo.cocomonyab.domain.enums.BufferStatus;
import org.xlyo.cocomonyab.repository.ChannelMessageRepository;
import org.xlyo.cocomonyab.repository.ProcessedMessageRepository;
import org.xlyo.cocomonyab.repository.UnreadMessageBufferRepository;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 只读控制器集成测试
 * 
 * 测试所有新增 API 端点的完整流程（Controller → Service → Repository → MongoDB）
 * 使用 @SpringBootTest 和 TestRestTemplate 进行端到端测试
 * 使用 Testcontainers 提供真实的 MongoDB 环境
 * 
 * 需求: 2.7, 3.7, 4.7, 5.7
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ReadonlyControllerIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ChannelMessageRepository channelMessageRepository;
    
    @Autowired
    private ProcessedMessageRepository processedMessageRepository;
    
    @Autowired
    private UnreadMessageBufferRepository unreadMessageBufferRepository;
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        channelMessageRepository.deleteAll();
        processedMessageRepository.deleteAll();
        unreadMessageBufferRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试数据
        channelMessageRepository.deleteAll();
        processedMessageRepository.deleteAll();
        unreadMessageBufferRepository.deleteAll();
    }
    
    // ==================== ChannelMessageController 测试 ====================
    
    /**
     * 测试 ChannelMessageController 的完整流程
     * 需求: 2.2, 2.3, 2.4, 2.5, 2.7
     */
    @Test
    void testChannelMessageController_CompleteFlow() throws Exception {
        // 1. 准备测试数据
        ChannelMessage message = createTestChannelMessage(-1001234567890L, 12345L);
        message = channelMessageRepository.save(message);
        
        // 2. 测试根据 MongoDB ID 查询
        String url = "/api/channel-message/" + message.getId();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        assertTrue(json.get("code").asInt() > 0, "成功响应的 code 应该为正数");
        assertEquals("操作成功", json.get("msg").asText());
        assertNotNull(json.get("data"));
        assertEquals(message.getId(), json.get("data").get("id").asText());
        
        // 3. 测试根据 chatId 和 messageId 查询
        url = "/api/channel-message/by-tg-id?chatId=-1001234567890&messageId=12345";
        response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        json = objectMapper.readTree(response.getBody());
        assertTrue(json.get("code").asInt() > 0);
        assertEquals(message.getId(), json.get("data").get("id").asText());
        
        // 4. 测试分页查询
        url = "/api/channel-message/page?current=1&size=10";
        response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        json = objectMapper.readTree(response.getBody());
        assertTrue(json.get("code").asInt() > 0);
        assertNotNull(json.get("data"));
        assertEquals(1, json.get("data").get("records").size());
        assertEquals(1, json.get("data").get("current").asLong());
        assertEquals(10, json.get("data").get("size").asLong());
        assertEquals(1, json.get("data").get("total").asLong());
        
        // 5. 测试媒体组查询
        ChannelMessage message2 = createTestChannelMessage(-1001234567890L, 12346L);
        message2.setMediaAlbumId(999L);
        channelMessageRepository.save(message2);
        
        ChannelMessage message3 = createTestChannelMessage(-1001234567890L, 12347L);
        message3.setMediaAlbumId(999L);
        channelMessageRepository.save(message3);
        
        url = "/api/channel-message/media-album?chatId=-1001234567890&mediaAlbumId=999";
        response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        json = objectMapper.readTree(response.getBody());
        assertTrue(json.get("code").asInt() > 0);
        assertEquals(2, json.get("data").size());
    }
    
    // ==================== ProcessedMessageController 测试 ====================
    
    /**
     * 测试 ProcessedMessageController 的完整流程
     * 需求: 4.2, 4.3, 4.4, 4.5, 4.7
     */
    @Test
    void testProcessedMessageController_CompleteFlow() throws Exception {
        // 1. 准备测试数据
        ProcessedMessage message1 = createTestProcessedMessage(-1001234567890L, 12345L, false, false);
        message1 = processedMessageRepository.save(message1);
        
        ProcessedMessage message2 = createTestProcessedMessage(-1001234567890L, 12346L, false, true);
        processedMessageRepository.save(message2);
        
        ProcessedMessage message3 = createTestProcessedMessage(-1001234567890L, 12347L, true, true);
        processedMessageRepository.save(message3);
        
        // 2. 测试根据 chatId 和 messageId 查询
        String url = "/api/processed-message/by-tg-id?chatId=-1001234567890&messageId=12345";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        assertTrue(json.get("code").asInt() > 0);
        assertEquals(message1.getId(), json.get("data").get("id").asText());
        
        // 3. 测试分页查询
        url = "/api/processed-message/page?current=1&size=10";
        response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        json = objectMapper.readTree(response.getBody());
        assertTrue(json.get("code").asInt() > 0);
        assertEquals(3, json.get("data").get("records").size());
        
        // 4. 测试查询未读消息
        url = "/api/processed-message/unread?current=1&size=10";
        response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        json = objectMapper.readTree(response.getBody());
        assertTrue(json.get("code").asInt() > 0);
        assertEquals(2, json.get("data").get("records").size(), "应该有2条未读消息");
        
        // 5. 测试查询匹配标签的消息
        url = "/api/processed-message/matched?current=1&size=10";
        response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        json = objectMapper.readTree(response.getBody());
        assertTrue(json.get("code").asInt() > 0);
        assertEquals(2, json.get("data").get("records").size(), "应该有2条匹配标签的消息");
    }
    
    // ==================== UnreadMessageBufferController 测试 ====================
    
    /**
     * 测试 UnreadMessageBufferController 的完整流程
     * 需求: 5.2, 5.3, 5.4, 5.5, 5.7
     */
    @Test
    void testUnreadMessageBufferController_CompleteFlow() throws Exception {
        // 1. 准备测试数据
        UnreadMessageBuffer buffer1 = createTestUnreadMessageBuffer(-1001234567890L, 12345L, BufferStatus.PENDING);
        buffer1 = unreadMessageBufferRepository.save(buffer1);
        
        UnreadMessageBuffer buffer2 = createTestUnreadMessageBuffer(-1001234567890L, 12346L, BufferStatus.PROCESSED);
        unreadMessageBufferRepository.save(buffer2);
        
        UnreadMessageBuffer buffer3 = createTestUnreadMessageBuffer(-1001234567890L, 12347L, BufferStatus.FAILED);
        unreadMessageBufferRepository.save(buffer3);
        
        // 2. 测试根据 chatId 和 messageId 查询
        String url = "/api/unread-buffer/by-tg-id?chatId=-1001234567890&messageId=12345";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        assertTrue(json.get("code").asInt() > 0);
        assertEquals(buffer1.getId(), json.get("data").get("id").asText());
        
        // 3. 测试分页查询
        url = "/api/unread-buffer/page?current=1&size=10";
        response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        json = objectMapper.readTree(response.getBody());
        assertTrue(json.get("code").asInt() > 0);
        assertEquals(3, json.get("data").get("records").size());
        
        // 4. 测试查询待处理消息数量
        url = "/api/unread-buffer/pending-count";
        response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        json = objectMapper.readTree(response.getBody());
        assertTrue(json.get("code").asInt() > 0);
        assertEquals(1, json.get("data").asLong(), "应该有1条待处理消息");
        
        // 5. 测试统计查询
        url = "/api/unread-buffer/stats";
        response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        json = objectMapper.readTree(response.getBody());
        assertTrue(json.get("code").asInt() > 0);
        assertNotNull(json.get("data"));
        assertEquals(1, json.get("data").get("pendingCount").asLong());
        assertEquals(1, json.get("data").get("processedCount").asLong());
        assertEquals(1, json.get("data").get("failedCount").asLong());
        assertEquals(3, json.get("data").get("totalCount").asLong());
    }
    
    // ==================== 辅助方法 ====================
    
    private ChannelMessage createTestChannelMessage(Long chatId, Long messageId) {
        ChannelMessage message = new ChannelMessage();
        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setChannelUsername("test_channel");
        message.setChannelTitle("Test Channel");
        message.setDate((int) (System.currentTimeMillis() / 1000));
        message.setContentType("TEXT");
        message.setTextContent("Test message content");
        message.setStatus(MessageStatus.PENDING);
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        return message;
    }
    
    private ProcessedMessage createTestProcessedMessage(Long chatId, Long messageId, Boolean isRead, Boolean isMatched) {
        ProcessedMessage message = new ProcessedMessage();
        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setMessageType("TEXT");
        message.setIsRead(isRead);
        message.setIsMatched(isMatched);
        message.setMatchedTags(isMatched ? new String[]{"tag1"} : new String[]{});
        message.setProcessTime(LocalDateTime.now());
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        return message;
    }
    
    private UnreadMessageBuffer createTestUnreadMessageBuffer(Long chatId, Long messageId, BufferStatus status) {
        UnreadMessageBuffer buffer = new UnreadMessageBuffer();
        buffer.setChatId(chatId);
        buffer.setMessageId(messageId);
        buffer.setStatus(status);
        buffer.setFetchTime(LocalDateTime.now());
        buffer.setCreateTime(LocalDateTime.now());
        buffer.setUpdateTime(LocalDateTime.now());
        return buffer;
    }
}
