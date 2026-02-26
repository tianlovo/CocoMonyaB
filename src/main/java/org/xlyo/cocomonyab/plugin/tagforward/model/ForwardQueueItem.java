package org.xlyo.cocomonyab.plugin.tagforward.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * 转发队列项数据模型
 * <p>
 * 存储待转发消息的信息，包括源频道、消息ID、匹配的标签、状态等
 * <p>
 * 注意：TTL索引需要在MongoDB初始化时单独创建（30天过期）
 */
@Document(collection = "forward_queue")
@CompoundIndex(name = "idx_source_unique", def = "{'sourceChatId': 1, 'sourceMessageId': 1}", unique = true)
@CompoundIndex(name = "idx_status_createTime", def = "{'status': 1, 'createTime': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForwardQueueItem {
    
    /**
     * 队列项ID
     */
    @Id
    private String id;
    
    /**
     * 源频道ID
     */
    private Long sourceChatId;
    
    /**
     * 源消息ID
     */
    private Long sourceMessageId;
    
    /**
     * 匹配到的标签列表
     */
    private List<String> matchedTags;
    
    /**
     * 转发状态
     */
    @Indexed
    private ForwardStatus status;
    
    /**
     * 创建时间（用于TTL索引和排序）
     */
    @Indexed
    private Instant createTime;
    
    /**
     * 更新时间
     */
    private Instant updateTime;
    
    /**
     * 转发时间（成功转发后设置）
     */
    private Instant forwardTime;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 错误消息（转发失败时记录）
     */
    private String errorMessage;
}
