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
    "spring.data.mongodb.database=cocomonya_test"
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
    
    @Test
    void testAuthorIndexCreation() throws Exception {
        // Given: 作者集合不存在
        String collectionName = "tag_authors";
        if (mongoTemplate.collectionExists(collectionName)) {
            mongoTemplate.dropCollection(collectionName);
        }
        
        // When: 运行初始化器
        databaseInitializer.run(new TestApplicationArguments());
        
        // Then: 作者库索引应该被创建
        List<IndexInfo> indexes = mongoTemplate.indexOps(collectionName).getIndexInfo();
        
        // 验证 name 唯一索引
        boolean foundNameIndex = false;
        boolean foundAliasesIndex = false;
        
        for (IndexInfo index : indexes) {
            if ("idx_author_name_unique".equals(index.getName())) {
                foundNameIndex = true;
                assertTrue(index.isUnique(), "作者 name 索引应该是唯一索引");
            } else if ("idx_author_aliases".equals(index.getName())) {
                foundAliasesIndex = true;
            }
        }
        
        assertTrue(foundNameIndex, "应该存在作者 name 唯一索引");
        assertTrue(foundAliasesIndex, "应该存在作者 aliases 索引");
    }
    
    @Test
    void testWorkIndexCreation() throws Exception {
        // Given: 原作集合不存在
        String collectionName = "tag_works";
        if (mongoTemplate.collectionExists(collectionName)) {
            mongoTemplate.dropCollection(collectionName);
        }
        
        // When: 运行初始化器
        databaseInitializer.run(new TestApplicationArguments());
        
        // Then: 原作库索引应该被创建
        List<IndexInfo> indexes = mongoTemplate.indexOps(collectionName).getIndexInfo();
        
        // 验证索引
        boolean foundNameIndex = false;
        boolean foundAliasesIndex = false;
        
        for (IndexInfo index : indexes) {
            if ("idx_work_name_unique".equals(index.getName())) {
                foundNameIndex = true;
                assertTrue(index.isUnique(), "原作 name 索引应该是唯一索引");
            } else if ("idx_work_aliases".equals(index.getName())) {
                foundAliasesIndex = true;
            }
        }
        
        assertTrue(foundNameIndex, "应该存在原作 name 唯一索引");
        assertTrue(foundAliasesIndex, "应该存在原作 aliases 索引");
    }
    
    @Test
    void testCharacterIndexCreation() throws Exception {
        // Given: 角色集合不存在
        String collectionName = "tag_characters";
        if (mongoTemplate.collectionExists(collectionName)) {
            mongoTemplate.dropCollection(collectionName);
        }
        
        // When: 运行初始化器
        databaseInitializer.run(new TestApplicationArguments());
        
        // Then: 角色库索引应该被创建
        List<IndexInfo> indexes = mongoTemplate.indexOps(collectionName).getIndexInfo();
        
        // 验证索引
        boolean foundNameIndex = false;
        boolean foundAliasesIndex = false;
        boolean foundWorkIdIndex = false;
        
        for (IndexInfo index : indexes) {
            if ("idx_character_name_unique".equals(index.getName())) {
                foundNameIndex = true;
                assertTrue(index.isUnique(), "角色 name 索引应该是唯一索引");
            } else if ("idx_character_aliases".equals(index.getName())) {
                foundAliasesIndex = true;
            } else if ("idx_character_workId".equals(index.getName())) {
                foundWorkIdIndex = true;
            }
        }
        
        assertTrue(foundNameIndex, "应该存在角色 name 唯一索引");
        assertTrue(foundAliasesIndex, "应该存在角色 aliases 索引");
        assertTrue(foundWorkIdIndex, "应该存在角色 workId 索引");
    }
    
    @Test
    void testDatabaseConnectionCheck() throws Exception {
        // Given: 数据库连接正常
        
        // When: 运行初始化器
        // Then: 不应该抛出异常
        assertDoesNotThrow(() -> databaseInitializer.run(new TestApplicationArguments()),
                "数据库连接检查应该成功");
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
