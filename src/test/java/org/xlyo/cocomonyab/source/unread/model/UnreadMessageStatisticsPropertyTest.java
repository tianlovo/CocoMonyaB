package org.xlyo.cocomonyab.source.unread.model;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Assertions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * UnreadMessageStatistics 属性测试
 * 
 * 使用属性测试验证统计类的准确性在所有输入下都成立
 * 
 * Property 20: 统计准确性
 * 
 * Validates: Requirements 10.2, 10.3, 10.4
 */
class UnreadMessageStatisticsPropertyTest {
    
    /**
     * Property 20: 统计准确性
     * 
     * For any 检测结果，统计的消息总数应该等于实际检测到的消息数量，
     * 成功数+失败数应该等于总数。
     * 
     * Validates: Requirements 10.2, 10.3, 10.4
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 20: 统计准确性")
    void statisticsAccuracy(
            @ForAll @IntRange(min = 0, max = 100) int totalChannels,
            @ForAll @IntRange(min = 0, max = 100) int successChannels,
            @ForAll @IntRange(min = 0, max = 1000) int totalUnreadMessages,
            @ForAll @IntRange(min = 0, max = 1000) int processedMessages,
            @ForAll @IntRange(min = 0, max = 1000) int failedMessages) {
        
        // 确保 successChannels + failedChannels <= totalChannels
        int failedChannels = Math.min(totalChannels - successChannels, totalChannels);
        if (failedChannels < 0) {
            failedChannels = 0;
        }
        
        // 创建检测结果
        UnreadMessageDetectionResult result = new UnreadMessageDetectionResult();
        result.setStartTime(LocalDateTime.now().minusMinutes(10));
        result.setEndTime(LocalDateTime.now());
        result.setTotalChannels(totalChannels);
        result.setSuccessChannels(successChannels);
        result.setFailedChannels(failedChannels);
        result.setTotalUnreadMessages(totalUnreadMessages);
        result.setProcessedMessages(processedMessages);
        result.setFailedMessages(failedMessages);
        
        // 创建统计实例并记录结果
        UnreadMessageStatistics statistics = new UnreadMessageStatistics();
        statistics.recordDetection(result);
        
        // 验证：统计的频道数应该等于检测结果中的频道数
        Assertions.assertEquals(totalChannels, statistics.getTotalChannelsScanned(),
                "Total channels scanned should match detection result");
        
        // 验证：统计的未读消息数应该等于检测结果中的未读消息数
        Assertions.assertEquals(totalUnreadMessages, statistics.getTotalUnreadMessages(),
                "Total unread messages should match detection result");
        
        // 验证：统计的已处理消息数应该等于检测结果中的已处理消息数
        Assertions.assertEquals(processedMessages, statistics.getTotalProcessedMessages(),
                "Total processed messages should match detection result");
        
        // 验证：统计的失败消息数应该等于检测结果中的失败消息数
        Assertions.assertEquals(failedMessages, statistics.getTotalFailedMessages(),
                "Total failed messages should match detection result");
        
        // 验证：最后检测时间应该被更新
        Assertions.assertEquals(result.getEndTime(), statistics.getLastDetectionTime(),
                "Last detection time should be updated");
    }
    
    /**
     * 附加属性测试：累计统计准确性
     * 
     * 验证多次检测后的累计统计是否准确
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Additional Property: 累计统计准确性")
    void cumulativeStatisticsAccuracy(
            @ForAll @IntRange(min = 1, max = 10) int detectionCount) {
        
        UnreadMessageStatistics statistics = new UnreadMessageStatistics();
        
        long expectedTotalChannels = 0;
        long expectedTotalUnreadMessages = 0;
        long expectedTotalProcessedMessages = 0;
        long expectedTotalFailedMessages = 0;
        LocalDateTime lastEndTime = null;
        
        // 执行多次检测
        for (int i = 0; i < detectionCount; i++) {
            int channels = (i + 1) * 5;
            int unread = (i + 1) * 10;
            int processed = (i + 1) * 8;
            int failed = (i + 1) * 2;
            
            UnreadMessageDetectionResult result = new UnreadMessageDetectionResult();
            result.setStartTime(LocalDateTime.now().minusMinutes(10));
            result.setEndTime(LocalDateTime.now());
            result.setTotalChannels(channels);
            result.setSuccessChannels(channels);
            result.setFailedChannels(0);
            result.setTotalUnreadMessages(unread);
            result.setProcessedMessages(processed);
            result.setFailedMessages(failed);
            
            statistics.recordDetection(result);
            
            expectedTotalChannels += channels;
            expectedTotalUnreadMessages += unread;
            expectedTotalProcessedMessages += processed;
            expectedTotalFailedMessages += failed;
            lastEndTime = result.getEndTime();
        }
        
        // 验证：累计统计应该等于所有检测结果的总和
        Assertions.assertEquals(expectedTotalChannels, statistics.getTotalChannelsScanned(),
                "Cumulative total channels should be sum of all detections");
        Assertions.assertEquals(expectedTotalUnreadMessages, statistics.getTotalUnreadMessages(),
                "Cumulative total unread messages should be sum of all detections");
        Assertions.assertEquals(expectedTotalProcessedMessages, statistics.getTotalProcessedMessages(),
                "Cumulative total processed messages should be sum of all detections");
        Assertions.assertEquals(expectedTotalFailedMessages, statistics.getTotalFailedMessages(),
                "Cumulative total failed messages should be sum of all detections");
        
        // 验证：最后检测时间应该是最后一次检测的结束时间
        Assertions.assertEquals(lastEndTime, statistics.getLastDetectionTime(),
                "Last detection time should be from the most recent detection");
    }
    
    /**
     * 附加属性测试：线程安全性
     * 
     * 验证统计类在并发环境下的线程安全性
     */
    @Property(tries = 50)
    @Label("Feature: unread-channel-message-source, Additional Property: 线程安全性")
    void threadSafety(
            @ForAll @IntRange(min = 2, max = 10) int threadCount,
            @ForAll @IntRange(min = 1, max = 20) int detectionsPerThread) throws InterruptedException {
        
        UnreadMessageStatistics statistics = new UnreadMessageStatistics();
        List<Thread> threads = new ArrayList<>();
        
        // 创建多个线程同时记录检测结果
        for (int t = 0; t < threadCount; t++) {
            Thread thread = new Thread(() -> {
                for (int i = 0; i < detectionsPerThread; i++) {
                    UnreadMessageDetectionResult result = new UnreadMessageDetectionResult();
                    result.setStartTime(LocalDateTime.now().minusMinutes(10));
                    result.setEndTime(LocalDateTime.now());
                    result.setTotalChannels(5);
                    result.setSuccessChannels(5);
                    result.setFailedChannels(0);
                    result.setTotalUnreadMessages(10);
                    result.setProcessedMessages(8);
                    result.setFailedMessages(2);
                    
                    statistics.recordDetection(result);
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证：统计结果应该等于所有线程的总和
        long expectedTotal = (long) threadCount * detectionsPerThread;
        Assertions.assertEquals(expectedTotal * 5, statistics.getTotalChannelsScanned(),
                "Total channels should be correct after concurrent updates");
        Assertions.assertEquals(expectedTotal * 10, statistics.getTotalUnreadMessages(),
                "Total unread messages should be correct after concurrent updates");
        Assertions.assertEquals(expectedTotal * 8, statistics.getTotalProcessedMessages(),
                "Total processed messages should be correct after concurrent updates");
        Assertions.assertEquals(expectedTotal * 2, statistics.getTotalFailedMessages(),
                "Total failed messages should be correct after concurrent updates");
    }
    
    /**
     * 附加属性测试：零值处理
     * 
     * 验证统计类正确处理零值输入
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Additional Property: 零值处理")
    void zeroValueHandling() {
        
        UnreadMessageDetectionResult result = new UnreadMessageDetectionResult();
        result.setStartTime(LocalDateTime.now().minusMinutes(10));
        result.setEndTime(LocalDateTime.now());
        result.setTotalChannels(0);
        result.setSuccessChannels(0);
        result.setFailedChannels(0);
        result.setTotalUnreadMessages(0);
        result.setProcessedMessages(0);
        result.setFailedMessages(0);
        
        UnreadMessageStatistics statistics = new UnreadMessageStatistics();
        statistics.recordDetection(result);
        
        // 验证：零值应该被正确记录
        Assertions.assertEquals(0, statistics.getTotalChannelsScanned(),
                "Zero channels should be recorded correctly");
        Assertions.assertEquals(0, statistics.getTotalUnreadMessages(),
                "Zero unread messages should be recorded correctly");
        Assertions.assertEquals(0, statistics.getTotalProcessedMessages(),
                "Zero processed messages should be recorded correctly");
        Assertions.assertEquals(0, statistics.getTotalFailedMessages(),
                "Zero failed messages should be recorded correctly");
        Assertions.assertNotNull(statistics.getLastDetectionTime(),
                "Last detection time should be set even with zero values");
    }
    
    /**
     * 附加属性测试：增量方法准确性
     * 
     * 验证 incrementFailedMessages 方法的准确性
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Additional Property: 增量方法准确性")
    void incrementMethodAccuracy(
            @ForAll @IntRange(min = 1, max = 100) int incrementCount) {
        
        UnreadMessageStatistics statistics = new UnreadMessageStatistics();
        
        // 多次调用 incrementFailedMessages
        for (int i = 0; i < incrementCount; i++) {
            statistics.incrementFailedMessages();
        }
        
        // 验证：失败消息计数应该等于调用次数
        Assertions.assertEquals(incrementCount, statistics.getTotalFailedMessages(),
                "Failed messages count should equal increment count");
    }
    
    /**
     * 附加属性测试：初始状态验证
     * 
     * 验证统计类的初始状态
     */
    @Property(tries = 10)
    @Label("Feature: unread-channel-message-source, Additional Property: 初始状态验证")
    void initialStateValidation() {
        
        UnreadMessageStatistics statistics = new UnreadMessageStatistics();
        
        // 验证：初始状态所有计数器都应该为 0
        Assertions.assertEquals(0, statistics.getTotalChannelsScanned(),
                "Initial total channels should be 0");
        Assertions.assertEquals(0, statistics.getTotalUnreadMessages(),
                "Initial total unread messages should be 0");
        Assertions.assertEquals(0, statistics.getTotalProcessedMessages(),
                "Initial total processed messages should be 0");
        Assertions.assertEquals(0, statistics.getTotalFailedMessages(),
                "Initial total failed messages should be 0");
        
        // 验证：初始状态最后检测时间应该为 null
        Assertions.assertNull(statistics.getLastDetectionTime(),
                "Initial last detection time should be null");
    }
}
