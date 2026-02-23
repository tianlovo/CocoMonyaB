package org.xlyo.cocomonyab.controller;

import net.jqwik.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigCreateDTO;
import org.xlyo.cocomonyab.domain.entity.TagFilterConfig;
import org.xlyo.cocomonyab.repository.TagFilterConfigRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TagFilterConfigController 属性测试
 * 使用属性测试验证API响应格式一致性
 * 
 * Property 17: API响应格式一致性
 * 
 * Validates: Requirement 4.6
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.embedded.storage.directory=data/db/mongo-test",
    "spring.data.mongodb.database=cocomonya"
})
class TagFilterConfigControllerPropertyTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private TagFilterConfigRepository repository;
    
    @Autowired
    private TagFilterConfigController controller;
    
    private MockMvc mockMvc;
    
    /**
     * Property 17: API响应格式一致性
     * 
     * For any API响应，响应体必须包含code、msg和data字段，
     * 且成功响应的code为200，失败响应的code为负数
     * 
     * Validates: Requirement 4.6
     */
    @Property(tries = 100)
    @Label("Feature: tag-filter-config, Property 17: API响应格式一致性")
    void apiResponseFormatConsistency(
            @ForAll("validChannelId") Long channelId,
            @ForAll("validTags") List<String> tags,
            @ForAll("validMatchMode") String matchMode,
            @ForAll Boolean enabled) throws Exception {
        
        setupMockMvc();
        repository.deleteAll();
        
        // 创建并保存配置
        TagFilterConfig config = new TagFilterConfig();
        config.setChannelId(channelId);
        config.setTags(tags);
        config.setMatchMode(matchMode);
        config.setEnabled(enabled);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        
        TagFilterConfig saved = repository.save(config);
        
        // 测试成功场景 - 通过ID获取配置
        mockMvc.perform(get("/api/tag-filter-config/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.channelId").value(channelId))
                .andExpect(jsonPath("$.data.matchMode").value(matchMode))
                .andExpect(jsonPath("$.data.enabled").value(enabled));
        
        // 测试失败场景 - 不存在的ID
        String nonExistentId = "507f1f77bcf86cd799439011";
        mockMvc.perform(get("/api/tag-filter-config/{id}", nonExistentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.lessThan(0)))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.msg").isString())
                .andExpect(jsonPath("$.data").doesNotExist());
        
        repository.deleteAll();
    }
    
    /**
     * Property 17.1: 分页响应格式一致性
     * 
     * For any 分页查询响应，响应体必须包含code、msg和data字段，
     * data中必须包含records、current、size、total和pages字段
     * 
     * Validates: Requirement 4.6
     */
    @Property(tries = 50)
    @Label("Feature: tag-filter-config, Property 17.1: 分页响应格式一致性")
    void paginationResponseFormatConsistency(
            @ForAll("validTags") List<String> tags,
            @ForAll("validMatchMode") String matchMode,
            @ForAll Boolean enabled) throws Exception {
        
        setupMockMvc();
        repository.deleteAll();
        
        // 创建多个配置
        for (int i = 1; i <= 3; i++) {
            TagFilterConfig config = new TagFilterConfig();
            config.setChannelId(-1001234567890L - i);
            config.setTags(tags);
            config.setMatchMode(matchMode);
            config.setEnabled(enabled);
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            repository.save(config);
        }
        
        // 测试分页查询响应格式
        mockMvc.perform(get("/api/tag-filter-config/page")
                .param("current", "1")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.current").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.pages").exists());
        
        repository.deleteAll();
    }
    
    /**
     * Property 17.2: 全局配置响应格式一致性
     * 
     * For any 全局配置操作，响应格式必须一致
     * 
     * Validates: Requirement 4.6
     */
    @Property(tries = 50)
    @Label("Feature: tag-filter-config, Property 17.2: 全局配置响应格式一致性")
    void globalConfigResponseFormatConsistency(
            @ForAll("validTags") List<String> tags,
            @ForAll("validMatchMode") String matchMode,
            @ForAll Boolean enabled) throws Exception {
        
        setupMockMvc();
        repository.deleteAll();
        
        // 创建全局配置
        TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
        dto.setTags(tags);
        dto.setMatchMode(matchMode);
        dto.setEnabled(enabled);
        
        controller.createOrUpdateGlobalConfig(dto);
        
        // 测试获取全局配置响应格式
        mockMvc.perform(get("/api/tag-filter-config/global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.channelId").doesNotExist())
                .andExpect(jsonPath("$.data.tags").isArray())
                .andExpect(jsonPath("$.data.matchMode").value(matchMode))
                .andExpect(jsonPath("$.data.enabled").value(enabled))
                .andExpect(jsonPath("$.data.createTime").exists())
                .andExpect(jsonPath("$.data.updateTime").exists());
        
        repository.deleteAll();
    }
    
    /**
     * Property 17.3: 有效配置响应格式一致性
     * 
     * For any 有效配置查询，响应格式必须一致
     * 
     * Validates: Requirement 4.6
     */
    @Property(tries = 50)
    @Label("Feature: tag-filter-config, Property 17.3: 有效配置响应格式一致性")
    void effectiveConfigResponseFormatConsistency(
            @ForAll("validChannelId") Long channelId,
            @ForAll("validTags") List<String> tags,
            @ForAll("validMatchMode") String matchMode,
            @ForAll Boolean enabled) throws Exception {
        
        setupMockMvc();
        repository.deleteAll();
        
        // 创建全局配置
        TagFilterConfig globalConfig = new TagFilterConfig();
        globalConfig.setChannelId(null);
        globalConfig.setTags(Arrays.asList("global"));
        globalConfig.setMatchMode("whitelist");
        globalConfig.setEnabled(true);
        globalConfig.setCreateTime(LocalDateTime.now());
        globalConfig.setUpdateTime(LocalDateTime.now());
        repository.save(globalConfig);
        
        // 测试获取有效配置响应格式（应返回全局配置）
        mockMvc.perform(get("/api/tag-filter-config/effective/{channelId}", channelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.tags").isArray())
                .andExpect(jsonPath("$.data.matchMode").exists())
                .andExpect(jsonPath("$.data.enabled").exists())
                .andExpect(jsonPath("$.data.createTime").exists())
                .andExpect(jsonPath("$.data.updateTime").exists());
        
        repository.deleteAll();
    }
    
    // Arbitraries for generating test data
    
    @Provide
    Arbitrary<Long> validChannelId() {
        return Arbitraries.longs().between(-9999999999999L, -1000000000000L);
    }
    
    @Provide
    Arbitrary<List<String>> validTags() {
        return Arbitraries.of(
            Arrays.asList("tech", "news"),
            Arrays.asList("urgent"),
            Arrays.asList("ai", "ml", "data"),
            Arrays.asList("test"),
            Arrays.asList("important", "priority")
        );
    }
    
    @Provide
    Arbitrary<String> validMatchMode() {
        return Arbitraries.of("whitelist", "blacklist");
    }
    
    private void setupMockMvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        }
    }
}
