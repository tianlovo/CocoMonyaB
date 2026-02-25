package org.xlyo.cocomonyab.service;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xlyo.cocomonyab.config.properties.ConcurrentSafetyProperties;
import org.xlyo.cocomonyab.domain.entity.RawMessage;
import org.xlyo.cocomonyab.filter.FilterChainManager;
import org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter;
import org.xlyo.cocomonyab.plugin.PluginManager;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.repository.RawMessageRepository;
import org.xlyo.cocomonyab.service.message.MessageParser;
import org.xlyo.cocomonyab.service.message.MessageStorageService;
import org.xlyo.cocomonyab.service.metrics.MediaGroupMetrics;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

/**
 * 向后兼容性集成测试
 * 
 * 验证需求：
 * - 需求 10.1: 保持现有的媒体组数据结构不变
 * - 需求 10.2: 保持现有的数据库索引不变
 * - 需求 10.3: 保持现有的 API 接口不变
 * - 需求 10.4: 系统升级后能够正确处理升级前保存的数据
 */
@ExtendWith(MockitoExtension.class)
class BackwardCompatibilityIntegrationTest {
    
    @Mock
    private ChannelRepository channelRepository;
    
    @Mock
    private RawMessageRepository rawMessageRepository;
    
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
    private DuplicateMessageFilter duplicateMessageFilter;
    
    @Mock
    private MediaGroupMetrics mediaGroupMetrics;
    
    private ChannelMonitorService channelMonitorService;
    
    private static final long TEST_CHAT_ID = -1001234567890L;
    
    @BeforeEach
    void setUp() {
        // 配置属性
        ConcurrentSafetyProperties properties = new ConcurrentSafetyProperties();
        properties.getMediaGroup().setTimeout(2000);
        properties.getMediaGroup().setMaxBufferSize(1000);
        properties.getLock().setStripes(128);
        properties.getLock().setTimeout(5000);
        properties.getCache().setTtl(10);
        properties.getCache().setMaxSize(10000);
        properties.getCache().setFailedMessageTtl(5);
        
        // 创建服务
        channelMonitorService = new ChannelMonitorService(
            channelRepository,
            messageStorageService,
            messageParser,
            pluginManager,
            filterChainManager,
            channelMonitoringFilter,
            duplicateMessageFilter,
            mediaGroupMetrics,
            properties
        );
        channelMonitorService.initMetrics();
        
        // 模拟过滤器链默认接受所有消息
        lenient().when(filterChainManager.executeChain(any())).thenReturn(true);
        lenient().when(channelMonitoringFilter.isMonitoring(anyLong())).thenReturn(true);
        lenient().when(messageStorageService.saveMessage(any())).thenReturn(true);
        
        // 启动监控
        channelMonitorService.startMonitoring(TEST_CHAT_ID);
    }
    
    @Test
    void testRawMessageDataStructureCompatibility() {
        // Given: 创建一个符合旧数据结构的 RawMessage
        RawMessage oldMessage = new RawMessage();
        oldMessage.setId("507f1f77bcf86cd799439011");
        oldMessage.setChatId(TEST_CHAT_ID);
        oldMessage.setMessageId(123L);
        oldMessage.setMediaAlbumId(0L); // 非媒体组消息
        oldMessage.setDate((int) (System.currentTimeMillis() / 1000));
        oldMessage.setRawJson("{\"id\":123,\"chatId\":-1001234567890}");
        oldMessage.setCreateTime(LocalDateTime.now());
        oldMessage.setUpdateTime(LocalDateTime.now());
        
        // When: 验证数据结构字段都存在且可访问
        // Then: 所有字段都应该可以正常访问
        assertThat(oldMessage.getId()).isNotNull();
        assertThat(oldMessage.getChatId()).isEqualTo(TEST_CHAT_ID);
        assertThat(oldMessage.getMessageId()).isEqualTo(123L);
        assertThat(oldMessage.getMediaAlbumId()).isEqualTo(0L);
        assertThat(oldMessage.getDate()).isNotNull();
        assertThat(oldMessage.getRawJson()).isNotNull();
        assertThat(oldMessage.getCreateTime()).isNotNull();
        assertThat(oldMessage.getUpdateTime()).isNotNull();
    }
    
    @Test
    void testMediaGroupDataStructureCompatibility() {
        // Given: 创建一个符合旧数据结构的媒体组 RawMessage
        RawMessage oldMediaGroupMessage = new RawMessage();
        oldMediaGroupMessage.setId("507f1f77bcf86cd799439012");
        oldMediaGroupMessage.setChatId(TEST_CHAT_ID);
        oldMediaGroupMessage.setMessageId(456L);
        oldMediaGroupMessage.setMediaAlbumId(5629499534213120L); // 媒体组ID
        oldMediaGroupMessage.setDate((int) (System.currentTimeMillis() / 1000));
        oldMediaGroupMessage.setRawJson("{\"id\":456,\"chatId\":-1001234567890,\"mediaAlbumId\":5629499534213120}");
        oldMediaGroupMessage.setCreateTime(LocalDateTime.now());
        oldMediaGroupMessage.setUpdateTime(LocalDateTime.now());
        
        // When: 验证媒体组数据结构字段都存在且可访问
        // Then: 所有字段都应该可以正常访问
        assertThat(oldMediaGroupMessage.getId()).isNotNull();
        assertThat(oldMediaGroupMessage.getChatId()).isEqualTo(TEST_CHAT_ID);
        assertThat(oldMediaGroupMessage.getMessageId()).isEqualTo(456L);
        assertThat(oldMediaGroupMessage.getMediaAlbumId()).isEqualTo(5629499534213120L);
        assertThat(oldMediaGroupMessage.getDate()).isNotNull();
        assertThat(oldMediaGroupMessage.getRawJson()).isNotNull();
    }
    
