package org.xlyo.cocomonyab.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.domain.dto.ChannelCreateDTO;
import org.xlyo.cocomonyab.domain.dto.ChannelUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.ChannelVO;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.service.ChannelService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 嵌入式模式集成测试
 * 验证应用在嵌入式MongoDB模式下能够正常启动、初始化数据库和执行CRUD操作
 * 
 * 注意：此测试需要实际的嵌入式MongoDB环境，需要下载MongoDB二进制文件
 * 可以通过设置环境变量 RUN_EMBEDDED_MONGO_TESTS=true 来启用此测试
 * 
 * Requirements: 1.2, 1.4, 3.1, 3.2, 3.3, 3.4
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.mongodb.mode=embedded",
        "spring.data.mongodb.embedded.storage.directory=data/db/mongo-integration-test"
})
@EnabledIfEnvironmentVariable(named = "RUN_EMBEDDED_MONGO_TESTS", matches = "true")
class EmbeddedModeIntegrationTest {
    
    @Autowired(required = false)
    private MongoTemplate mongoTemplate;
    
    @Autowired(required = false)
    private ChannelService channelService;
    
    @Autowired(required = false)
    private ChannelRepository channelRepository;
    
    /**
     * 测试应用在嵌入式模式下成功启动
     * Requirement 1.2: 嵌入式模式初始化
     * Requirement 1.4: 使用embedded-mongodb库
     */
    @Test
    void testApplicationStartsSuccessfullyWithEmbeddedMode() {
        // Given: 应用已启动（通过@SpringBootTest）
        
        // Then: MongoTemplate应该可用
        assertNotNull(mongoTemplate, "MongoTemplate应该被成功注入");
        
        // 验证可以连接到MongoDB
        assertDoesNotThrow(() -> mongoTemplate.getDb().getName(),
                "应该能够连接到嵌入式MongoDB");
    }
    
    /**
     * 测试数据库和集合自动创建
     * Requirement 3.1: 验证数据库存在
     * Requirement 3.2: 创建数据库
     * Requirement 3.3: 验证集合存在
     * Requirement 3.4: 创建集合
     */
    @Test
    void testDatabaseAndCollectionAreCreatedAutomatically() {
        // Given: 应用已启动
        
        // Then: 数据库应该存在
        String dbName = mongoTemplate.getDb().getName();
        assertNotNull(dbName, "数据库名称不应为null");
        
        // 集合应该存在
        boolean collectionExists = mongoTemplate.collectionExists("telegram_channels");
        assertTrue(collectionExists, "telegram_channels集合应该被自动创建");
        
        // 验证索引已创建
        List<String> indexNames = mongoTemplate.getCollection("telegram_channels")
                .listIndexes()
                .into(new java.util.ArrayList<>())
                .stream()
                .map(doc -> doc.getString("name"))
                .toList();
        
        // 应该至少有_id索引
        assertTrue(indexNames.contains("_id_"), "应该有_id索引");
    }
    
    /**
     * 测试CRUD操作在嵌入式MongoDB下正常工作
     * Requirement 1.2: 嵌入式模式功能完整性
     */
    @Test
    void testCrudOperationsWorkWithEmbeddedMongoDB() {
        // 清理测试数据
        channelRepository.deleteAll();
        
        try {
            // Given: 准备测试数据
            ChannelCreateDTO createDTO = new ChannelCreateDTO();
            createDTO.setChannelId(1001L);
            createDTO.setChannelUsername("test_channel");
            createDTO.setChannelTitle("Test Channel");
            createDTO.setMonitoringStatus(true);
            
            // When: 创建channel
            ChannelVO created = channelService.create(createDTO);
            
            // Then: 创建成功
            assertNotNull(created, "创建的channel不应为null");
            assertNotNull(created.getId(), "创建的channel应该有ID");
            assertEquals(1001L, created.getChannelId(), "channelId应该匹配");
            assertEquals("test_channel", created.getChannelUsername(), "channelUsername应该匹配");
            assertEquals("Test Channel", created.getChannelTitle(), "channelTitle应该匹配");
            assertTrue(created.getMonitoringStatus(), "monitoringStatus应该为true");
            assertNotNull(created.getCreateTime(), "createTime应该被设置");
            assertNotNull(created.getUpdateTime(), "updateTime应该被设置");
            
            // When: 读取channel
            ChannelVO retrieved = channelService.getById(created.getId());
            
            // Then: 读取成功
            assertNotNull(retrieved, "读取的channel不应为null");
            assertEquals(created.getId(), retrieved.getId(), "ID应该匹配");
            assertEquals(created.getChannelId(), retrieved.getChannelId(), "channelId应该匹配");
            
            // When: 更新channel
            ChannelUpdateDTO updateDTO = new ChannelUpdateDTO();
            updateDTO.setChannelTitle("Updated Test Channel");
            updateDTO.setMonitoringStatus(false);
            
            ChannelVO updated = channelService.update(created.getId(), updateDTO);
            
            // Then: 更新成功
            assertNotNull(updated, "更新的channel不应为null");
            assertEquals("Updated Test Channel", updated.getChannelTitle(), "channelTitle应该被更新");
            assertFalse(updated.getMonitoringStatus(), "monitoringStatus应该被更新为false");
            assertEquals("test_channel", updated.getChannelUsername(), "未更新的字段应该保持不变");
            
            // When: 列出所有channels
            List<ChannelVO> list = channelService.list();
            
            // Then: 列表包含创建的channel
            assertNotNull(list, "列表不应为null");
            assertEquals(1, list.size(), "应该有1个channel");
            assertEquals(created.getId(), list.get(0).getId(), "列表中的channel ID应该匹配");
            
            // When: 删除channel
            channelService.deleteById(created.getId());
            
            // Then: 删除成功
            List<ChannelVO> emptyList = channelService.list();
            assertTrue(emptyList.isEmpty(), "删除后列表应该为空");
        } finally {
            // 清理测试数据
            channelRepository.deleteAll();
        }
    }
}

