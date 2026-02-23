package org.xlyo.cocomonyab.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.xlyo.cocomonyab.config.ConcurrentSafetyProperties;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.filter.FilterChainManager;
import org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter;
import org.xlyo.cocomonyab.plugin.PluginManager;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.service.message.MessageParser;
import org.xlyo.cocomonyab.service.message.MessageStorageService;
import org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.xlyo.cocomonyab.config.ConcurrentSafetyProperties;

/**
 * 日志记录验证测试
 * 验证需求 9.1, 9.2, 9.3, 9.4, 9.5
 */
class LoggingVerificationTest {

    private ChannelMonitorService channelMonitorService;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private MessageStorageService messageStorageService;
    @Mock
    private MessageParser messageParser;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private FilterChainManager filterChainManager;
    @Mock
    private ChannelMonitoringFilter channelMonitoringFilter;
    @Mock
    private MediaGroupMetrics mediaGroupMetrics;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 设置日志捕获
        logger = (Logger) LoggerFactory.getLogger(ChannelMonitorService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        
        // 设置 mock 行为
        when(filterChainManager.executeChain(any())).thenReturn(true);
        when(channelRepository.findByChannelId(any())).thenReturn(Optional.of(new Channel()));
        when(messageStorageService.saveMessage(any())).thenReturn(true);
        
        // 创建配置
        ConcurrentSafetyProperties properties = new ConcurrentSafetyProperties();
        properties.getMediaGroup().setTimeout(2000);
        properties.getMediaGroup().setMaxBufferSize(1000);
        properties.getLock().setStripes(128);
        properties.getLock().setTimeout(5000);
        properties.getCache().setTtl(10);
        properties.getCache().setMaxSize(10000);
        properties.getCache().setFailedMessageTtl(5);
        
        // 创建服务实例
        channelMonitorService = new ChannelMonitorService(
            channelRepository,
            messageStorageService,
            messageParser,
            pluginManager,
            filterChainManager,
            channelMonitoringFilter,
            mediaGroupMetrics,
            properties
        );
        channelMonitorService.initMetrics();
    }

    @AfterEach
    void tearDown() {
        // 清理日志捕获
        logger.detachAppender(listAppender);
    }

    /**
     * 测试状态转换日志（需求 9.1）
     * 验证状态转换时记录 groupKey、oldState、newState
     */
    @Test
    void testStateTransitionLogging() throws InterruptedException {
        // 创建测试消息
        TdApi.Message message = createTestMessage(1001L, 1L, 5001L);

        // 添加消息到媒体组
        channelMonitorService.handleMediaGroupMessage(message);

        // 等待超时处理
        Thread.sleep(2500);

        // 验证日志
        List<ILoggingEvent> logsList = listAppender.list;
        
        // 验证 NONE -> COLLECTING 状态转换日志
        boolean foundCollectingTransition = logsList.stream()
            .anyMatch(event -> 
                event.getLevel() == Level.INFO &&
                event.getFormattedMessage().contains("媒体组状态转换") &&
                event.getFormattedMessage().contains("oldState=NONE") &&
                event.getFormattedMessage().contains("newState=COLLECTING") &&
                event.getFormattedMessage().contains("groupKey=1001:5001")
            );
        assertTrue(foundCollectingTransition, "应该记录 NONE -> COLLECTING 状态转换");

        // 验证 COLLECTING -> PROCESSING 状态转换日志
        boolean foundProcessingTransition = logsList.stream()
            .anyMatch(event -> 
                event.getLevel() == Level.INFO &&
                event.getFormattedMessage().contains("媒体组状态转换") &&
                event.getFormattedMessage().contains("oldState=COLLECTING") &&
                event.getFormattedMessage().contains("newState=PROCESSING")
            );
        assertTrue(foundProcessingTransition, "应该记录 COLLECTING -> PROCESSING 状态转换");

        // 验证 PROCESSING -> COMPLETED 状态转换日志
        boolean foundCompletedTransition = logsList.stream()
            .anyMatch(event -> 
                event.getLevel() == Level.INFO &&
                event.getFormattedMessage().contains("媒体组状态转换") &&
                event.getFormattedMessage().contains("oldState=PROCESSING") &&
                event.getFormattedMessage().contains("newState=COMPLETED")
            );
        assertTrue(foundCompletedTransition, "应该记录 PROCESSING -> COMPLETED 状态转换");
    }

