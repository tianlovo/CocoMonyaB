package org.xlyo.cocomonyab.config.mongo;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.domain.entity.tag.Author;
import org.xlyo.cocomonyab.domain.entity.tag.Character;
import org.xlyo.cocomonyab.domain.entity.tag.Work;

/**
 * 标签数据库索引配置
 * 确保MongoDB集合的索引正确创建
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagDatabaseIndexConfiguration {
    
    private final MongoTemplate mongoTemplate;
    
    @PostConstruct
    public void initIndexes() {
        log.info("开始初始化标签数据库索引...");
        
        try {
            createAuthorIndexes();
            createWorkIndexes();
            createCharacterIndexes();
            
            log.info("标签数据库索引初始化完成");
        } catch (Exception e) {
            log.error("标签数据库索引初始化失败", e);
            throw new RuntimeException("标签数据库索引初始化失败", e);
        }
    }
    
    /**
     * 创建作者库索引
     */
    private void createAuthorIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(Author.class);
        
        // 名称唯一索引
        Index nameIndex = new Index()
                .on("name", Sort.Direction.ASC)
                .unique()
                .named("idx_name_unique");
        indexOps.ensureIndex(nameIndex);
        
        // 别名多键索引
        Index aliasesIndex = new Index()
                .on("aliases", Sort.Direction.ASC)
                .named("idx_aliases");
        indexOps.ensureIndex(aliasesIndex);
        
        log.debug("作者库索引创建完成");
    }
    
    /**
     * 创建原作库索引
     */
    private void createWorkIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(Work.class);
        
        // 名称唯一索引
        Index nameIndex = new Index()
                .on("name", Sort.Direction.ASC)
                .unique()
                .named("idx_name_unique");
        indexOps.ensureIndex(nameIndex);
        
        // 别名多键索引
        Index aliasesIndex = new Index()
                .on("aliases", Sort.Direction.ASC)
                .named("idx_aliases");
        indexOps.ensureIndex(aliasesIndex);
        
        log.debug("原作库索引创建完成");
    }
    
    /**
     * 创建角色库索引
     */
    private void createCharacterIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(Character.class);
        
        // 名称唯一索引
        Index nameIndex = new Index()
                .on("name", Sort.Direction.ASC)
                .unique()
                .named("idx_name_unique");
        indexOps.ensureIndex(nameIndex);
        
        // 别名多键索引
        Index aliasesIndex = new Index()
                .on("aliases", Sort.Direction.ASC)
                .named("idx_aliases");
        indexOps.ensureIndex(aliasesIndex);
        
        // 原作ID索引
        Index workIdIndex = new Index()
                .on("workId", Sort.Direction.ASC)
                .named("idx_work_id");
        indexOps.ensureIndex(workIdIndex);
        
        log.debug("角色库索引创建完成");
    }
}
