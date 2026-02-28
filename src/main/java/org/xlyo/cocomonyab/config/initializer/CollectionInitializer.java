package org.xlyo.cocomonyab.config.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.event.startup.DatabaseReadyEvent;
import org.xlyo.cocomonyab.event.startup.StartupEventPublisher;
import org.xlyo.cocomonyab.event.startup.StartupException;
import org.xlyo.cocomonyab.event.startup.StartupProgressTracker;

/**
 * 集合初始化器
 * <p>
 * 负责应用启动时的数据库集合初始化阶段，包括：
 * <ul>
 *   <li>监听数据库就绪事件</li>
 *   <li>按指定顺序创建集合索引</li>
 *   <li>实现索引幂等性检查（索引已存在则跳过）</li>
 *   <li>检查初始数据（telegram_channels 是否为空）</li>
 *   <li>发布集合就绪事件</li>
 * </ul>
 * </p>
 * <p>
 * 索引创建顺序：telegram_channels → raw_messages → channel_messages → tag_authors → 
 * tag_works → tag_characters → tag_filter_configs_v2 → forward_queue → 
 * processed_messages → unread_messages_buffer
 * </p>
 * <p>
 * 这是启动流程的第三个阶段，依赖于数据库初始化阶段的完成
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CollectionInitializer {
    
    private final MongoTemplate mongoTemplate;
    private final StartupEventPublisher eventPublisher;
    private final StartupProgressTracker progressTracker;
    
    /**
     * 监听数据库就绪事件，开始集合初始化
     * <p>
     * 当数据库初始化完成后，此方法会被自动调用。
     * 执行索引创建和初始数据检查，并在成功后发布集合就绪事件。
     * </p>
     *
     * @param event 数据库就绪事件
     */
    @EventListener
    public void onDatabaseReady(DatabaseReadyEvent event) {
        progressTracker.startPhase("集合初始化");
        
        try {
            log.info("开始集合初始化...");
            
            // 1. 按顺序创建索引
            createIndexes();
            
            // 2. 检查初始数据
            checkInitialData();
            
            // 3. 发布集合就绪事件
            eventPublisher.publishCollectionsReady();
            
            progressTracker.completePhase("集合初始化");
            log.info("✅ 集合初始化完成");
            
        } catch (Exception e) {
            progressTracker.failPhase("集合初始化", e.getMessage());
            log.error("❌ 集合初始化失败", e);
            throw new StartupException("集合初始化失败", e);
        }
    }
    
    /**
     * 按指定顺序创建所有集合的索引
     * <p>
     * 索引创建顺序：telegram_channels → raw_messages → channel_messages → tag_authors → 
     * tag_works → tag_characters → tag_filter_configs_v2 → forward_queue → 
     * processed_messages → unread_messages_buffer
     * </p>
     * <p>
     * 如果某个索引创建失败，会记录错误但继续创建其他索引（非致命错误）
     * </p>
     */
    private void createIndexes() {
        log.info("开始创建集合索引...");
        
        // 按指定顺序创建索引
        createTelegramChannelsIndexes();
        createRawMessagesIndexes();
        createChannelMessagesIndexes();
        createTagAuthorsIndexes();
        createTagWorksIndexes();
        createTagCharactersIndexes();
        createTagFilterConfigsIndexes();
        createForwardQueueIndexes();
        createProcessedMessagesIndexes();
        createUnreadMessagesBufferIndexes();
        
        log.info("✅ 集合索引创建完成");
    }
    
    /**
     * 创建 telegram_channels 集合的索引
     */
    private void createTelegramChannelsIndexes() {
        String collectionName = "telegram_channels";
        log.debug("创建 {} 集合索引...", collectionName);
        
        try {
            // channelId 唯一索引
            ensureIndex(collectionName, "channelId", true, false);
            
            // monitoringStatus 索引
            ensureIndex(collectionName, "monitoringStatus", false, false);
            
            log.debug("✅ {} 集合索引创建完成", collectionName);
        } catch (Exception e) {
            log.error("❌ {} 集合索引创建失败: {}", collectionName, e.getMessage(), e);
        }
    }
    
    /**
     * 创建 raw_messages 集合的索引
     */
    private void createRawMessagesIndexes() {
        String collectionName = "raw_messages";
        log.debug("创建 {} 集合索引...", collectionName);
        
        try {
            // chatId 索引
            ensureIndex(collectionName, "chatId", false, false);
            
            // messageId 索引
            ensureIndex(collectionName, "messageId", false, false);
            
            // mediaAlbumId 索引
            ensureIndex(collectionName, "mediaAlbumId", false, false);
            
            // 复合索引：chatId + messageId（唯一）
            ensureCompoundIndex(collectionName, "chat_message_unique", 
                new String[]{"chatId", "messageId"}, 
                new Sort.Direction[]{Sort.Direction.ASC, Sort.Direction.ASC}, 
                true, false);
            
            // 复合索引：chatId + mediaAlbumId（唯一，稀疏）
            ensureCompoundIndex(collectionName, "chat_album_unique", 
                new String[]{"chatId", "mediaAlbumId"}, 
                new Sort.Direction[]{Sort.Direction.ASC, Sort.Direction.ASC}, 
                true, true);
            
            // 复合索引：chatId + date
            ensureCompoundIndex(collectionName, "chat_date_idx", 
                new String[]{"chatId", "date"}, 
                new Sort.Direction[]{Sort.Direction.ASC, Sort.Direction.DESC}, 
                false, false);
            
            log.debug("✅ {} 集合索引创建完成", collectionName);
        } catch (Exception e) {
            log.error("❌ {} 集合索引创建失败: {}", collectionName, e.getMessage(), e);
        }
    }
    
    /**
     * 创建 channel_messages 集合的索引
     */
    private void createChannelMessagesIndexes() {
        String collectionName = "channel_messages";
        log.debug("创建 {} 集合索引...", collectionName);
        
        try {
            // messageId 索引
            ensureIndex(collectionName, "messageId", false, false);
            
            // chatId 索引
            ensureIndex(collectionName, "chatId", false, false);
            
            // mediaAlbumId 索引
            ensureIndex(collectionName, "mediaAlbumId", false, false);
            
            // 复合索引：chatId + messageId（唯一）
            ensureCompoundIndex(collectionName, "chat_message_unique", 
                new String[]{"chatId", "messageId"}, 
                new Sort.Direction[]{Sort.Direction.ASC, Sort.Direction.ASC}, 
                true, false);
            
            // 复合索引：chatId + date
            ensureCompoundIndex(collectionName, "chat_date_idx", 
                new String[]{"chatId", "date"}, 
                new Sort.Direction[]{Sort.Direction.ASC, Sort.Direction.DESC}, 
                false, false);
            
            // 复合索引：chatId + mediaAlbumId + date
            ensureCompoundIndex(collectionName, "media_album_idx", 
                new String[]{"chatId", "mediaAlbumId", "date"}, 
                new Sort.Direction[]{Sort.Direction.ASC, Sort.Direction.ASC, Sort.Direction.ASC}, 
                false, false);
            
            log.debug("✅ {} 集合索引创建完成", collectionName);
        } catch (Exception e) {
            log.error("❌ {} 集合索引创建失败: {}", collectionName, e.getMessage(), e);
        }
    }
    
    /**
     * 创建 tag_authors 集合的索引
     */
    private void createTagAuthorsIndexes() {
        String collectionName = "tag_authors";
        log.debug("创建 {} 集合索引...", collectionName);
        
        try {
            // name 唯一索引
            ensureIndex(collectionName, "name", true, false);
            
            // aliases 索引
            ensureIndex(collectionName, "aliases", false, false);
            
            log.debug("✅ {} 集合索引创建完成", collectionName);
        } catch (Exception e) {
            log.error("❌ {} 集合索引创建失败: {}", collectionName, e.getMessage(), e);
        }
    }
    
    /**
     * 创建 tag_works 集合的索引
     */
    private void createTagWorksIndexes() {
        String collectionName = "tag_works";
        log.debug("创建 {} 集合索引...", collectionName);
        
        try {
            // name 唯一索引
            ensureIndex(collectionName, "name", true, false);
            
            // aliases 索引
            ensureIndex(collectionName, "aliases", false, false);
            
            log.debug("✅ {} 集合索引创建完成", collectionName);
        } catch (Exception e) {
            log.error("❌ {} 集合索引创建失败: {}", collectionName, e.getMessage(), e);
        }
    }
    
    /**
     * 创建 tag_characters 集合的索引
     */
    private void createTagCharactersIndexes() {
        String collectionName = "tag_characters";
        log.debug("创建 {} 集合索引...", collectionName);
        
        try {
            // name 唯一索引
            ensureIndex(collectionName, "name", true, false);
            
            // aliases 索引
            ensureIndex(collectionName, "aliases", false, false);
            
            // workId 索引
            ensureIndex(collectionName, "workId", false, false);
            
            log.debug("✅ {} 集合索引创建完成", collectionName);
        } catch (Exception e) {
            log.error("❌ {} 集合索引创建失败: {}", collectionName, e.getMessage(), e);
        }
    }
    
    /**
     * 创建 tag_filter_configs_v2 集合的索引
     */
    private void createTagFilterConfigsIndexes() {
        String collectionName = "tag_filter_configs_v2";
        log.debug("创建 {} 集合索引...", collectionName);
        
        try {
            // 此集合没有额外的索引定义，只有默认的 _id 索引
            // 但仍然需要访问 indexOps 以保持处理顺序的一致性
            mongoTemplate.indexOps(collectionName).getIndexInfo();
            
            log.debug("✅ {} 集合索引创建完成（无额外索引）", collectionName);
        } catch (Exception e) {
            log.error("❌ {} 集合索引创建失败: {}", collectionName, e.getMessage(), e);
        }
    }
    
    /**
     * 创建 forward_queue 集合的索引
     */
    private void createForwardQueueIndexes() {
        String collectionName = "forward_queue";
        log.debug("创建 {} 集合索引...", collectionName);
        
        try {
            // status 索引
            ensureIndex(collectionName, "status", false, false);
            
            // createTime 索引
            ensureIndex(collectionName, "createTime", false, false);
            
            // 复合索引：sourceChatId + sourceMessageId（唯一）
            ensureCompoundIndex(collectionName, "idx_source_unique", 
                new String[]{"sourceChatId", "sourceMessageId"}, 
                new Sort.Direction[]{Sort.Direction.ASC, Sort.Direction.ASC}, 
                true, false);
            
            // 复合索引：status + createTime
            ensureCompoundIndex(collectionName, "idx_status_createTime", 
                new String[]{"status", "createTime"}, 
                new Sort.Direction[]{Sort.Direction.ASC, Sort.Direction.ASC}, 
                false, false);
            
            log.debug("✅ {} 集合索引创建完成", collectionName);
        } catch (Exception e) {
            log.error("❌ {} 集合索引创建失败: {}", collectionName, e.getMessage(), e);
        }
    }
    
    /**
     * 创建 processed_messages 集合的索引
     */
    private void createProcessedMessagesIndexes() {
        String collectionName = "processed_messages";
        log.debug("创建 {} 集合索引...", collectionName);
        
        try {
            // chatId 索引
            ensureIndex(collectionName, "chatId", false, false);
            
            // messageId 索引
            ensureIndex(collectionName, "messageId", false, false);
            
            // isRead 索引
            ensureIndex(collectionName, "isRead", false, false);
            
            // isMatched 索引
            ensureIndex(collectionName, "isMatched", false, false);
            
            // processTime 索引
            ensureIndex(collectionName, "processTime", false, false);
            
            // 复合索引：chatId + messageId（唯一）
            ensureCompoundIndex(collectionName, "chat_message_unique", 
                new String[]{"chatId", "messageId"}, 
                new Sort.Direction[]{Sort.Direction.ASC, Sort.Direction.ASC}, 
                true, false);
            
            log.debug("✅ {} 集合索引创建完成", collectionName);
        } catch (Exception e) {
            log.error("❌ {} 集合索引创建失败: {}", collectionName, e.getMessage(), e);
        }
    }
    
    /**
     * 创建 unread_messages_buffer 集合的索引
     */
    private void createUnreadMessagesBufferIndexes() {
        String collectionName = "unread_messages_buffer";
        log.debug("创建 {} 集合索引...", collectionName);
        
        try {
            // chatId 索引
            ensureIndex(collectionName, "chatId", false, false);
            
            // messageId 索引
            ensureIndex(collectionName, "messageId", false, false);
            
            // fetchTime 索引
            ensureIndex(collectionName, "fetchTime", false, false);
            
            // status 索引
            ensureIndex(collectionName, "status", false, false);
            
            // createTime 索引
            ensureIndex(collectionName, "createTime", false, false);
            
            // 复合索引：chatId + messageId（唯一）
            ensureCompoundIndex(collectionName, "idx_chat_message_unique", 
                new String[]{"chatId", "messageId"}, 
                new Sort.Direction[]{Sort.Direction.ASC, Sort.Direction.ASC}, 
                true, false);
            
            // 复合索引：status + fetchTime
            ensureCompoundIndex(collectionName, "idx_status_fetchTime", 
                new String[]{"status", "fetchTime"}, 
                new Sort.Direction[]{Sort.Direction.ASC, Sort.Direction.ASC}, 
                false, false);
            
            log.debug("✅ {} 集合索引创建完成", collectionName);
        } catch (Exception e) {
            log.error("❌ {} 集合索引创建失败: {}", collectionName, e.getMessage(), e);
        }
    }
    
    /**
     * 检查初始数据
     * <p>
     * 检查 telegram_channels 集合是否为空，如果为空则记录警告信息
     * </p>
     */
    private void checkInitialData() {
        log.info("检查初始数据...");
        
        try {
            long count = mongoTemplate.count(new Query(), "telegram_channels");
            
            if (count == 0) {
                log.warn("⚠️ telegram_channels 集合为空，请添加监控频道");
            } else {
                log.info("✅ telegram_channels 集合包含 {} 条记录", count);
            }
        } catch (Exception e) {
            log.error("❌ 检查初始数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 确保单字段索引存在（幂等性）
     * <p>
     * 如果索引已存在，则跳过创建；如果不存在，则创建索引
     * </p>
     *
     * @param collectionName 集合名称
     * @param fieldName 字段名称
     * @param unique 是否唯一索引
     * @param sparse 是否稀疏索引
     */
    private void ensureIndex(String collectionName, String fieldName, boolean unique, boolean sparse) {
        try {
            // 构建索引名称
            String indexName = fieldName;
            
            // 检查索引是否已存在
            if (indexExists(collectionName, indexName)) {
                log.debug("索引 {} 已存在，跳过创建", indexName);
                return;
            }
            
            // 创建索引
            Index index = new Index().on(fieldName, Sort.Direction.ASC).named(indexName);
            
            if (unique) {
                index = index.unique();
            }
            
            if (sparse) {
                index = index.sparse();
            }
            
            mongoTemplate.indexOps(collectionName).createIndex(index);
            log.debug("✅ 创建索引: {}.{}", collectionName, indexName);
            
        } catch (Exception e) {
            log.error("❌ 创建索引失败: {}.{} - {}", collectionName, fieldName, e.getMessage());
        }
    }
    
    /**
     * 确保复合索引存在（幂等性）
     * <p>
     * 如果索引已存在，则跳过创建；如果不存在，则创建索引
     * </p>
     *
     * @param collectionName 集合名称
     * @param indexName 索引名称
     * @param fieldNames 字段名称数组
     * @param directions 排序方向数组
     * @param unique 是否唯一索引
     * @param sparse 是否稀疏索引
     */
    private void ensureCompoundIndex(String collectionName, String indexName, 
                                    String[] fieldNames, Sort.Direction[] directions, 
                                    boolean unique, boolean sparse) {
        try {
            // 检查索引是否已存在
            if (indexExists(collectionName, indexName)) {
                log.debug("索引 {} 已存在，跳过创建", indexName);
                return;
            }
            
            // 创建复合索引
            Index index = new Index().named(indexName);
            
            for (int i = 0; i < fieldNames.length; i++) {
                index = index.on(fieldNames[i], directions[i]);
            }
            
            if (unique) {
                index = index.unique();
            }
            
            if (sparse) {
                index = index.sparse();
            }
            
            mongoTemplate.indexOps(collectionName).createIndex(index);
            log.debug("✅ 创建复合索引: {}.{}", collectionName, indexName);
            
        } catch (Exception e) {
            log.error("❌ 创建复合索引失败: {}.{} - {}", collectionName, indexName, e.getMessage());
        }
    }
    
    /**
     * 检查索引是否存在
     *
     * @param collectionName 集合名称
     * @param indexName 索引名称
     * @return 索引是否存在
     */
    private boolean indexExists(String collectionName, String indexName) {
        try {
            return mongoTemplate.indexOps(collectionName)
                .getIndexInfo()
                .stream()
                .anyMatch(indexInfo -> indexInfo.getName().equals(indexName));
        } catch (Exception e) {
            log.debug("检查索引是否存在时出错: {}.{} - {}", collectionName, indexName, e.getMessage());
            return false;
        }
    }
}
