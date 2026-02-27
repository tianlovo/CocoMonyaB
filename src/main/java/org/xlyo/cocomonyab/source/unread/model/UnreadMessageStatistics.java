package org.xlyo.cocomonyab.source.unread.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 未读消息统计信息
 * <p>
 * 使用 AtomicLong 实现线程安全的统计计数器，记录未读消息检测的累计统计信息。
 */
public class UnreadMessageStatistics {
    
    /**
     * 扫描的频道总数
     */
    private final AtomicLong totalChannelsScanned = new AtomicLong(0);
    
    /**
     * 检测到的未读消息总数
     */
    private final AtomicLong totalUnreadMessages = new AtomicLong(0);
    
    /**
     * 成功处理的消息总数
     */
    private final AtomicLong totalProcessedMessages = new AtomicLong(0);
    
    /**
     * 处理失败的消息总数
     */
    private final AtomicLong totalFailedMessages = new AtomicLong(0);
    
    /**
     * 最后一次检测时间
     */
    @Getter
    private volatile LocalDateTime lastDetectionTime;
    
    /**
     * 记录一次检测结果，更新统计信息
     * 
     * @param result 检测结果
     */
    public void recordDetection(UnreadMessageDetectionResult result) {
        totalChannelsScanned.addAndGet(result.getTotalChannels());
        totalUnreadMessages.addAndGet(result.getTotalUnreadMessages());
        totalProcessedMessages.addAndGet(result.getProcessedMessages());
        totalFailedMessages.addAndGet(result.getFailedMessages());
        lastDetectionTime = result.getEndTime();
    }
    
    /**
     * 获取扫描的频道总数
     * 
     * @return 扫描的频道总数
     */
    public long getTotalChannelsScanned() {
        return totalChannelsScanned.get();
    }
    
    /**
     * 获取检测到的未读消息总数
     * 
     * @return 未读消息总数
     */
    public long getTotalUnreadMessages() {
        return totalUnreadMessages.get();
    }
    
    /**
     * 获取成功处理的消息总数
     * 
     * @return 已处理消息总数
     */
    public long getTotalProcessedMessages() {
        return totalProcessedMessages.get();
    }
    
    /**
     * 获取处理失败的消息总数
     * 
     * @return 失败消息总数
     */
    public long getTotalFailedMessages() {
        return totalFailedMessages.get();
    }
    
    /**
     * 增加失败消息计数
     */
    public void incrementFailedMessages() {
        totalFailedMessages.incrementAndGet();
    }
}