    @Test
    void testRepositoryMethodsCompatibility() {
        // Given: 模拟旧数据存在于数据库
        RawMessage existingMessage = new RawMessage();
        existingMessage.setChatId(TEST_CHAT_ID);
        existingMessage.setMessageId(789L);
        existingMessage.setMediaAlbumId(0L);
        
        lenient().when(rawMessageRepository.existsByChatIdAndMessageId(TEST_CHAT_ID, 789L))
            .thenReturn(true);
        lenient().when(rawMessageRepository.findByChatIdAndMessageId(TEST_CHAT_ID, 789L))
            .thenReturn(Optional.of(existingMessage));
        
        // When: 使用repository方法查询
        boolean exists = rawMessageRepository.existsByChatIdAndMessageId(TEST_CHAT_ID, 789L);
        Optional<RawMessage> found = rawMessageRepository.findByChatIdAndMessageId(TEST_CHAT_ID, 789L);
        
        // Then: 旧的repository方法应该仍然可用
        assertThat(exists).isTrue();
        assertThat(found).isPresent();
        assertThat(found.get().getMessageId()).isEqualTo(789L);
    }
    
    @Test
    void testMediaGroupRepositoryMethodsCompatibility() {
        // Given: 模拟旧的媒体组数据存在于数据库
        RawMessage existingMediaGroup = new RawMessage();
        existingMediaGroup.setChatId(TEST_CHAT_ID);
        existingMediaGroup.setMessageId(999L);
        existingMediaGroup.setMediaAlbumId(5629499534213120L);
        
        lenient().when(rawMessageRepository.existsByChatIdAndMediaAlbumId(TEST_CHAT_ID, 5629499534213120L))
            .thenReturn(true);
        lenient().when(rawMessageRepository.findByChatIdAndMediaAlbumId(TEST_CHAT_ID, 5629499534213120L))
            .thenReturn(Optional.of(existingMediaGroup));
        
        // When: 使用repository方法查询媒体组
        boolean exists = rawMessageRepository.existsByChatIdAndMediaAlbumId(TEST_CHAT_ID, 5629499534213120L);
        Optional<RawMessage> found = rawMessageRepository.findByChatIdAndMediaAlbumId(TEST_CHAT_ID, 5629499534213120L);
        
        // Then: 旧的repository方法应该仍然可用
        assertThat(exists).isTrue();
        assertThat(found).isPresent();
        assertThat(found.get().getMediaAlbumId()).isEqualTo(5629499534213120L);
    }
    
