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
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigCreateDTO;
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigQueryDTO;
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.TagFilterConfigVO;
import org.xlyo.cocomonyab.repository.TagFilterConfigRepository;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TagFilterConfigController 单元测试
 * 测试所有API端点的基本功能、参数验证和错误处理
 * 
 * Validates: Requirements 6.1-6.10
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.embedded.storage.directory=data/db/mongo-test",
    "spring.data.mongodb.database=cocomonya"
})
class TagFilterConfigControllerTest {
    
    @Autowired
    private TagFilterConfigController controller;
    
    @Autowired
    private TagFilterConfigRepository repository;
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        repository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }
    
    /**
     * Test 1: 创建全局配置
     * POST /api/tag-filter-config/global
     * 
     * Validates: Requirement 6.1
     */
    @Test
    void testCreateGlobalConfig() {
        // Given
        TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
        dto.setTags(Arrays.asList("tech", "news"));
        dto.setMatchMode("whitelist");
        dto.setEnabled(true);
        
        // When
        ApiResponse<TagFilterConfigVO> response = controller.createOrUpdateGlobalConfig(dto);
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        
        TagFilterConfigVO vo = response.getData();
        assertNotNull(vo.getId());
        assertNull(vo.getChannelId());
        assertEquals("whitelist", vo.getMatchMode());
        assertEquals(true, vo.getEnabled());
        assertEquals(2, vo.getTags().size());
    }
    
    /**
     * Test 2: 更新全局配置
     * POST /api/tag-filter-config/global
     * 
     * Validates: Requirement 6.1
     */
    @Test
    void testUpdateGlobalConfig() {
        // Given - 先创建全局配置
        TagFilterConfigCreateDTO createDto = new TagFilterConfigCreateDTO();
        createDto.setTags(Arrays.asList("tech"));
        createDto.setMatchMode("whitelist");
        createDto.setEnabled(true);
        controller.createOrUpdateGlobalConfig(createDto);
        
        // When - 更新全局配置
        TagFilterConfigCreateDTO updateDto = new TagFilterConfigCreateDTO();
        updateDto.setTags(Arrays.asList("news", "ai"));
        updateDto.setMatchMode("blacklist");
        updateDto.setEnabled(false);
        ApiResponse<TagFilterConfigVO> response = controller.createOrUpdateGlobalConfig(updateDto);
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getCode());
        
        TagFilterConfigVO vo = response.getData();
        assertNull(vo.getChannelId());
        assertEquals("blacklist", vo.getMatchMode());
        assertEquals(false, vo.getEnabled());
        assertEquals(2, vo.getTags().size());
        
        // 验证只有一个全局配置
        assertEquals(1, repository.count());
    }
    
    /**
     * Test 3: 获取全局配置
     * GET /api/tag-filter-config/global
     * 
     * Validates: Requirement 6.2
     */
    @Test
    void testGetGlobalConfig() {
        // Given
        TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
        dto.setTags(Arrays.asList("test"));
        dto.setMatchMode("whitelist");
        dto.setEnabled(true);
        controller.createOrUpdateGlobalConfig(dto);
        
        // When
        ApiResponse<TagFilterConfigVO> response = controller.getGlobalConfig();
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertNull(response.getData().getChannelId());
    }
    
    /**
     * Test 4: 获取不存在的全局配置
     * GET /api/tag-filter-config/global
     * 
     * Validates: Requirement 6.2
     */
    @Test
    void testGetGlobalConfigNotFound() {
        // Given - 没有全局配置
        
        // When & Then
        assertThrows(Exception.class, () -> controller.getGlobalConfig());
    }
    
    /**
     * Test 5: 创建频道配置
     * POST /api/tag-filter-config/channel
     * 
     * Validates: Requirement 6.3
     */
    @Test
    void testCreateChannelConfig() {
        // Given
        TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
        dto.setChannelId(-1001234567890L);
        dto.setTags(Arrays.asList("urgent"));
        dto.setMatchMode("blacklist");
        dto.setEnabled(true);
        
        // When
        ApiResponse<TagFilterConfigVO> response = controller.createChannelConfig(dto);
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        
        TagFilterConfigVO vo = response.getData();
        assertNotNull(vo.getId());
        assertEquals(-1001234567890L, vo.getChannelId());
        assertEquals("blacklist", vo.getMatchMode());
        assertEquals(true, vo.getEnabled());
    }
    
    /**
     * Test 6: 创建重复的频道配置
     * POST /api/tag-filter-config/channel
     * 
     * Validates: Requirement 6.3
     */
    @Test
    void testCreateDuplicateChannelConfig() {
        // Given - 先创建一个频道配置
        TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
        dto.setChannelId(-1001234567890L);
        dto.setTags(Arrays.asList("test"));
        dto.setMatchMode("whitelist");
        dto.setEnabled(true);
        controller.createChannelConfig(dto);
        
        // When & Then - 尝试创建相同channelId的配置
        assertThrows(Exception.class, () -> controller.createChannelConfig(dto));
    }
    
    /**
     * Test 7: 更新配置
     * PUT /api/tag-filter-config/{id}
     * 
     * Validates: Requirement 6.4
     */
    @Test
    void testUpdateConfig() {
        // Given - 先创建配置
        TagFilterConfigCreateDTO createDto = new TagFilterConfigCreateDTO();
        createDto.setChannelId(-1001234567890L);
        createDto.setTags(Arrays.asList("old"));
        createDto.setMatchMode("whitelist");
        createDto.setEnabled(true);
        ApiResponse<TagFilterConfigVO> createResponse = controller.createChannelConfig(createDto);
        String configId = createResponse.getData().getId();
        
        // When - 更新配置
        TagFilterConfigUpdateDTO updateDto = new TagFilterConfigUpdateDTO();
        updateDto.setTags(Arrays.asList("new", "updated"));
        updateDto.setMatchMode("blacklist");
        ApiResponse<TagFilterConfigVO> response = controller.updateConfig(configId, updateDto);
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getCode());
        
        TagFilterConfigVO vo = response.getData();
        assertEquals(configId, vo.getId());
        assertEquals("blacklist", vo.getMatchMode());
        assertEquals(2, vo.getTags().size());
        assertTrue(vo.getTags().contains("new"));
    }
    
    /**
     * Test 8: 删除配置
     * DELETE /api/tag-filter-config/{id}
     * 
     * Validates: Requirement 6.5
     */
    @Test
    void testDeleteConfig() {
        // Given - 先创建配置
        TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
        dto.setChannelId(-1001234567890L);
        dto.setTags(Arrays.asList("test"));
        dto.setMatchMode("whitelist");
        dto.setEnabled(true);
        ApiResponse<TagFilterConfigVO> createResponse = controller.createChannelConfig(dto);
        String configId = createResponse.getData().getId();
        
        // When
        ApiResponse<Void> response = controller.deleteConfig(configId);
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getCode());
        
        // 验证配置已删除
        assertFalse(repository.existsById(configId));
    }
    
    /**
     * Test 9: 通过ID获取配置
     * GET /api/tag-filter-config/{id}
     * 
     * Validates: Requirement 6.6
     */
    @Test
    void testGetConfigById() {
        // Given
        TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
        dto.setChannelId(-1001234567890L);
        dto.setTags(Arrays.asList("test"));
        dto.setMatchMode("whitelist");
        dto.setEnabled(true);
        ApiResponse<TagFilterConfigVO> createResponse = controller.createChannelConfig(dto);
        String configId = createResponse.getData().getId();
        
        // When
        ApiResponse<TagFilterConfigVO> response = controller.getConfigById(configId);
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(configId, response.getData().getId());
    }
    
    /**
     * Test 10: 通过channelId获取配置
     * GET /api/tag-filter-config/channel/{channelId}
     * 
     * Validates: Requirement 6.6
     */
    @Test
    void testGetConfigByChannelId() {
        // Given
        Long channelId = -1001234567890L;
        TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
        dto.setChannelId(channelId);
        dto.setTags(Arrays.asList("test"));
        dto.setMatchMode("whitelist");
        dto.setEnabled(true);
        controller.createChannelConfig(dto);
        
        // When
        ApiResponse<TagFilterConfigVO> response = controller.getConfigByChannelId(channelId);
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(channelId, response.getData().getChannelId());
    }
    
    /**
     * Test 11: 获取有效配置（频道配置存在）
     * GET /api/tag-filter-config/effective/{channelId}
     * 
     * Validates: Requirement 6.8
     */
    @Test
    void testGetEffectiveConfigWithChannelConfig() {
        // Given - 创建全局配置和频道配置
        TagFilterConfigCreateDTO globalDto = new TagFilterConfigCreateDTO();
        globalDto.setTags(Arrays.asList("global"));
        globalDto.setMatchMode("whitelist");
        globalDto.setEnabled(true);
        controller.createOrUpdateGlobalConfig(globalDto);
        
        Long channelId = -1001234567890L;
        TagFilterConfigCreateDTO channelDto = new TagFilterConfigCreateDTO();
        channelDto.setChannelId(channelId);
        channelDto.setTags(Arrays.asList("channel"));
        channelDto.setMatchMode("blacklist");
        channelDto.setEnabled(false);
        controller.createChannelConfig(channelDto);
        
        // When
        ApiResponse<TagFilterConfigVO> response = controller.getEffectiveConfig(channelId);
        
        // Then - 应该返回频道配置
        assertNotNull(response);
        assertEquals(200, response.getCode());
        
        TagFilterConfigVO vo = response.getData();
        assertEquals(channelId, vo.getChannelId());
        assertEquals("blacklist", vo.getMatchMode());
        assertTrue(vo.getTags().contains("channel"));
    }
    
    /**
     * Test 12: 获取有效配置（只有全局配置）
     * GET /api/tag-filter-config/effective/{channelId}
     * 
     * Validates: Requirement 6.8
     */
    @Test
    void testGetEffectiveConfigWithOnlyGlobalConfig() {
        // Given - 只创建全局配置
        TagFilterConfigCreateDTO globalDto = new TagFilterConfigCreateDTO();
        globalDto.setTags(Arrays.asList("global"));
        globalDto.setMatchMode("whitelist");
        globalDto.setEnabled(true);
        controller.createOrUpdateGlobalConfig(globalDto);
        
        // When
        Long channelId = -1001234567890L;
        ApiResponse<TagFilterConfigVO> response = controller.getEffectiveConfig(channelId);
        
        // Then - 应该返回全局配置
        assertNotNull(response);
        assertEquals(200, response.getCode());
        
        TagFilterConfigVO vo = response.getData();
        assertNull(vo.getChannelId());
        assertEquals("whitelist", vo.getMatchMode());
        assertTrue(vo.getTags().contains("global"));
    }
    
    /**
     * Test 13: 分页查询频道配置
     * GET /api/tag-filter-config/page
     * 
     * Validates: Requirement 6.7
     */
    @Test
    void testPageChannelConfigs() {
        // Given - 创建多个频道配置
        for (int i = 1; i <= 5; i++) {
            TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
            dto.setChannelId(-1001234567890L - i);
            dto.setTags(Arrays.asList("tag" + i));
            dto.setMatchMode("whitelist");
            dto.setEnabled(true);
            controller.createChannelConfig(dto);
        }
        
        // When
        PageResponse<TagFilterConfigVO> response = controller.pageChannelConfigs(
            1L, 3L, new TagFilterConfigQueryDTO());
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        
        PageResponse.PageData<TagFilterConfigVO> pageData = response.getData();
        assertEquals(1L, pageData.getCurrent());
        assertEquals(3L, pageData.getSize());
        assertEquals(5L, pageData.getTotal());
        assertEquals(3, pageData.getRecords().size());
    }
    
    /**
     * Test 14: 触发重新加载
     * POST /api/tag-filter-config/reload
     * 
     * Validates: Requirement 6.10
     */
    @Test
    void testReloadAll() {
        // When
        ApiResponse<Void> response = controller.reloadAll();
        
        // Then
        assertNotNull(response);
        assertEquals(200, response.getCode());
    }
    
    /**
     * Test 15: 参数验证 - tags为null
     * 
     * Validates: Requirement 6.10
     */
    @Test
    void testValidationTagsNull() throws Exception {
        // Given
        String invalidJson = """
            {
                "tags": null,
                "matchMode": "whitelist",
                "enabled": true
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/tag-filter-config/global")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(-40006));
    }
    
    /**
     * Test 16: 参数验证 - matchMode无效
     * 
     * Validates: Requirement 6.10
     */
    @Test
    void testValidationInvalidMatchMode() throws Exception {
        // Given
        String invalidJson = """
            {
                "tags": ["test"],
                "matchMode": "invalid",
                "enabled": true
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/tag-filter-config/global")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(-40006));
    }
    
    /**
     * Test 17: 参数验证 - enabled为null
     * 
     * Validates: Requirement 6.10
     */
    @Test
    void testValidationEnabledNull() throws Exception {
        // Given
        String invalidJson = """
            {
                "tags": ["test"],
                "matchMode": "whitelist",
                "enabled": null
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/tag-filter-config/global")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(-40006));
    }
    
    /**
     * Test 18: 错误处理 - 配置不存在
     * 
     * Validates: Requirement 6.10
     */
    @Test
    void testErrorHandlingConfigNotFound() {
        // When & Then
        assertThrows(Exception.class, () -> 
            controller.getConfigById("nonexistent-id"));
    }
}
