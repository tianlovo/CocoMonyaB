package org.xlyo.cocomonyab.service;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xlyo.cocomonyab.domain.entity.Channel;
import org.xlyo.cocomonyab.domain.entity.message.BaseMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.TextMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.TelegraphMessageEntity;
import org.xlyo.cocomonyab.domain.entity.message.MediaGroupMessageEntity;
import org.xlyo.cocomonyab.plugin.PluginManager;
import org.xlyo.cocomonyab.repository.ChannelRepository;
import org.xlyo.cocomonyab.service.message.MessageParser;
import org.xlyo.cocomonyab.service.message.MessageStorageService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 消息处理流程集成测试
 * 测试完整的消息处理流程：TDLib → Storage → Parser → PluginManager → Plugins
 */
@ExtendWith(MockitoExtension.class)
class MessageProcessingIntegrationTest {
    
    @Mock
    private ChannelRepository channelRepository;
    
    @Mock
    private MessageStorageService messageStorageService;
    
    @Mock
    private MessageParser messageParser;
    
    @Mock
    private PluginManager pluginManager;
    
    @Mock
    private org.xlyo.cocomonyab.filter.FilterChainManager filterChainManager;
    
    @Mock
    private org.xlyo.cocomonyab.filter.impl.ChannelMonitoringFilter channelMonitoringFilter;
    
    private ChannelMonitorService channelMonitorService;
    
    @BeforeEach
    void setUp() {
        channelMonitorService = new ChannelMonitorService(
            channelRepository,
            messageStorageService,
            messageParser,
            pluginManager,
            filterChainManager,
            channelMonitoringFilter
        );
        
        // 模拟过滤器链默认接受所有消息
        when(filterChainManager.executeChain(any())).thenReturn(true);
        when(channelMonitoringFilter.isMonitoring(anyLong())).thenReturn(true);
        
        // 启动监控
        channelMonitorService.startMonitoring(-1001234567890L);
    }
    
    @Test
    void testSingleTextMessageProcessingFlow() {
        // Given: 创建文本消息
        TdApi.Message message = createTextMessage(123L, -1001234567890L, "Hello World");
        
        TextMessageEntity parsedEntity = new TextMessageEntity();
        parsedEntity.setMessageId(123L);
        parsedEntity.setChatId(-1001234567890L);
        parsedEntity.setTextContent("Hello World");
        
        when(messageStorageService.saveMessage(any())).thenReturn(true);
        when(messageParser.parse(any())).thenReturn(parsedEntity);
        
        // When: 处理消息
        channelMonitorService.handleNewMessage(message);
        
        // Then: 验证处理流程
        // 1. 保存原始消息
        verify(messageStorageService, times(1)).saveMessage(eq(message));
        
        // 2. 解析消息
        verify(messageParser, times(1)).parse(eq(message));
        
        // 3. 插件处理
        ArgumentCaptor<BaseMessageEntity> entityCaptor = ArgumentCaptor.forClass(BaseMessageEntity.class);
        ArgumentCaptor<TdApi.Message> messageCaptor = ArgumentCaptor.forClass(TdApi.Message.class);
        verify(pluginManager, times(1)).process(entityCaptor.capture(), messageCaptor.capture());
        
        assertThat(entityCaptor.getValue()).isInstanceOf(TextMessageEntity.class);
        assertThat(((TextMessageEntity) entityCaptor.getValue()).getTextContent()).isEqualTo("Hello World");
        assertThat(messageCaptor.getValue()).isEqualTo(message);
    }
    
    @Test
    void testTelegraphMessageProcessingFlow() {
        // Given: 创建Telegraph消息
        TdApi.Message message = createTelegraphMessage(456L, -1001234567890L, "Article Title");
        
        TelegraphMessageEntity parsedEntity = new TelegraphMessageEntity();
        parsedEntity.setMessageId(456L);
        parsedEntity.setChatId(-1001234567890L);
        parsedEntity.setTextContent("Article Title");
        
        when(messageStorageService.saveMessage(any())).thenReturn(true);
        when(messageParser.parse(any())).thenReturn(parsedEntity);
        
        // When: 处理消息
        channelMonitorService.handleNewMessage(message);
        
        // Then: 验证处理流程
        verify(messageStorageService, times(1)).saveMessage(eq(message));
        verify(messageParser, times(1)).parse(eq(message));
        
        ArgumentCaptor<BaseMessageEntity> entityCaptor = ArgumentCaptor.forClass(BaseMessageEntity.class);
        verify(pluginManager, times(1)).process(entityCaptor.capture(), any());
        
        assertThat(entityCaptor.getValue()).isInstanceOf(TelegraphMessageEntity.class);
    }
    