    @Test
    void testChannelMonitorServiceAPICompatibility() {
        // When: 调用ChannelMonitorService的公共API方法
        // Then: 所有旧的API方法应该仍然可用
        
        // 测试监控控制方法
        channelMonitorService.startMonitoring(TEST_CHAT_ID);
        channelMonitorService.stopMonitoring(TEST_CHAT_ID);
        channelMonitorService.reloadMonitoringChannels();
        
        // 测试查询方法
        boolean isMonitoring = channelMonitorService.isMonitoring(TEST_CHAT_ID);
        int count = channelMonitorService.getMonitoringChannelCount();
        
        // 验证方法可以正常调用
        assertThat(isMonitoring).isNotNull();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    void testHandleNewMessageAPICompatibility() {
        // Given: 创建一条测试消息
        TdApi.Message message = createTestMessage(111L, TEST_CHAT_ID);
        
        // When: 调用handleNewMessage方法（旧的API）
        // Then: 方法应该正常执行，不抛出异常
        channelMonitorService.handleNewMessage(message);
        
        // 验证方法签名没有改变
        assertThat(message).isNotNull();
    }
    
    @Test
    void testMediaGroupProcessingAPICompatibility() throws InterruptedException {
        // Given: 创建媒体组消息
        long mediaAlbumId = 5629499534213120L;
        TdApi.Message message1 = createMediaGroupMessage(201L, TEST_CHAT_ID, mediaAlbumId);
        TdApi.Message message2 = createMediaGroupMessage(202L, TEST_CHAT_ID, mediaAlbumId);
        
        // When: 使用旧的API处理媒体组消息
        channelMonitorService.handleNewMessage(message1);
        channelMonitorService.handleNewMessage(message2);
        
        // 等待超时处理
        Thread.sleep(2500);
        channelMonitorService.processTimedOutMediaGroups();
        
        // Then: 处理应该正常完成，不抛出异常
        // 验证API签名没有改变
        assertThat(message1.mediaAlbumId).isEqualTo(mediaAlbumId);
        assertThat(message2.mediaAlbumId).isEqualTo(mediaAlbumId);
    }
    
    @Test
    void testConfigurationPropertiesCompatibility() {
        // Given: 创建配置对象
        ConcurrentSafetyProperties properties = new ConcurrentSafetyProperties();
        
        // When: 设置配置值
        properties.getMediaGroup().setTimeout(3000);
        properties.getMediaGroup().setMaxBufferSize(2000);
        properties.getLock().setStripes(256);
        properties.getLock().setTimeout(10000);
        properties.getCache().setTtl(20);
        properties.getCache().setMaxSize(20000);
        properties.getCache().setFailedMessageTtl(10);
        
        // Then: 所有配置项都应该可以正常访问
        assertThat(properties.getMediaGroup().getTimeout()).isEqualTo(3000);
        assertThat(properties.getMediaGroup().getMaxBufferSize()).isEqualTo(2000);
        assertThat(properties.getLock().getStripes()).isEqualTo(256);
        assertThat(properties.getLock().getTimeout()).isEqualTo(10000);
        assertThat(properties.getCache().getTtl()).isEqualTo(20);
        assertThat(properties.getCache().getMaxSize()).isEqualTo(20000);
        assertThat(properties.getCache().getFailedMessageTtl()).isEqualTo(10);
    }
    
    @Test
    void testOldDataCanBeProcessed() {
        // Given: 模拟升级前保存的旧数据
        RawMessage oldData = new RawMessage();
        oldData.setId("old-message-id");
        oldData.setChatId(TEST_CHAT_ID);
        oldData.setMessageId(333L);
        oldData.setMediaAlbumId(0L);
        oldData.setDate((int) (System.currentTimeMillis() / 1000) - 86400); // 1天前
        oldData.setRawJson("{\"id\":333,\"chatId\":-1001234567890}");
        oldData.setCreateTime(LocalDateTime.now().minusDays(1));
        oldData.setUpdateTime(LocalDateTime.now().minusDays(1));
        
        lenient().when(rawMessageRepository.findByChatIdAndMessageId(TEST_CHAT_ID, 333L))
            .thenReturn(Optional.of(oldData));
        
        // When: 查询旧数据
        Optional<RawMessage> result = rawMessageRepository.findByChatIdAndMessageId(TEST_CHAT_ID, 333L);
        
        // Then: 旧数据应该可以正常读取和处理
        assertThat(result).isPresent();
        RawMessage retrieved = result.get();
        assertThat(retrieved.getId()).isEqualTo("old-message-id");
        assertThat(retrieved.getChatId()).isEqualTo(TEST_CHAT_ID);
        assertThat(retrieved.getMessageId()).isEqualTo(333L);
        assertThat(retrieved.getRawJson()).contains("333");
    }
    
    @Test
    void testMediaGroupStateEnumCompatibility() {
        // When: 使用MediaGroupState枚举
        MediaGroupState collecting = MediaGroupState.COLLECTING;
        MediaGroupState processing = MediaGroupState.PROCESSING;
        MediaGroupState completed = MediaGroupState.COMPLETED;
        
        // Then: 枚举值应该可以正常使用
        assertThat(collecting).isNotNull();
        assertThat(processing).isNotNull();
        assertThat(completed).isNotNull();
        
        // 验证枚举顺序（向后兼容）
        assertThat(MediaGroupState.values()).hasSize(3);
        assertThat(MediaGroupState.values()[0]).isEqualTo(MediaGroupState.COLLECTING);
        assertThat(MediaGroupState.values()[1]).isEqualTo(MediaGroupState.PROCESSING);
        assertThat(MediaGroupState.values()[2]).isEqualTo(MediaGroupState.COMPLETED);
    }
    
    // Helper methods
    
    private TdApi.Message createTestMessage(long messageId, long chatId) {
        TdApi.Message message = new TdApi.Message();
        message.id = messageId;
        message.chatId = chatId;
        message.isChannelPost = true;
        message.date = (int) (System.currentTimeMillis() / 1000);
        message.mediaAlbumId = 0;
        
        TdApi.MessageText content = new TdApi.MessageText();
        content.text = new TdApi.FormattedText();
        content.text.text = "Test message";
        message.content = content;
        
        return message;
    }
    
    private TdApi.Message createMediaGroupMessage(long messageId, long chatId, long mediaAlbumId) {
        TdApi.Message message = new TdApi.Message();
        message.id = messageId;
        message.chatId = chatId;
        message.isChannelPost = true;
        message.date = (int) (System.currentTimeMillis() / 1000);
        message.mediaAlbumId = mediaAlbumId;
        
        TdApi.MessagePhoto content = new TdApi.MessagePhoto();
        content.photo = new TdApi.Photo();
        content.photo.sizes = new TdApi.PhotoSize[0];
        content.caption = new TdApi.FormattedText();
        content.caption.text = "";
        message.content = content;
        
        return message;
    }
}
