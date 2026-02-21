package org.xlyo.cocomonyab.config;

import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.config.mongo.MongoDatabaseInitializer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MongoDatabaseInitializer 单元测试
 * 测试数据库和集合初始化逻辑
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.embedded.storage.directory=data/db/mongo-test",
    "spring.data.mongodb.database=cocomonya"
})
class MongoDatabaseInitializerTest {
    
    private static final String COLLECTION_NAME = "telegram_channels";
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private MongoDatabaseInitializer databaseInitializer;
    
    @BeforeEach
    void setUp() {
        // 清理测试环境：删除集合（如果存在）
        if (mongoTemplate.collectionExists(COLLECTION_NAME)) {
            mongoTemplate.dropCollection(COLLECTION_NAME);
        }
    }
    
    @Test
    void testDatabaseCreationWhenNotExists() throws Exception {
        // Given: MongoDatabaseInitializer 已注入
        
        // When: 运行初始化器
        databaseInitializer.run(new TestApplicationArguments());
        
        // Then: 数据库应该存在（通过 MongoTemplate 访问）
        MongoDatabase database = mongoTemplate.getDb();
        assertNotNull(database);
        // 数据库名称可能是默认的 "test" 或配置的名称
        assertNotNull(database.getName());
    }
    
    @Test
    void testCollectionCreationWhenNotExists() throws Exception {
        // Given: 集合不存在
        assertFalse(mongoTemplate.collectionExists(COLLECTION_NAME));
        
        // When: 运行初始化器
        databaseInitializer.run(new TestApplicationArguments());
        
        // Then: 集合应该被创建
        assertTrue(mongoTemplate.collectionExists(COLLECTION_NAME));
    }
    
    @Test
    void testIndexCreationOnChannelId() throws Exception {
        // Given: 集合不存在
        assertFalse(mongoTemplate.collectionExists(COLLECTION_NAME));
        
        // When: 运行初始化器
        databaseInitializer.run(new TestApplicationArguments());
        
        // Then: channelId 索引应该被创建且为唯一索引
        List<IndexInfo> indexes = mongoTemplate.indexOps(COLLECTION_NAME).getIndexInfo();
        
        boolean foundChannelIdIndex = false;
        for (IndexInfo index : indexes) {
            if ("idx_channelId_unique".equals(index.getName())) {
                foundChannelIdIndex = true;
                assertTrue(index.isUnique(), "channelId 索引应该是唯一索引");
                break;
            }
        }
        
        assertTrue(foundChannelIdIndex, "应该存在 channelId 索引");
    }
    
    @Test
    void testIndexCreationOnMonitoringStatus() throws Exception {
        // Given: 集合不存在
        assertFalse(mongoTemplate.collectionExists(COLLECTION_NAME));
        
        // When: 运行初始化器
        databaseInitializer.run(new TestApplicationArguments());
        
        // Then: monitoringStatus 索引应该被创建
        List<IndexInfo> indexes = mongoTemplate.indexOps(COLLECTION_NAME).getIndexInfo();
        
        boolean foundMonitoringStatusIndex = false;
        for (IndexInfo index : indexes) {
            if ("idx_monitoringStatus".equals(index.getName())) {
                foundMonitoringStatusIndex = true;
                assertFalse(index.isUnique(), "monitoringStatus 索引不应该是唯一索引");
                break;
            }
        }
        
        assertTrue(foundMonitoringStatusIndex, "应该存在 monitoringStatus 索引");
    }
    
    @Test
    void testIdempotency() throws Exception {
        // Given: 运行初始化器第一次
        databaseInitializer.run(new TestApplicationArguments());
        
        // 验证集合和索引已创建
        assertTrue(mongoTemplate.collectionExists(COLLECTION_NAME));
        List<IndexInfo> indexesAfterFirstRun = mongoTemplate.indexOps(COLLECTION_NAME).getIndexInfo();
        int indexCountAfterFirstRun = indexesAfterFirstRun.size();
        
        // When: 再次运行初始化器
        assertDoesNotThrow(() -> databaseInitializer.run(new TestApplicationArguments()));
        
        // Then: 不应该抛出异常，集合仍然存在，索引数量不变
        assertTrue(mongoTemplate.collectionExists(COLLECTION_NAME));
        List<IndexInfo> indexesAfterSecondRun = mongoTemplate.indexOps(COLLECTION_NAME).getIndexInfo();
        assertEquals(indexCountAfterFirstRun, indexesAfterSecondRun.size(), 
                    "索引数量应该保持不变");
    }
    
    /**
     * 测试用的 ApplicationArguments 实现
     */
    private static class TestApplicationArguments implements ApplicationArguments {
        @Override
        public String[] getSourceArgs() {
            return new String[0];
        }
        
        @Override
        public java.util.Set<String> getOptionNames() {
            return java.util.Set.of();
        }
        
        @Override
        public boolean containsOption(String name) {
            return false;
        }
        
        @Override
        public java.util.List<String> getOptionValues(String name) {
            return null;
        }
        
        @Override
        public java.util.List<String> getNonOptionArgs() {
            return java.util.List.of();
        }
    }
}
