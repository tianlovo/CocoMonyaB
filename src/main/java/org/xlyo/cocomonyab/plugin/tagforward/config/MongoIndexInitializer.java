package org.xlyo.cocomonyab.plugin.tagforward.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB索引初始化器
 * <p>
 * 负责创建forward_queue集合的TTL索引，该索引用于自动清理30天前的记录
 * 其他索引（唯一索引和查询索引）通过@CompoundIndex注解在ForwardQueueItem类上定义
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MongoIndexInitializer {
    
    private final MongoTemplate mongoTemplate;
    
    /**
     * TTL过期时间：30天（秒）
     */
    private static final long TTL_EXPIRE_AFTER_SECONDS = TimeUnit.DAYS.toSeconds(30);
    
    /**
     * 初始化MongoDB索引
     * <p>
     * 创建forward_queue集合的TTL索引，用于自动清理过期记录
     * TTL索引会在createTime字段上创建，文档会在创建30天后自动删除
     */
    @PostConstruct
    public void initIndexes() {
        try {
            // 创建TTL索引：在createTime字段上，30天后过期
            Index ttlIndex = new Index()
                    .on("createTime", org.springframework.data.domain.Sort.Direction.ASC)
                    .named("idx_ttl_30days")
                    .expire(TTL_EXPIRE_AFTER_SECONDS, TimeUnit.SECONDS);
            
            String indexName = mongoTemplate.indexOps("forward_queue").createIndex(ttlIndex);
            
            log.info("MongoDB TTL 索引 '{}' 创建成功，用于 forward_queue 集合（{} 天后过期）", 
                    indexName, TimeUnit.SECONDS.toDays(TTL_EXPIRE_AFTER_SECONDS));
        } catch (Exception e) {
            log.error("为 forward_queue 集合创建 MongoDB TTL 索引失败", e);
            // 不抛出异常，允许应用继续启动
        }
    }
}
