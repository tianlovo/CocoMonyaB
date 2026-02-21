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
        // 创建 channelId 唯一索引
        Index channelIdIndex = new Index()
                .on("channelId", Sort.Direction.ASC)
                .unique()
                .named("idx_channelId_unique");
        mongoTemplate.indexOps(COLLECTION_NAME).createIndex(channelIdIndex);
        log.info("索引 'idx_channelId_unique' 已创建");
        
        // 创建 monitoringStatus 索引
        Index monitoringStatusIndex = new Index()
                .on("monitoringStatus", Sort.Direction.ASC)
                .named("idx_monitoringStatus");
        mongoTemplate.indexOps(COLLECTION_NAME).createIndex(monitoringStatusIndex);
        log.info("索引 'idx_monitoringStatus' 已创建");
    }
}
