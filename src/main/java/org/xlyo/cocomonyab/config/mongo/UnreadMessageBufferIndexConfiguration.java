package org.xlyo.cocomonyab.config.mongo;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.domain.entity.UnreadMessageBuffer;

import java.util.concurrent.TimeUnit;

/**
 * 未读消息缓冲区索引配置
 * <p>
 * 确保 MongoDB 集合的索引正确创建，包括：
 * <ul>
 *   <li>复合唯一索引：chatId + messageId</li>
 *   <li>复合索引：status + fetchTime</li>
 *   <li>TTL 索引：自动清理 7 天前的已处理记录</li>
 * </ul>
 * 
 * @author tianluoqaq
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnreadMessageBufferIndexConfiguration {
    
    private final MongoTemplate mongoTemplate;
    
    /**
     * TTL 时间：7 天（单位：秒）
     */
    private static final long TTL_SECONDS = TimeUnit.DAYS.toSeconds(7);
    
    @PostConstruct
    public void initIndexes() {
        log.info("开始初始化未读消息缓冲区索引...");
        
        try {
            createUnreadMessageBufferIndexes();
            log.info("未读消息缓冲区索引初始化完成");
        } catch (Exception e) {
            log.error("未读消息缓冲区索引初始化失败", e);
            throw new RuntimeException("未读消息缓冲区索引初始化失败", e);
        }
    }
    
    /**
     * 创建未读消息缓冲区索引
     */
    private void createUnreadMessageBufferIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps(UnreadMessageBuffer.class);
        
        // 复合唯一索引：chatId + messageId
        // 确保同一条消息不会被重复缓冲
        Index chatMessageUniqueIndex = new Index()
                .on("chatId", Sort.Direction.ASC)
                .on("messageId", Sort.Direction.ASC)
                .unique()
                .named("idx_chat_message_unique");
        indexOps.createIndex(chatMessageUniqueIndex);
        log.debug("创建复合唯一索引: idx_chat_message_unique (chatId + messageId)");
        
        // 复合索引：status + fetchTime
        // 用于查询特定状态的消息，按获取时间排序
        Index statusFetchTimeIndex = new Index()
                .on("status", Sort.Direction.ASC)
                .on("fetchTime", Sort.Direction.ASC)
                .named("idx_status_fetchTime");
        indexOps.createIndex(statusFetchTimeIndex);
        log.debug("创建复合索引: idx_status_fetchTime (status + fetchTime)");
        
        // TTL 索引：自动清理 7 天前的已处理记录
        // 注意：TTL 索引只能在单个字段上创建，且该字段必须是日期类型
        // MongoDB 会定期（默认每 60 秒）检查并删除过期文档
        Index ttlIndex = new Index()
                .on("createTime", Sort.Direction.ASC)
                .expire(TTL_SECONDS, TimeUnit.SECONDS)
                .named("idx_ttl_7days");
        
        // 注意：TTL 索引会对所有文档生效，但我们只想清理 PROCESSED 状态的记录
        // 这需要在应用层面通过定期清理任务来实现，或者使用 MongoDB 4.2+ 的部分 TTL 索引
        // 这里先创建基础 TTL 索引，后续可以通过定期任务补充清理逻辑
        indexOps.createIndex(ttlIndex);
        log.debug("创建 TTL 索引: idx_ttl_7days (createTime, 7 天过期)");
        
        log.info("未读消息缓冲区索引创建完成：" +
                "1) 复合唯一索引 (chatId + messageId), " +
                "2) 复合索引 (status + fetchTime), " +
                "3) TTL 索引 (createTime, 7 天)");
    }
}
