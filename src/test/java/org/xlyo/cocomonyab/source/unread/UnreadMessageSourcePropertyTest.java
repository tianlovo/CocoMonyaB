package org.xlyo.cocomonyab.source.unread;

import net.jqwik.api.*;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.source.unread.config.UnreadMessageSourceConfig;
import org.xlyo.cocomonyab.source.unread.service.UnreadMessageBufferService;
import org.xlyo.cocomonyab.source.unread.service.UnreadMessageFetchService;
import org.xlyo.cocomonyab.source.unread.service.UnreadMessageSourceService;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * UnreadMessageSource 属性测试
 * <p>
 * 使用属性测试验证未读消息来源生成器在所有输入下的正确性
 * <p>
 * 测试属性：
 * - Property 21: 生命周期往返
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
class UnreadMessageSourcePropertyTest {
    
    /**
     * Property 21: 生命周期往返
     * <p>
     * For any 消息来源实例，调用 start() 后 isRunning() 应该返回 true，
     * 调用 stop() 后 isRunning() 应该返回 false
     * <p>
     * Validates: Requirements 12.2, 12.3, 12.4
     */
    @Property(tries = 100)
    @Label("Feature: unread-channel-message-source, Property 21: 生命周期往返")
    void lifecycleRoundTrip(@ForAll boolean autoDetectOnStartup) throws Exception {
        // 创建 mock 对象
        ChannelRepository channelRepo = mock(ChannelRepository.class);
        UnreadMessageFetchService fetchService = mock(UnreadMessageFetchService.class);
        UnreadMessageBufferService bufferService = mock(UnreadMessageBufferService.class);
        
        // 创建配置
        UnreadMessageSourceConfig config = createTestConfig();
        config.setAutoDetectOnStartup(autoDetectOnStartup);
        
        // Mock 依赖
        when(channelRepo.findByMonitoringStatus(true))
            .thenReturn(new ArrayList<>());
        when(bufferService.countPendingMessages()).thenReturn(0L);
        
        // 创建服务
        UnreadMessageSourceService service = new UnreadMessageSourceService(
            channelRepo, fetchService, bufferService, config
        );
        
        // 创建消息来源
        UnreadMessageSource source = new UnreadMessageSource(service, config);
        
        // 验证：初始状态应该是未运行
        assertThat(source.isRunning())
            .as("Initial state should be not running")
            .isFalse();
        
        // 启动消息来源
        source.start();
        
        // 验证：启动后应该是运行状态
        assertThat(source.isRunning())
            .as("After start(), isRunning() should return true")
            .isTrue();
        
        // 验证：initialize 被调用
        verify(bufferService).countPendingMessages();
        
        // 验证：如果配置了自动检测，应该调用 detectUnreadMessages
        if (autoDetectOnStartup) {
            verify(channelRepo).findByMonitoringStatus(true);
        }
        
        // 停止消息来源
        source.stop();
        
        // 验证：停止后应该是未运行状态
        assertThat(source.isRunning())
            .as("After stop(), isRunning() should return false")
            .isFalse();
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
}
