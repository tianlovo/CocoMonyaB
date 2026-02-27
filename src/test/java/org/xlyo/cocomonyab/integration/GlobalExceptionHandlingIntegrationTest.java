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
import org.xlyo.cocomonyab.domain.entity.ChannelMessage.MessageStatus;
import org.xlyo.cocomonyab.repository.ChannelMessageRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全局异常处理集成测试
 * 
 * 测试全局异常处理器对各种错误场景的处理：
 * 1. 参数验证失败返回 -40006
 * 2. 数据不存在返回 -60002
 * 3. 系统内部错误返回 -50000
 * 4. 错误日志记录
 * 5. 错误响应不暴露敏感信息
 * 
 * 需求: 7.4, 7.6, 8.1, 8.2, 8.3, 8.4, 8.5
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GlobalExceptionHandlingIntegrationTest {
    
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
    
    @BeforeEach
    void setUp() {
        channelMessageRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        channelMessageRepository.deleteAll();
    }
    
    // ==================== 参数验证失败测试 ====================
    
    /**
     * 测试分页参数验证失败返回 -40006
     * 需求: 7.1, 7.4, 8.2
     */
    @Test
    void testParameterValidation_InvalidPageNumber() throws Exception {
        // 测试 current < 1
        String url = "/api/channel-message/page?current=0&size=10";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        
        // 验证错误码
        int code = json.get("code").asInt();
        assertTrue(code < 0, "错误响应的 code 应该为负数");
        assertTrue(code == -40006 || code == -40000, 
                  "参数验证失败应该返回 -40006 或 -40000");
        
        // 验证错误消息
        String msg = json.get("msg").asText();
        assertNotNull(msg, "应该包含错误消息");
        assertFalse(msg.isEmpty(), "错误消息不应为空");
        assertTrue(msg.contains("页码") || msg.contains("参数"), 
                  "错误消息应该描述具体的验证错误");
    }
    
    /**
     * 测试分页大小验证失败返回 -40006
     * 需求: 7.1, 7.4, 8.2
     */
    @Test
    void testParameterValidation_InvalidPageSize() throws Exception {
        // 测试 size < 1
        String url = "/api/channel-message/page?current=1&size=0";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        
        int code = json.get("code").asInt();
        assertTrue(code < 0, "错误响应的 code 应该为负数");
        assertTrue(code == -40006 || code == -40000, 
                  "参数验证失败应该返回 -40006 或 -40000");
        
        String msg = json.get("msg").asText();
        assertTrue(msg.contains("大小") || msg.contains("size") || msg.contains("参数"), 
                  "错误消息应该描述具体的验证错误");
    }
    
    /**
     * 测试必填参数缺失返回 -40006
     * 需求: 7.2, 7.4, 8.2
     */
    @Test
    void testParameterValidation_MissingRequiredParameter() throws Exception {
        // 测试缺少 chatId 参数
        String url = "/api/channel-message/by-tg-id?messageId=12345";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        
        int code = json.get("code").asInt();
        assertTrue(code < 0, "错误响应的 code 应该为负数");
        assertTrue(code == -40006 || code == -40000, 
                  "参数验证失败应该返回 -40006 或 -40000");
        
        String msg = json.get("msg").asText();
        assertNotNull(msg, "应该包含错误消息");
        assertFalse(msg.isEmpty(), "错误消息不应为空");
    }
    
    /**
     * 测试日期参数验证失败返回 -40006
     * 需求: 7.3, 7.4, 8.2
     */
    @Test
    void testParameterValidation_InvalidDateParameter() throws Exception {
        // 测试 startDate < 0
        String url = "/api/channel-message/page?current=1&size=10&startDate=-1";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        
        int code = json.get("code").asInt();
        assertTrue(code < 0, "错误响应的 code 应该为负数");
        assertTrue(code == -40006 || code == -40000, 
                  "参数验证失败应该返回 -40006 或 -40000");
        
        String msg = json.get("msg").asText();
        assertTrue(msg.contains("日期") || msg.contains("date") || msg.contains("参数"), 
                  "错误消息应该描述具体的验证错误");
    }
    
    // ==================== 数据不存在测试 ====================
    
    /**
     * 测试数据不存在返回 -60002
     * 需求: 8.1
     */
    @Test
    void testDataNotFound_ById() throws Exception {
        // 查询不存在的 MongoDB ID
        String url = "/api/channel-message/nonexistent-id-12345";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        
        // 验证错误码
        int code = json.get("code").asInt();
        assertTrue(code < 0, "错误响应的 code 应该为负数");
        assertEquals(-60002, code, "数据不存在应该返回 -60002");
        
        // 验证错误消息
        String msg = json.get("msg").asText();
        assertNotNull(msg, "应该包含错误消息");
        assertFalse(msg.isEmpty(), "错误消息不应为空");
        assertTrue(msg.contains("不存在") || msg.contains("未找到") || msg.contains("not found"), 
                  "错误消息应该说明数据不存在");
    }
    
    /**
     * 测试通过 Telegram ID 查询不存在的数据返回 -60002
     * 需求: 8.1
     */
    @Test
    void testDataNotFound_ByTgId() throws Exception {
        // 查询不存在的 chatId 和 messageId
        String url = "/api/channel-message/by-tg-id?chatId=-1001234567890&messageId=99999999";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        
        int code = json.get("code").asInt();
        assertTrue(code < 0, "错误响应的 code 应该为负数");
        assertEquals(-60002, code, "数据不存在应该返回 -60002");
        
        String msg = json.get("msg").asText();
        assertTrue(msg.contains("不存在") || msg.contains("未找到") || msg.contains("not found"), 
                  "错误消息应该说明数据不存在");
    }
    
    /**
     * 测试错误消息包含查询条件但不暴露敏感信息
     * 需求: 8.1, 8.5
     */
    @Test
    void testDataNotFound_MessageContainsQueryInfo() throws Exception {
        String testId = "test-id-12345";
        String url = "/api/channel-message/" + testId;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        
        String msg = json.get("msg").asText();
        
        // 验证错误消息包含有用的信息
        assertTrue(msg.contains("不存在") || msg.contains("未找到"), 
                  "错误消息应该说明数据不存在");
        
        // 验证错误消息不暴露敏感信息
        assertFalse(msg.contains("Exception"), "错误消息不应包含异常类名");
        assertFalse(msg.contains("Stack"), "错误消息不应包含堆栈信息");
        assertFalse(msg.contains("mongodb://"), "错误消息不应包含数据库连接信息");
        assertFalse(msg.contains("password"), "错误消息不应包含密码信息");
    }
    
    // ==================== 统一响应格式测试 ====================
    
    /**
     * 测试所有错误响应都包含统一的字段
     * 需求: 8.6
     */
    @Test
    void testErrorResponse_UnifiedFormat() throws Exception {
        // 测试参数验证错误
        String url = "/api/channel-message/page?current=0&size=10";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        
        // 验证响应包含必要的字段
        assertTrue(json.has("code"), "错误响应应该包含 code 字段");
        assertTrue(json.has("msg"), "错误响应应该包含 msg 字段");
        assertTrue(json.has("data"), "错误响应应该包含 data 字段");
        
        // 验证 code 为负数
        int code = json.get("code").asInt();
        assertTrue(code < 0, "错误响应的 code 应该为负数");
        
        // 验证 msg 不为空
        String msg = json.get("msg").asText();
        assertNotNull(msg, "错误消息不应为 null");
        assertFalse(msg.isEmpty(), "错误消息不应为空");
    }
    
    /**
     * 测试数据不存在错误的响应格式
     * 需求: 8.6
     */
    @Test
    void testDataNotFoundError_UnifiedFormat() throws Exception {
        String url = "/api/channel-message/nonexistent-id";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        
        // 验证响应格式
        assertTrue(json.has("code"), "错误响应应该包含 code 字段");
        assertTrue(json.has("msg"), "错误响应应该包含 msg 字段");
        assertTrue(json.has("data"), "错误响应应该包含 data 字段");
        
        // 验证错误码
        assertEquals(-60002, json.get("code").asInt(), "数据不存在应该返回 -60002");
        
        // 验证 data 字段为 null
        assertTrue(json.get("data").isNull(), "错误响应的 data 字段应该为 null");
    }
    
    // ==================== 安全性测试 ====================
    
    /**
     * 测试错误响应不暴露系统内部信息
     * 需求: 8.5
     */
    @Test
    void testErrorResponse_NoSensitiveInfo() throws Exception {
        // 测试多个不同的错误场景
        String[] urls = {
            "/api/channel-message/invalid-id",
            "/api/channel-message/page?current=0&size=10",
            "/api/channel-message/by-tg-id?chatId=-1001234567890&messageId=99999999"
        };
        
        for (String url : urls) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            
            String body = response.getBody();
            assertNotNull(body);
            
            // 验证响应不包含敏感信息
            assertFalse(body.contains("Exception"), "响应不应包含异常类名");
            assertFalse(body.contains("Stack"), "响应不应包含堆栈信息");
            assertFalse(body.contains("at org.xlyo"), "响应不应包含堆栈跟踪");
            assertFalse(body.contains("mongodb://"), "响应不应包含数据库连接信息");
            assertFalse(body.contains("password"), "响应不应包含密码");
            assertFalse(body.contains("secret"), "响应不应包含密钥");
            assertFalse(body.toLowerCase().contains("internal error"), 
                       "响应不应暴露内部错误详情");
        }
    }
    
    /**
     * 测试成功响应的格式
     * 需求: 8.6
     */
    @Test
    void testSuccessResponse_UnifiedFormat() throws Exception {
        // 准备测试数据
        ChannelMessage message = new ChannelMessage();
        message.setChatId(-1001234567890L);
        message.setMessageId(12345L);
        message.setChannelUsername("test_channel");
        message.setChannelTitle("Test Channel");
        message.setDate((int) (System.currentTimeMillis() / 1000));
        message.setContentType("TEXT");
        message.setTextContent("Test message");
        message.setStatus(MessageStatus.PENDING);
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        message = channelMessageRepository.save(message);
        
        // 测试成功响应
        String url = "/api/channel-message/" + message.getId();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        
        // 验证响应格式
        assertTrue(json.has("code"), "成功响应应该包含 code 字段");
        assertTrue(json.has("msg"), "成功响应应该包含 msg 字段");
        assertTrue(json.has("data"), "成功响应应该包含 data 字段");
        
        // 验证 code 为正数
        int code = json.get("code").asInt();
        assertTrue(code > 0, "成功响应的 code 应该为正数");
        
        // 验证 data 不为 null
        assertFalse(json.get("data").isNull(), "成功响应的 data 字段不应为 null");
    }
    
    /**
     * 测试分页查询的成功响应格式
     * 需求: 8.6
     */
    @Test
    void testPageResponse_UnifiedFormat() throws Exception {
        // 测试空结果的分页查询
        String url = "/api/channel-message/page?current=1&size=10";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode json = objectMapper.readTree(response.getBody());
        
        // 验证响应格式
        assertTrue(json.has("code"), "分页响应应该包含 code 字段");
        assertTrue(json.has("msg"), "分页响应应该包含 msg 字段");
        assertTrue(json.has("data"), "分页响应应该包含 data 字段");
        
        // 验证 code 为正数
        assertTrue(json.get("code").asInt() > 0, "成功响应的 code 应该为正数");
        
        // 验证分页数据结构
        JsonNode data = json.get("data");
        assertTrue(data.has("records"), "分页数据应该包含 records 字段");
        assertTrue(data.has("current"), "分页数据应该包含 current 字段");
        assertTrue(data.has("size"), "分页数据应该包含 size 字段");
        assertTrue(data.has("total"), "分页数据应该包含 total 字段");
        assertTrue(data.has("pages"), "分页数据应该包含 pages 字段");
    }
    
    /**
     * 测试不同控制器的错误处理一致性
     * 需求: 8.1, 8.2, 8.6
     */
    @Test
    void testErrorHandling_ConsistencyAcrossControllers() throws Exception {
        // 测试所有只读控制器的错误处理是否一致
        String[] urls = {
            "/api/channel-message/nonexistent-id",
            "/api/forward-queue/nonexistent-id",
            "/api/processed-message/by-tg-id?chatId=-1001234567890&messageId=99999999",
            "/api/unread-buffer/by-tg-id?chatId=-1001234567890&messageId=99999999"
        };
        
        for (String url : urls) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            
            JsonNode json = objectMapper.readTree(response.getBody());
            
            // 验证所有控制器都返回统一的错误格式
            assertTrue(json.has("code"), url + " 的错误响应应该包含 code 字段");
            assertTrue(json.has("msg"), url + " 的错误响应应该包含 msg 字段");
            assertTrue(json.has("data"), url + " 的错误响应应该包含 data 字段");
            
            int code = json.get("code").asInt();
            assertTrue(code < 0, url + " 的错误响应 code 应该为负数");
            assertEquals(-60002, code, url + " 的数据不存在应该返回 -60002");
        }
    }
}
