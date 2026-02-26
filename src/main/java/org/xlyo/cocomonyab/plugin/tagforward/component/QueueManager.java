package org.xlyo.cocomonyab.plugin.tagforward.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardQueueItem;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardStatus;

import java.time.Instant;
import java.util.List;

/**
 * 队列管理器
 * <p>
 * 负责管理转发队列，包括消息入队、状态更新、重试计数等操作
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QueueManager {
    
    private final MongoTemplate mongoTemplate;
    
    /**
     * 将消息加入转发队列
     * 
     * @param sourceChatId 源频道ID
     * @param sourceMessageId 源消息ID
     * @param matchedTags 匹配到的标签列表
     */
    public void enqueue(Long sourceChatId, Long sourceMessageId, List<String> matchedTags) {
        ForwardQueueItem item = ForwardQueueItem.builder()
                .sourceChatId(sourceChatId)
                .sourceMessageId(sourceMessageId)
                .matchedTags(matchedTags)
                .status(ForwardStatus.PENDING)
                .createTime(Instant.now())
                .retryCount(0)
                .build();
        
        try {
            mongoTemplate.insert(item, "forward_queue");
            log.debug("消息已加入队列: chatId={}, messageId={}, tags={}", 
                    sourceChatId, sourceMessageId, matchedTags);
        } catch (DuplicateKeyException e) {
            log.debug("消息已在队列中: chatId={}, messageId={}", 
                    sourceChatId, sourceMessageId);
        }
    }
    
    /**
     * 获取待处理的消息列表
     * 
     * @param limit 最大返回数量
     * @return 待处理消息列表，按创建时间升序排序
     */
    public List<ForwardQueueItem> getPendingItems(int limit) {
        Query query = Query.query(Criteria.where("status").is(ForwardStatus.PENDING))
                .with(Sort.by(Sort.Direction.ASC, "createTime"))
                .limit(limit);
        
        List<ForwardQueueItem> items = mongoTemplate.find(query, ForwardQueueItem.class, "forward_queue");
        log.debug("从队列中获取了 {} 个待处理项", items.size());
        
        return items;
    }
    
    /**
     * 更新队列项状态
     * 
     * @param itemId 队列项ID
     * @param status 新状态
     * @param errorMessage 错误消息（可选）
     */
    public void updateStatus(String itemId, ForwardStatus status, String errorMessage) {
        Update update = new Update()
                .set("status", status)
                .set("updateTime", Instant.now());
        
        if (status == ForwardStatus.SUCCESS) {
            update.set("forwardTime", Instant.now());
        }
        
        if (errorMessage != null) {
            update.set("errorMessage", errorMessage);
        }
        
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(itemId)),
                update,
                "forward_queue"
        );
        
        log.debug("队列项状态已更新: id={}, status={}", itemId, status);
    }
    
    /**
     * 递增重试计数
     * 
     * @param itemId 队列项ID
     */
    public void incrementRetryCount(String itemId) {
        Update update = new Update()
                .inc("retryCount", 1)
                .set("updateTime", Instant.now());
        
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(itemId)),
                update,
                "forward_queue"
        );
        
        log.debug("队列项重试次数已递增: id={}", itemId);
    }
}
