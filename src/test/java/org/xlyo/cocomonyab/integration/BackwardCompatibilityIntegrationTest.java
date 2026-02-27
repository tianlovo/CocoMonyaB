package org.xlyo.cocomonyab.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 向后兼容性集成测试
 * 
 * 测试重构后的系统保持向后兼容性：
 * 1. MessageController 的所有端点仍然可访问
 * 2. ChannelController 未受影响
 * 3. tag 子包下的所有控制器未受影响
 * 4. 所有现有的单元测试和集成测试通过
 * 
 * 需求: 9.1, 9.2, 9.3, 9.4
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class BackwardCompatibilityIntegrationTest {
    
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
    private ApplicationContext applicationContext;
    
    // ==================== MessageController 向后兼容性测试 ====================
    
    /**
     * 测试 MessageController 的所有端点仍然可访问
     * 需求: 9.1
     */
    @Test
    void testMessageController_AllEndpointsAccessible() {
        // 测试所有 MessageController 端点返回正确的响应格式（即使数据为空）
        
        // 1. GET /api/message/{id} - 应该返回404或正确的错误格式
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/message/nonexistent-id", 
            String.class
        );
        assertNotNull(response);
        assertTrue(response.getStatusCode() == HttpStatus.OK || 
                   response.getStatusCode() == HttpStatus.NOT_FOUND,
                   "端点应该可访问");
        
        // 2. GET /api/message/by-tg-id - 应该返回正确的响应格式
        response = restTemplate.getForEntity(
            "/api/message/by-tg-id?chatId=-1001234567890&messageId=12345", 
            String.class
        );
        assertNotNull(response);
        assertTrue(response.getStatusCode() == HttpStatus.OK || 
                   response.getStatusCode() == HttpStatus.NOT_FOUND,
                   "端点应该可访问");
        
        // 3. GET /api/message/page - 应该返回分页响应
        response = restTemplate.getForEntity(
            "/api/message/page?current=1&size=10", 
            String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode(), 
                    "分页端点应该可访问并返回200");
        
        // 4. GET /api/message/media-album - 应该返回正确的响应格式
        response = restTemplate.getForEntity(
            "/api/message/media-album?chatId=-1001234567890&mediaAlbumId=999", 
            String.class
        );
        assertNotNull(response);
        assertTrue(response.getStatusCode() == HttpStatus.OK || 
                   response.getStatusCode() == HttpStatus.NOT_FOUND,
                   "端点应该可访问");
    }
    
    /**
     * 测试 MessageController 的 API 路径未改变
     * 需求: 9.1
     */
    @Test
    void testMessageController_ApiPathUnchanged() throws Exception {
        // 验证 MessageController 仍然映射到 /api/message
        Object messageController = applicationContext.getBean("messageController");
        assertNotNull(messageController, "MessageController 应该存在");
        
        Class<?> controllerClass = messageController.getClass();
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping, "MessageController 应该有 @RequestMapping 注解");
        
        String[] paths = requestMapping.value();
        assertTrue(paths.length > 0, "应该有至少一个路径映射");
        assertEquals("/api/message", paths[0], "API 路径应该保持为 /api/message");
    }
    
    /**
     * 测试 MessageController 的响应格式未改变
     * 需求: 9.1
     */
    @Test
    void testMessageController_ResponseFormatUnchanged() throws Exception {
        // 测试分页查询的响应格式
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/message/page?current=1&size=10", 
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        JsonNode json = objectMapper.readTree(response.getBody());
        
        // 验证响应包含必要的字段
        assertTrue(json.has("code"), "响应应该包含 code 字段");
        assertTrue(json.has("msg"), "响应应该包含 msg 字段");
        assertTrue(json.has("data"), "响应应该包含 data 字段");
        
        // 验证分页响应的数据结构
        JsonNode data = json.get("data");
        assertTrue(data.has("records"), "分页数据应该包含 records 字段");
        assertTrue(data.has("current"), "分页数据应该包含 current 字段");
        assertTrue(data.has("size"), "分页数据应该包含 size 字段");
        assertTrue(data.has("total"), "分页数据应该包含 total 字段");
        assertTrue(data.has("pages"), "分页数据应该包含 pages 字段");
    }
    
    // ==================== ChannelController 未受影响测试 ====================
    
    /**
     * 测试 ChannelController 未受影响
     * 需求: 9.2
     */
    @Test
    void testChannelController_Unaffected() {
        // 验证 ChannelController 仍然存在于原位置
        Object channelController = applicationContext.getBean("channelController");
        assertNotNull(channelController, "ChannelController 应该存在");
        
        // 验证 ChannelController 的包路径未改变
        String packageName = channelController.getClass().getPackage().getName();
        assertEquals("org.xlyo.cocomonyab.controller", packageName,
                    "ChannelController 应该仍在 org.xlyo.cocomonyab.controller 包中");
        
        // 验证 ChannelController 仍然是 RestController
        RestController restController = channelController.getClass().getAnnotation(RestController.class);
        assertNotNull(restController, "ChannelController 应该仍然是 @RestController");
    }
    
    // ==================== Tag 子包控制器未受影响测试 ====================
    
    /**
     * 测试 tag 子包下的所有控制器未受影响
     * 需求: 9.3
     */
    @Test
    void testTagControllers_Unaffected() {
        // 定义 tag 子包下的所有控制器
        List<String> tagControllers = Arrays.asList(
            "authorController",
            "workController",
            "characterController"
        );
        
        for (String controllerName : tagControllers) {
            // 验证控制器存在
            Object controller = applicationContext.getBean(controllerName);
            assertNotNull(controller, controllerName + " 应该存在");
            
            // 验证控制器的包路径未改变
            String packageName = controller.getClass().getPackage().getName();
            assertEquals("org.xlyo.cocomonyab.controller.tag", packageName,
                        controllerName + " 应该仍在 org.xlyo.cocomonyab.controller.tag 包中");
            
            // 验证控制器仍然是 RestController
            RestController restController = controller.getClass().getAnnotation(RestController.class);
            assertNotNull(restController, controllerName + " 应该仍然是 @RestController");
        }
    }
    
    /**
     * 测试 AuthorController 的端点仍然可访问
     * 需求: 9.3
     */
    @Test
    void testAuthorController_EndpointsAccessible() {
        // 测试 AuthorController 的分页查询端点
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/author/page?current=1&size=10", 
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                    "AuthorController 的端点应该可访问");
    }
    
    /**
     * 测试 WorkController 的端点仍然可访问
     * 需求: 9.3
     */
    @Test
    void testWorkController_EndpointsAccessible() {
        // 测试 WorkController 的分页查询端点
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/work/page?current=1&size=10", 
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                    "WorkController 的端点应该可访问");
    }
    
    /**
     * 测试 CharacterController 的端点仍然可访问
     * 需求: 9.3
     */
    @Test
    void testCharacterController_EndpointsAccessible() {
        // 测试 CharacterController 的分页查询端点
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/character/page?current=1&size=10", 
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode(),
                    "CharacterController 的端点应该可访问");
    }
    
    // ==================== 控制器结构验证 ====================
    
    /**
     * 测试 readonly 子包已创建且包含正确的控制器
     * 需求: 1.1
     */
    @Test
    void testReadonlyPackage_Created() {
        // 验证所有 readonly 控制器存在
        List<String> readonlyControllers = Arrays.asList(
            "messageController",
            "channelMessageController",
            "forwardQueueController",
            "processedMessageController",
            "unreadMessageBufferController"
        );
        
        for (String controllerName : readonlyControllers) {
            Object controller = applicationContext.getBean(controllerName);
            assertNotNull(controller, controllerName + " 应该存在");
            
            // 验证控制器在 readonly 子包中
            String packageName = controller.getClass().getPackage().getName();
            assertEquals("org.xlyo.cocomonyab.controller.readonly", packageName,
                        controllerName + " 应该在 org.xlyo.cocomonyab.controller.readonly 包中");
        }
    }
    
    /**
     * 测试所有控制器的方法签名未改变
     * 需求: 9.1, 9.4
     */
    @Test
    void testMessageController_MethodSignaturesUnchanged() throws Exception {
        Object messageController = applicationContext.getBean("messageController");
        Class<?> controllerClass = messageController.getClass();
        
        // 验证关键方法存在
        Method getByIdMethod = controllerClass.getMethod("getById", String.class);
        assertNotNull(getByIdMethod, "getById 方法应该存在");
        
        Method getByTgIdMethod = controllerClass.getMethod("getByTgId", Long.class, Long.class);
        assertNotNull(getByTgIdMethod, "getByTgId 方法应该存在");
        
        Method pageMethod = controllerClass.getMethod("page", Long.class, Long.class, 
            Class.forName("org.xlyo.cocomonyab.domain.dto.MessageQueryDTO"));
        assertNotNull(pageMethod, "page 方法应该存在");
        
        Method getMediaAlbumMethod = controllerClass.getMethod("getMediaAlbum", Long.class, Long.class);
        assertNotNull(getMediaAlbumMethod, "getMediaAlbum 方法应该存在");
    }
}
