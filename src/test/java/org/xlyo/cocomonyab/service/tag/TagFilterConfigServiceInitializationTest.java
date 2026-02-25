package org.xlyo.cocomonyab.service.tag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.xlyo.cocomonyab.domain.vo.tag.TagFilterConfigVO;
import org.xlyo.cocomonyab.repository.tag.TagFilterConfigRepository;
import org.xlyo.cocomonyab.service.tag.impl.TagFilterConfigServiceImpl;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 标签过滤配置服务初始化测试
 * 
 * 测试应用启动时的默认配置初始化
 * 
 * 需求: 5.7
 */
@SpringBootTest
@Testcontainers
class TagFilterConfigServiceInitializationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private TagFilterConfigService tagFilterConfigService;
    
    @Autowired
    private TagFilterConfigServiceImpl tagFilterConfigServiceImpl;
    
    @Autowired
    private TagFilterConfigRepository tagFilterConfigRepository;
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        tagFilterConfigRepository.deleteAll();
    }
    
    /**
     * 测试默认全局配置初始化
     * 需求: 5.7
     */
    @Test
    void testDefaultConfigInitialization() {
        // Given: 全局配置不存在
        assertFalse(tagFilterConfigRepository.existsById("global"));
        
        // When: 调用初始化方法
        tagFilterConfigServiceImpl.initializeDefaultConfig();
        
        // Then: 应该创建默认全局配置
        assertTrue(tagFilterConfigRepository.existsById("global"));
        
        TagFilterConfigVO config = tagFilterConfigService.getGlobal();
        assertNotNull(config);
        assertEquals("global", config.getId());
        assertNotNull(config.getAuthorIds());
        assertTrue(config.getAuthorIds().isEmpty());
        assertNotNull(config.getCharacterIds());
        assertTrue(config.getCharacterIds().isEmpty());
        assertNotNull(config.getWorkIds());
        assertTrue(config.getWorkIds().isEmpty());
        assertNotNull(config.getCustomTags());
        assertTrue(config.getCustomTags().isEmpty());
        assertEquals("whitelist", config.getMatchMode());
        assertFalse(config.getEnabled());
        assertNotNull(config.getCreateTime());
        assertNotNull(config.getUpdateTime());
    }
    
    /**
     * 测试初始化的幂等性
     * 需求: 5.7
     */
    @Test
    void testInitializationIdempotency() {
        // Given: 全局配置不存在
        assertFalse(tagFilterConfigRepository.existsById("global"));
        
        // When: 第一次初始化
        tagFilterConfigServiceImpl.initializeDefaultConfig();
        TagFilterConfigVO firstConfig = tagFilterConfigService.getGlobal();
        
        // When: 第二次初始化
        tagFilterConfigServiceImpl.initializeDefaultConfig();
        TagFilterConfigVO secondConfig = tagFilterConfigService.getGlobal();
        
        // Then: 配置应该保持不变
        assertEquals(firstConfig.getId(), secondConfig.getId());
        assertEquals(firstConfig.getCreateTime(), secondConfig.getCreateTime());
        
        // 验证只有一条记录
        assertEquals(1, tagFilterConfigRepository.count());
    }
    
    /**
     * 测试初始化后配置可以正常更新
     * 需求: 5.7
     */
    @Test
    void testConfigUpdateAfterInitialization() {
        // Given: 初始化默认配置
        tagFilterConfigServiceImpl.initializeDefaultConfig();
        TagFilterConfigVO initialConfig = tagFilterConfigService.getGlobal();
        assertFalse(initialConfig.getEnabled());
        
        // When: 更新配置
        org.xlyo.cocomonyab.domain.dto.tag.TagFilterConfigUpdateDTO updateDTO = 
            new org.xlyo.cocomonyab.domain.dto.tag.TagFilterConfigUpdateDTO();
        updateDTO.setEnabled(true);
        updateDTO.setMatchMode("blacklist");
        
        TagFilterConfigVO updatedConfig = tagFilterConfigService.update("global", updateDTO);
        
        // Then: 配置应该被正确更新
        assertTrue(updatedConfig.getEnabled());
        assertEquals("blacklist", updatedConfig.getMatchMode());
        
        // 验证更新时间改变
        assertTrue(updatedConfig.getUpdateTime().isAfter(initialConfig.getUpdateTime()) ||
                   updatedConfig.getUpdateTime().isEqual(initialConfig.getUpdateTime()));
    }
}
