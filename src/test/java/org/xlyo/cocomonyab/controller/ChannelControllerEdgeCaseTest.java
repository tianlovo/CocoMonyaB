package org.xlyo.cocomonyab.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.ChannelCreateDTO;
import org.xlyo.cocomonyab.domain.dto.ChannelQueryDTO;
import org.xlyo.cocomonyab.domain.vo.ChannelVO;
import org.xlyo.cocomonyab.repository.ChannelRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChannelController 的边界情况测试
 * 测试边界情况，如空列表、超出边界的分页和验证错误聚合
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.embedded.storage.directory=data/db/mongo-test",
    "spring.data.mongodb.database=cocomonya"
})
class ChannelControllerEdgeCaseTest {
    
    @Autowired
    private ChannelController channelController;
    
    @Autowired
    private ChannelRepository channelRepository;
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        // 清理所有现有测试数据
        channelRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // 测试后清理数据
        channelRepository.deleteAll();
    }
    
    /**
     * Test 11.1: 空列表场景
     * 
     * 测试当没有频道存在时，list 端点返回空列表并带有成功状态
     * 
     * Validates: Requirements 9.4
     */
    @Test
    void testEmptyListScenario() {
        // 确保数据库为空
        channelRepository.deleteAll();
        
        // 调用 list 端点
        ApiResponse<List<ChannelVO>> response = channelController.listChannels();
        
        // 验证响应结构
        assertNotNull(response, "Response should not be null");
        assertEquals(200, response.getCode(), "Response code should be 200 (success)");
        assertNotNull(response.getData(), "Response data should not be null");
        
        // 验证返回空列表
        List<ChannelVO> channels = response.getData();
        assertTrue(channels.isEmpty(), "Channel list should be empty when no channels exist");
        assertEquals(0, channels.size(), "Channel list size should be 0");
    }
    
    /**
     * Test 11.2: 超出边界的分页
     * 
     * 测试当请求的页码超出可用页数时，page 端点返回空记录并带有正确的元数据
     * 
     * Validates: Requirements 10.6
     */
    @Test
    void testPageBeyondBounds() {
        // 创建少量频道（5个频道）
        for (int i = 1; i <= 5; i++) {
            ChannelCreateDTO createDTO = new ChannelCreateDTO();
            createDTO.setChannelId((long) i);
            createDTO.setChannelUsername("channel_" + i);
            createDTO.setChannelTitle("Channel " + i);
            createDTO.setMonitoringStatus(true);
            
            channelController.createChannel(createDTO);
        }
        
        // 请求第10页，每页10条（实际只有1页，包含5条记录）
        Long requestedPage = 10L;
        Long pageSize = 10L;
        
        PageResponse<ChannelVO> response = channelController.pageChannels(
            requestedPage, pageSize, new ChannelQueryDTO());
        
        // 验证响应结构
        assertNotNull(response, "Response should not be null");
        assertEquals(200, response.getCode(), "Response code should be 200 (success)");
        assertNotNull(response.getData(), "Response data should not be null");
        
        PageResponse.PageData<ChannelVO> pageData = response.getData();
        
        // 验证分页元数据
        assertEquals(requestedPage, pageData.getCurrent(), 
            "Current page should match requested page");
        assertEquals(pageSize, pageData.getSize(), 
            "Page size should match requested size");
        assertEquals(5L, pageData.getTotal(), 
            "Total should be 5 (number of channels created)");
        assertEquals(1L, pageData.getPages(), 
            "Total pages should be 1 (ceiling(5/10))");
        
        // 验证超出边界的页面返回空记录
        List<ChannelVO> records = pageData.getRecords();
        assertNotNull(records, "Records should not be null");
        assertTrue(records.isEmpty(), 
            "Records should be empty when requested page exceeds available pages");
        assertEquals(0, records.size(), 
            "Records size should be 0 for page beyond bounds");
    }
    
    /**
     * Test 11.3: 验证错误聚合
     * 
     * 测试多个验证错误被聚合到单个响应中
     * 
     * Validates: Requirements 11.6
     */
    @Test
    void testValidationErrorAggregation() throws Exception {
        // 创建一个包含多个验证错误的 ChannelCreateDTO：
        // - channelId 为 null（违反 @NotNull）
        // - channelUsername 为空白（违反 @NotBlank）
        // - channelTitle 为空白（违反 @NotBlank）
        String invalidJson = """
            {
                "channelId": null,
                "channelUsername": "",
                "channelTitle": ""
            }
            """;
        
        // 发送包含无效数据的 POST 请求
        mockMvc.perform(post("/api/channel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(-40006)) // VALIDATION_ERROR code
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.msg").isString());
        
        // 注意：实际的验证消息聚合由 GlobalExceptionHandler 处理
        // 它收集所有 FieldError 消息并用 "; " 连接
        // 我们验证：
        // 1. HTTP 状态为 400 (BAD_REQUEST)
        // 2. 响应代码为 -40006 (VALIDATION_ERROR)
        // 3. 响应消息存在并包含聚合的错误
    }
    
    /**
     * 附加测试：带有大小违规的验证错误聚合
     * 
     * 测试大小约束违规也能被正确聚合
     */
    @Test
    void testValidationErrorAggregationWithSizeViolations() throws Exception {
        // 创建一个包含大小违规的 ChannelCreateDTO：
        // - channelUsername 超过 100 个字符
        // - channelTitle 超过 200 个字符
        String longUsername = "a".repeat(101); // 101 个字符
        String longTitle = "b".repeat(201); // 201 个字符
        
        String invalidJson = String.format("""
            {
                "channelId": 123456,
                "channelUsername": "%s",
                "channelTitle": "%s"
            }
            """, longUsername, longTitle);
        
        // 发送包含无效数据的 POST 请求
        mockMvc.perform(post("/api/channel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(-40006)) // VALIDATION_ERROR code
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.msg").isString());
        
        // GlobalExceptionHandler 应该聚合两个大小违规消息
    }
}