    @Test
    void testMediaGroupProcessingFlow() throws InterruptedException {
        // Given: 创建媒体组消息
        long mediaAlbumId = 999L;
        TdApi.Message message1 = createPhotoMessage(101L, -1001234567890L, mediaAlbumId);
        TdApi.Message message2 = createPhotoMessage(102L, -1001234567890L, mediaAlbumId);
        TdApi.Message message3 = createPhotoMessage(103L, -1001234567890L, mediaAlbumId);
        
        BaseMessageEntity entity1 = new TextMessageEntity();
        entity1.setMessageId(101L);
        BaseMessageEntity entity2 = new TextMessageEntity();
        entity2.setMessageId(102L);
        BaseMessageEntity entity3 = new TextMessageEntity();
        entity3.setMessageId(103L);
        
        when(messageStorageService.saveMessage(any())).thenReturn(true);
        when(messageParser.parse(message1)).thenReturn(entity1);
        when(messageParser.parse(message2)).thenReturn(entity2);
        when(messageParser.parse(message3)).thenReturn(entity3);
        
        // When: 处理媒体组消息
        channelMonitorService.handleNewMessage(message1);
        channelMonitorService.handleNewMessage(message2);
        channelMonitorService.handleNewMessage(message3);
        
        // 等待超时处理
        Thread.sleep(2500);
        channelMonitorService.processTimedOutMediaGroups();
        
        // Then: 验证处理流程
        // 1. 保存所有原始消息
        verify(messageStorageService, times(3)).saveMessage(any());
        
        // 2. 解析所有消息
        verify(messageParser, times(3)).parse(any());
        
        // 3. 插件处理（应该只调用一次，处理整个媒体组）
        ArgumentCaptor<BaseMessageEntity> entityCaptor = ArgumentCaptor.forClass(BaseMessageEntity.class);
        verify(pluginManager, times(1)).process(entityCaptor.capture(), any());
        
        // 验证传递的是媒体组实体
        assertThat(entityCaptor.getValue()).isInstanceOf(MediaGroupMessageEntity.class);
        MediaGroupMessageEntity mediaGroup = (MediaGroupMessageEntity) entityCaptor.getValue();
        assertThat(mediaGroup.getItems()).hasSize(3);
        assertThat(mediaGroup.getMediaAlbumId()).isEqualTo(mediaAlbumId);
        assertThat(mediaGroup.getIsMediaGroup()).isTrue();
    }
    
    @Test
    void testMessageProcessingWithStorageFailure() {
        // Given: 存储失败的场景
        TdApi.Message message = createTextMessage(789L, -1001234567890L, "Test");
        
        TextMessageEntity parsedEntity = new TextMessageEntity();
        parsedEntity.setMessageId(789L);
        
        when(messageStorageService.saveMessage(any())).thenReturn(false); // 存储失败
        when(messageParser.parse(any())).thenReturn(parsedEntity);
        
        // When: 处理消息
        channelMonitorService.handleNewMessage(message);
        
        // Then: 即使存储失败，解析和插件处理仍应继续
        verify(messageStorageService, times(1)).saveMessage(any());
        verify(messageParser, times(1)).parse(any());
        verify(pluginManager, times(1)).process(any(), any());
    }
    
    @Test
    void testMessageProcessingWithParsingFailure() {
        // Given: 解析失败的场景
        TdApi.Message message = createTextMessage(999L, -1001234567890L, "Test");
        
        when(messageStorageService.saveMessage(any())).thenReturn(true);
        when(messageParser.parse(any())).thenThrow(new RuntimeException("Parse error"));
        
        // When: 处理消息
        channelMonitorService.handleNewMessage(message);
        
        // Then: 存储应该成功，但插件处理不应被调用
        verify(messageStorageService, times(1)).saveMessage(any());
        verify(messageParser, times(1)).parse(any());
        verify(pluginManager, never()).process(any(), any());
    }
    
    // Helper methods to create test messages
    
    private TdApi.Message createTextMessage(long messageId, long chatId, String text) {
        TdApi.Message message = new TdApi.Message();
        message.id = messageId;
        message.chatId = chatId;
        message.isChannelPost = true;
        message.date = (int) (System.currentTimeMillis() / 1000);
        message.mediaAlbumId = 0;
        
        TdApi.MessageText content = new TdApi.MessageText();
        content.text = new TdApi.FormattedText();
        content.text.text = text;
        message.content = content;
        
        return message;
    }
    
    private TdApi.Message createTelegraphMessage(long messageId, long chatId, String title) {
        TdApi.Message message = createTextMessage(messageId, chatId, title);
        
        TdApi.MessageText content = (TdApi.MessageText) message.content;
        content.webPage = new TdApi.WebPage();
        content.webPage.url = "https://telegra.ph/article";
        content.webPage.title = title;
        content.webPage.instantViewVersion = 2;
        
        return message;
    }
    
    private TdApi.Message createPhotoMessage(long messageId, long chatId, long mediaAlbumId) {
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
