package org.xlyo.cocomonyab.source.unread.service;

import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.Size;
import org.junit.jupiter.api.Assertions;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.source.unread.config.UnreadMessageSourceConfig;
import org.xlyo.cocomonyab.source.unread.model.UnreadMessageDetectionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * UnreadMessageSourceService 属性测�?
 * <p>
 * 使用属性测试验证未读消息来源服务在所有输入下的正确�?
 * <p>
 * 测试属性：
 * - Property 19: 并发检测互�?
 * - Property 1: 监控频道过滤
 * - Property 2: 全频道覆�?
 * - Property 18: 错误隔离
 * 
 * @author tianluoqaq
 * @since 1.0
 */
class UnreadMessageSourceServicePropertyTest {
    
    /**
     * Property 19: 并发检测互�?
     * <p>
     * For any 时刻，最多只能有一个未读消息检测任务在运行
     * <p>
     * Validates: Requirement 9.3
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 19: 并发检测互�?)
    void concurrentDetectionMutualExclusion(
            @ForAll @IntRange(min = 2, max = 10) int concurrentAttempts) throws Exception {
        
        // 创建 mock 对象
        ChannelRepository channelRepo = mock(ChannelRepository.class);
        UnreadMessageFetchService fetchService = mock(UnreadMessageFetchService.class);
        UnreadMessageSourceBufferService bufferService = mock(UnreadMessageSourceBufferService.class);
        UnreadMessageSourceConfig config = createTestConfig();
        
        // Mock 返回空频道列表（简化测试）
        when(channelRepo.findByMonitoringStatus(true))
            .thenReturn(new ArrayList<>());
        when(bufferService.countPendingMessages()).thenReturn(0L);
        
        // 创建服务实例
        UnreadMessageSourceService service = new UnreadMessageSourceService(
            channelRepo, fetchService, bufferService, config
        );
        
        // 使用 CountDownLatch 同步多个线程
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrentAttempts);
        
        // 记录成功和失败的次数
        List<Boolean> results = new CopyOnWriteArrayList<>();
        
        // 创建多个线程同时尝试检�?
        ExecutorService executor = Executors.newFixedThreadPool(concurrentAttempts);
        
        for (int i = 0; i < concurrentAttempts; i++) {
            executor.submit(() -> {
                try {
                    // 等待所有线程就�?
                    startLatch.await();
                    
                    // 尝试检�?
                    service.detectUnreadMessages();
                    results.add(true); // 成功
                    
                } catch (IllegalStateException e) {
                    // 预期的并发冲突异�?
                    results.add(false); // 失败（被阻止�?
                } catch (Exception e) {
                    // 其他异常
                    results.add(false);
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        // 启动所有线�?
        startLatch.countDown();
        
        // 等待所有线程完�?
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        
        Assertions.assertTrue(completed, "All threads should complete within timeout");
        
        // 验证：只有一个线程成功执�?
        long successCount = results.stream().filter(r -> r).count();
        assertThat(successCount)
            .as("Only one thread should successfully execute detection")
            .isEqualTo(1);
        
        // 验证：其他线程被阻止
        long failedCount = results.stream().filter(r -> !r).count();
        assertThat(failedCount)
            .as("Other threads should be blocked")
            .isEqualTo(concurrentAttempts - 1);
    }
    
    /**
     * Property 1: 监控频道过滤
     * <p>
     * For any 频道列表，获取监控频道时应该只返�?monitoringStatus �?true 的频�?
     * <p>
     * Validates: Requirement 1.2
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 1: 监控频道过滤")
    void monitoringChannelFilter(
            @ForAll @Size(min = 0, max = 50) List<@From("channels") Channel> channels) {
        
        // 创建 mock 对象
        ChannelRepository channelRepo = mock(ChannelRepository.class);
        UnreadMessageFetchService fetchService = mock(UnreadMessageFetchService.class);
        UnreadMessageSourceBufferService bufferService = mock(UnreadMessageSourceBufferService.class);
        UnreadMessageSourceConfig config = createTestConfig();
        
        // 过滤出监控频�?
        List<Channel> monitoringChannels = channels.stream()
            .filter(Channel::getMonitoringStatus)
            .collect(Collectors.toList());
        
        // Mock repository 返回监控频道
        when(channelRepo.findByMonitoringStatus(true))
            .thenReturn(monitoringChannels);
        when(bufferService.countPendingMessages()).thenReturn(0L);
        
        // Mock fetchService 返回空列�?
        when(fetchService.fetchUnreadMessages(anyLong()))
            .thenReturn(new ArrayList<>());
        
        // 创建服务实例
        UnreadMessageSourceService service = new UnreadMessageSourceService(
            channelRepo, fetchService, bufferService, config
        );
        
        // 执行检�?
        UnreadMessageDetectionResult result = service.detectUnreadMessages();
        
        // 验证：调用了 findByMonitoringStatus(true)
        verify(channelRepo).findByMonitoringStatus(true);
        
        // 验证：处理的频道数等于监控频道数
        assertThat(result.getTotalChannels())
            .as("Total channels should equal monitoring channels count")
            .isEqualTo(monitoringChannels.size());
        
        // 验证：所有监控频道都被处�?
        verify(fetchService, times(monitoringChannels.size()))
            .fetchUnreadMessages(anyLong());
    }
    
    /**
     * Property 2: 全频道覆�?
     * <p>
     * For any 监控频道列表，检测过程应该查询每一个频道的未读消息
     * <p>
     * Validates: Requirement 1.3
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 2: 全频道覆�?)
    void allChannelsCoverage(
            @ForAll @Size(min = 1, max = 20) List<@From("monitoringChannels") Channel> channels) {
        
        // 创建 mock 对象
        ChannelRepository channelRepo = mock(ChannelRepository.class);
        UnreadMessageFetchService fetchService = mock(UnreadMessageFetchService.class);
        UnreadMessageSourceBufferService bufferService = mock(UnreadMessageSourceBufferService.class);
        UnreadMessageSourceConfig config = createTestConfig();
        
        // Mock repository 返回监控频道
        when(channelRepo.findByMonitoringStatus(true))
            .thenReturn(channels);
        when(bufferService.countPendingMessages()).thenReturn(0L);
        
        // Mock fetchService 返回空列�?
        when(fetchService.fetchUnreadMessages(anyLong()))
            .thenReturn(new ArrayList<>());
        
        // 创建服务实例
        UnreadMessageSourceService service = new UnreadMessageSourceService(
            channelRepo, fetchService, bufferService, config
        );
        
        // 执行检�?
        UnreadMessageDetectionResult result = service.detectUnreadMessages();
        
        // 验证：每个频道都被查�?
        for (Channel channel : channels) {
            verify(fetchService).fetchUnreadMessages(channel.getChannelId());
        }
        
        // 验证：查询次数等于频道数
        verify(fetchService, times(channels.size()))
            .fetchUnreadMessages(anyLong());
        
        // 验证：成功频道数等于总频道数（没有错误）
        assertThat(result.getSuccessChannels())
            .as("All channels should be processed successfully")
            .isEqualTo(channels.size());
    }
    
    /**
     * Property 18: 错误隔离
     * <p>
     * For any 频道列表，即使某个频道处理失败，其他频道仍应该被处理
     * <p>
     * Validates: Requirement 8.5
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 18: 错误隔离")
    void errorIsolation(
            @ForAll @Size(min = 3, max = 10) List<@From("monitoringChannels") Channel> channels,
            @ForAll @IntRange(min = 0, max = 2) int failingChannelIndex) {
        
        // 确保 failingChannelIndex 在有效范围内
        int actualFailingIndex = failingChannelIndex % channels.size();
        
        // 创建 mock 对象
        ChannelRepository channelRepo = mock(ChannelRepository.class);
        UnreadMessageFetchService fetchService = mock(UnreadMessageFetchService.class);
        UnreadMessageSourceBufferService bufferService = mock(UnreadMessageSourceBufferService.class);
        UnreadMessageSourceConfig config = createTestConfig();
        
        // Mock repository 返回监控频道
        when(channelRepo.findByMonitoringStatus(true))
            .thenReturn(channels);
        when(bufferService.countPendingMessages()).thenReturn(0L);
        
        // Mock fetchService：某个频道抛出异常，其他返回空列�?
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = channels.get(i);
            if (i == actualFailingIndex) {
                // 这个频道抛出异常
                when(fetchService.fetchUnreadMessages(channel.getChannelId()))
                    .thenThrow(new RuntimeException("Test error for channel " + channel.getChannelId()));
            } else {
                // 其他频道正常返回
                when(fetchService.fetchUnreadMessages(channel.getChannelId()))
                    .thenReturn(new ArrayList<>());
            }
        }
        
        // 创建服务实例
        UnreadMessageSourceService service = new UnreadMessageSourceService(
            channelRepo, fetchService, bufferService, config
        );
        
        // 执行检�?
        UnreadMessageDetectionResult result = service.detectUnreadMessages();
        
        // 验证：所有频道都被尝试处�?
        verify(fetchService, times(channels.size()))
            .fetchUnreadMessages(anyLong());
        
        // 验证：失败频道数�?1
        assertThat(result.getFailedChannels())
            .as("One channel should fail")
            .isEqualTo(1);
        
        // 验证：成功频道数�?总数 - 1
        assertThat(result.getSuccessChannels())
            .as("Other channels should succeed")
            .isEqualTo(channels.size() - 1);
        
        // 验证：总频道数正确
        assertThat(result.getTotalChannels())
            .as("Total channels should be correct")
            .isEqualTo(channels.size());
    }
    
    /**
     * 创建测试配置
     */
    private UnreadMessageSourceConfig createTestConfig() {
        UnreadMessageSourceConfig config = new UnreadMessageSourceConfig();
        config.setAutoDetectOnStartup(true);
        config.setMaxMessagesPerFetch(100);
        config.setMaxTotalMessages(1000);
        config.setApiCallDelay(100L);
        config.setMaxRetries(3);
        config.setRetryBaseDelay(1000L);
        config.setRetryMaxDelay(10000L);
        config.setBatchSize(10);
        config.setBatchDelay(100L);
        config.setBufferTtlDays(7);
        return config;
    }
    
    /**
     * 生成随机频道
     */
    @Provide
    Arbitrary<Channel> channels() {
        return Combinators.combine(
            Arbitraries.longs().between(-1000000000000L, -1L),
            Arbitraries.strings().alpha().ofLength(10),
            Arbitraries.strings().alpha().ofLength(20),
            Arbitraries.of(true, false)
        ).as((id, username, title, monitoring) -> {
            Channel channel = new Channel();
            channel.setChannelId(id);
            channel.setChannelUsername(username);
            channel.setChannelTitle(title);
            channel.setMonitoringStatus(monitoring);
            return channel;
        });
    }
    
    /**
     * 生成随机监控频道（monitoringStatus = true�?
     */
    @Provide
    Arbitrary<Channel> monitoringChannels() {
        return Combinators.combine(
            Arbitraries.longs().between(-1000000000000L, -1L),
            Arbitraries.strings().alpha().ofLength(10),
            Arbitraries.strings().alpha().ofLength(20)
        ).as((id, username, title) -> {
            Channel channel = new Channel();
            channel.setChannelId(id);
            channel.setChannelUsername(username);
            channel.setChannelTitle(title);
            channel.setMonitoringStatus(true); // 固定�?true
            return channel;
        });
    }
}
