package org.xlyo.cocomonyab.source.unread.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 未读消息检测结果
 * 
 * 记录每次未读消息检测的统计信息，包括检测时间、频道数量、消息数量等。
 * 
 * 验证需求：10.1, 10.2, 10.3, 10.4
 */
@Data
public class UnreadMessageDetectionResult {
    
    /**
     * 检测开始时间
     * 需求：10.1
     */
    private LocalDateTime startTime;
    
    /**
     * 检测结束时间
     * 需求：10.1
     */
    private LocalDateTime endTime;
    
    /**
     * 总频道数
     * 需求：10.2
     */
    private int totalChannels;
    
    /**
     * 成功处理的频道数
     * 需求：10.2
     */
    private int successChannels;
    
    /**
     * 失败的频道数
     * 需求：10.2
     */
    private int failedChannels;
    
    /**
     * 检测到的未读消息总数
     * 需求：10.3
     */
    private int totalUnreadMessages;
    
    /**
     * 成功处理的消息数量
     * 需求：10.3
     */
    private int processedMessages;
    
    /**
     * 处理失败的消息数量
     * 需求：10.4
     */
    private int failedMessages;
    
    /**
     * 增加成功频道计数
     * 需求：10.2
     */
    public void incrementSuccessChannels() {
        this.successChannels++;
    }
    
    /**
     * 增加失败频道计数
     * 需求：10.2
     */
    public void incrementFailedChannels() {
        this.failedChannels++;
    }
    
    /**
     * 添加未读消息数量
     * 需求：10.3
     * 
     * @param count 要添加的消息数量
     */
    public void addUnreadMessages(int count) {
        this.totalUnreadMessages += count;
    }
    
    /**
     * 增加已处理消息计数
     * 需求：10.3
     */
    public void incrementProcessedMessages() {
        this.processedMessages++;
    }
    
    /**
     * 增加失败消息计数
     * 需求：10.4
     */
    public void incrementFailedMessages() {
        this.failedMessages++;
    }
}
