package org.xlyo.cocomonyab.config.mongo;

import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化器
 * 在应用启动时自动创建数据库、集合和索引
 */
@Slf4j
@Component
public class MongoDatabaseInitializer implements ApplicationRunner {
    
    private static final String DATABASE_NAME = "cocomonya";
    private static final String COLLECTION_NAME = "telegram_channels";
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Override
    public void run(ApplicationArguments args) {
        log.info("开始初始化 MongoDB 数据库和集合...");
        
        try {
            // 检查数据库连接
            checkDatabaseConnection();
            
            // 验证并创建数据库（MongoDB 会在首次使用时自动创建）
            verifyAndCreateDatabase();
            
            // 验证并创建集合
            verifyAndCreateCollection();
            
            // 创建索引
            createIndexes();
            
            log.info("MongoDB 数据库和集合初始化完成");
        } catch (Exception e) {
            log.error("MongoDB 数据库初始化失败", e);
            throw new RuntimeException("数据库初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查数据库连接
     */
    private void checkDatabaseConnection() {
        try {
            // 执行简单的 ping 命令检查连接
            mongoTemplate.executeCommand("{ ping: 1 }");
            log.info("数据库连接检查成功");
        } catch (Exception e) {
            log.error("数据库连接检查失败", e);
            throw new RuntimeException("无法连接到 MongoDB 数据库: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证并创建数据库
     */
    private void verifyAndCreateDatabase() {
        MongoDatabase database = mongoTemplate.getDb();
        String dbName = database.getName();
        
        if (DATABASE_NAME.equals(dbName)) {
            log.info("数据库 '{}' 已存在或将在首次使用时创建", DATABASE_NAME);
        } else {
            log.warn("当前数据库名称为 '{}', 期望为 '{}'", dbName, DATABASE_NAME);
        }
    }
    
    /**
     * 验证并创建集合
     */
    private void verifyAndCreateCollection() {
        if (!mongoTemplate.collectionExists(COLLECTION_NAME)) {
            mongoTemplate.createCollection(COLLECTION_NAME);
            log.info("集合 '{}' 创建成功", COLLECTION_NAME);
        } else {
            log.info("集合 '{}' 已存在", COLLECTION_NAME);
        }
    }
    
    /**
     * 创建索引
     */
    private void createIndexes() {
        // 创建 telegram_channels 集合索引
        createChannelIndexes();
        
        // 创建标签数据库索引
        createTagDatabaseIndexes();
    }
    
    /**
     * 安全创建索引 - 如果索引已存在则跳过
     */
    private void createIndexSafely(String collectionName, Index index, String indexName) {
        try {
            mongoTemplate.indexOps(collectionName).createIndex(index);
            log.info("索引 '{}' 已创建", indexName);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (e.getMessage().contains("IndexOptionsConflict") || e.getMessage().contains("already exists")) {
                log.warn("索引 '{}' 已存在，跳过创建", indexName);
            } else {
                throw e;
            }
        }
    }
    
    /**
     * 创建频道集合索引
     */
    private void createChannelIndexes() {
        // 创建 channelId 唯一索引
        Index channelIdIndex = new Index()
                .on("channelId", Sort.Direction.ASC)
                .unique()
                .named("idx_channelId_unique");
        createIndexSafely(COLLECTION_NAME, channelIdIndex, "idx_channelId_unique");
        
        // 创建 monitoringStatus 索引
        Index monitoringStatusIndex = new Index()
                .on("monitoringStatus", Sort.Direction.ASC)
                .named("idx_monitoringStatus");
        createIndexSafely(COLLECTION_NAME, monitoringStatusIndex, "idx_monitoringStatus");
    }
    
    /**
     * 创建标签数据库索引
     */
    private void createTagDatabaseIndexes() {
        // 作者库索引
        createAuthorIndexes();
        
        // 原作库索引
        createWorkIndexes();
        
        // 角色库索引
        createCharacterIndexes();
    }
    
    /**
     * 创建作者库索引
     */
    private void createAuthorIndexes() {
        String collectionName = "tag_authors";
        
        // 为 Author.name 创建唯一索引
        Index nameIndex = new Index()
                .on("name", Sort.Direction.ASC)
                .unique()
                .named("idx_author_name_unique");
        createIndexSafely(collectionName, nameIndex, "idx_author_name_unique");
        
        // 为 Author.aliases 创建多键索引
        Index aliasesIndex = new Index()
                .on("aliases", Sort.Direction.ASC)
                .named("idx_author_aliases");
        createIndexSafely(collectionName, aliasesIndex, "idx_author_aliases");
    }
    
    /**
     * 创建原作库索引
     */
    private void createWorkIndexes() {
        String collectionName = "tag_works";
        
        // 为 Work.name 创建唯一索引
        Index nameIndex = new Index()
                .on("name", Sort.Direction.ASC)
                .unique()
                .named("idx_work_name_unique");
        createIndexSafely(collectionName, nameIndex, "idx_work_name_unique");
        
        // 为 Work.aliases 创建多键索引
        Index aliasesIndex = new Index()
                .on("aliases", Sort.Direction.ASC)
                .named("idx_work_aliases");
        createIndexSafely(collectionName, aliasesIndex, "idx_work_aliases");
    }
    
    /**
     * 创建角色库索引
     */
    private void createCharacterIndexes() {
        String collectionName = "tag_characters";
        
        // 为 Character.name 创建唯一索引
        Index nameIndex = new Index()
                .on("name", Sort.Direction.ASC)
                .unique()
                .named("idx_character_name_unique");
        createIndexSafely(collectionName, nameIndex, "idx_character_name_unique");
        
        // 为 Character.aliases 创建多键索引
        Index aliasesIndex = new Index()
                .on("aliases", Sort.Direction.ASC)
                .named("idx_character_aliases");
        createIndexSafely(collectionName, aliasesIndex, "idx_character_aliases");
        
        // 为 Character.workId 创建普通索引
        Index workIdIndex = new Index()
                .on("workId", Sort.Direction.ASC)
                .named("idx_character_workId");
        createIndexSafely(collectionName, workIdIndex, "idx_character_workId");
    }
}