    /**
     * 测试消息拒绝日志（需求 9.2）
     * 验证消息被拒绝时记录 groupKey、messageId、currentState、reason
     */
    @Test
    void testMessageRejectionLogging() throws InterruptedException {
        // 创建测试消息
        TdApi.Message message1 = createTestMessage(1002L, 2L, 5002L);
        TdApi.Message message2 = createTestMessage(1002L, 3L, 5002L);

        // 添加第一条消息
        channelMonitorService.handleMediaGroupMessage(message1);

        // 等待超时并处理
        Thread.sleep(2500);

        // 清空之前的日志
        listAppender.list.clear();

        // 尝试添加第二条消息（应该被拒绝）
        boolean accepted = channelMonitorService.handleMediaGroupMessage(message2);
        assertFalse(accepted, "消息应该被拒绝");

        // 验证日志
        List<ILoggingEvent> logsList = listAppender.list;
        
        boolean foundRejectionLog = logsList.stream()
            .anyMatch(event -> 
                event.getLevel() == Level.WARN &&
                event.getFormattedMessage().contains("消息被拒绝") &&
                event.getFormattedMessage().contains("groupKey=1002:5002") &&
                event.getFormattedMessage().contains("messageId=3") &&
                event.getFormattedMessage().contains("currentState=") &&
                event.getFormattedMessage().contains("reason=")
            );
        assertTrue(foundRejectionLog, "应该记录消息拒绝日志");
    }

    /**
     * 测试锁操作日志（需求 9.3, 9.5）
     * 验证锁获取和释放的 DEBUG 日志
     */
    @Test
    void testLockOperationLogging() {
        // 设置 DEBUG 级别
        logger.setLevel(Level.DEBUG);

        // 创建测试消息
        TdApi.Message message = createTestMessage(1003L, 4L, 5003L);

        // 添加消息
        channelMonitorService.handleMediaGroupMessage(message);

        // 验证日志
        List<ILoggingEvent> logsList = listAppender.list;
        
        // 验证锁获取日志
        boolean foundLockAcquire = logsList.stream()
            .anyMatch(event -> 
                event.getLevel() == Level.DEBUG &&
                event.getFormattedMessage().contains("尝试获取锁") &&
                event.getFormattedMessage().contains("groupKey=1003:5003")
            );
        assertTrue(foundLockAcquire, "应该记录锁获取日志");

        // 验证锁已获取日志
        boolean foundLockAcquired = logsList.stream()
            .anyMatch(event -> 
                event.getLevel() == Level.DEBUG &&
                event.getFormattedMessage().contains("已获取锁") &&
                event.getFormattedMessage().contains("groupKey=1003:5003")
            );
        assertTrue(foundLockAcquired, "应该记录已获取锁日志");

        // 验证锁释放日志
        boolean foundLockRelease = logsList.stream()
            .anyMatch(event -> 
                event.getLevel() == Level.DEBUG &&
                event.getFormattedMessage().contains("已释放锁") &&
                event.getFormattedMessage().contains("groupKey=1003:5003")
            );
        assertTrue(foundLockRelease, "应该记录锁释放日志");

        // 恢复日志级别
        logger.setLevel(Level.INFO);
    }

    /**
     * 测试并发冲突日志（需求 9.4）
     * 验证并发冲突时记录 groupKey、thread、operation、currentState
     */
    @Test
    void testConcurrentConflictLogging() throws InterruptedException {
        // 创建测试消息
        TdApi.Message message1 = createTestMessage(1004L, 5L, 5004L);
        TdApi.Message message2 = createTestMessage(1004L, 6L, 5004L);

        // 添加第一条消息
        channelMonitorService.handleMediaGroupMessage(message1);

        // 等待超时并处理
        Thread.sleep(2500);

        // 清空之前的日志
        listAppender.list.clear();

        // 尝试添加第二条消息（应该产生并发冲突）
        channelMonitorService.handleMediaGroupMessage(message2);

        // 验证日志
        List<ILoggingEvent> logsList = listAppender.list;
        
        boolean foundConflictLog = logsList.stream()
            .anyMatch(event -> 
                event.getLevel() == Level.WARN &&
                event.getFormattedMessage().contains("并发冲突") &&
                event.getFormattedMessage().contains("groupKey=1004:5004") &&
                event.getFormattedMessage().contains("thread=") &&
                event.getFormattedMessage().contains("operation=handleMediaGroupMessage") &&
                event.getFormattedMessage().contains("currentState=")
            );
        assertTrue(foundConflictLog, "应该记录并发冲突日志");
    }

    /**
     * 创建测试消息
     */
    private TdApi.Message createTestMessage(long chatId, long messageId, long mediaAlbumId) {
        TdApi.Message message = new TdApi.Message();
        message.chatId = chatId;
        message.id = messageId;
        message.mediaAlbumId = mediaAlbumId;
        message.date = (int) (System.currentTimeMillis() / 1000);
        message.content = new TdApi.MessagePhoto();
        return message;
    }
}
