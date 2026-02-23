package org.xlyo.cocomonyab.plugin.impl.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.xlyo.cocomonyab.domain.entity.message.*;
import org.xlyo.cocomonyab.event.ChannelMonitoringEvent;
import org.xlyo.cocomonyab.plugin.PluginContext;
import org.xlyo.cocomonyab.plugin.PluginResult;
import org.xlyo.cocomonyab.plugin.impl.websocket.config.WebSocketBroadcastProperties;
import org.xlyo.cocomonyab.plugin.impl.websocket.dto.ChannelMonitoringNotificationDTO;
import org.xlyo.cocomonyab.plugin.impl.websocket.dto.MessageBroadcastDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebSocketBroadcastPlugin 单元测试
 * 测试插件基础属性、消息广播、DTO转换、错误处理、频道监控事件处理
 */
@ExtendWith(MockitoExtension.class)
class WebSocketBroadcastPluginTest {
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @Mock
    private WebSocketBroadcastProperties properties;
    
    private WebSocketBroadcastPlugin plugin;
    
    @BeforeEach
    void setUp() {
        // 设置默认配置
        when(properties.getTopicPrefix()).thenReturn("/topic/channel/real");
        when(properties.getMonitoringTopicPrefix()).thenReturn("/topic/channel/monitoring");
        
        plugin = new WebSocketBroadcastPlugin(messagingTemplate, properties);
    }
    
    /**
     * 测试插件基本属性
     * Requirements: 1.2, 1.3, 1.4
     */
    @Test
    void testPluginProperties() {
        assertEquals("WebSocketBroadcastPlugin", plugin.getName());
        assertEquals(50, plugin.getPriority());
        assertTrue(plugin.isEnabled());
    }

    /**
     * 测试消息广播到正确的topic
     * Requirements: 2.1
     */
    @Test
    void testBroadcastToCorrectTopic() {
        // 准备测试数据
        TextMessageEntity entity = createTextMessage();
        PluginContext context = new PluginContext(null);
        
        // 执行测试
        PluginResult result = plugin.handle(entity, context);
        
        // 验证结果
        assertEquals(PluginResult.CONTINUE, result);
        
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessageBroadcastDTO> dtoCaptor = ArgumentCaptor.forClass(MessageBroadcastDTO.class);
        
        verify(messagingTemplate).convertAndSend(topicCaptor.capture(), dtoCaptor.capture());
        
        String expectedTopic = "/topic/channel/real/-1001234567890";
        assertEquals(expectedTopic, topicCaptor.getValue());
        
        MessageBroadcastDTO dto = dtoCaptor.getValue();
        assertNotNull(dto);
        assertEquals(12345L, dto.getMessageId());
        assertEquals(-1001234567890L, dto.getChatId());
    }
    
    /**
     * 测试广播失败时返回CONTINUE
     * Requirements: 2.1
     */
    @Test
    void testBroadcastFailureReturnsContinue() {
        // 准备测试数据
        TextMessageEntity entity = createTextMessage();
        PluginContext context = new PluginContext(null);
        
        doThrow(new RuntimeException("Connection failed"))
            .when(messagingTemplate).convertAndSend(anyString(), any(MessageBroadcastDTO.class));
        
        // 执行测试
        PluginResult result = plugin.handle(entity, context);
        
        // 验证结果
        assertEquals(PluginResult.CONTINUE, result);
        verify(messagingTemplate).convertAndSend(anyString(), any(MessageBroadcastDTO.class));
    }

    /**
     * 测试TEXT消息转换
     * Requirements: 2.2, 2.3, 3.2, 3.3
     */
    @Test
    void testTextMessageConversion() {
        // 准备测试数据
        TextMessageEntity entity = createTextMessage();
        PluginContext context = new PluginContext(null);
        
        // 执行测试
        plugin.handle(entity, context);
        
        // 验证结果
        ArgumentCaptor<MessageBroadcastDTO> dtoCaptor = ArgumentCaptor.forClass(MessageBroadcastDTO.class);
        verify(messagingTemplate).convertAndSend(anyString(), dtoCaptor.capture());
        
        MessageBroadcastDTO dto = dtoCaptor.getValue();
        
        // 验证所有必需字段
        assertEquals(12345L, dto.getMessageId());
        assertEquals(-1001234567890L, dto.getChatId());
        assertEquals("testchannel", dto.getChannelUsername());
        assertEquals("Test Channel", dto.getChannelTitle());
        assertEquals(1000000000, dto.getDate());
        assertEquals("TEXT", dto.getContentType());
        assertEquals("Hello, World!", dto.getTextContent());
        assertEquals(100, dto.getViews());
        assertEquals(10, dto.getForwards());
    }
    
    /**
     * 测试PHOTO消息转换（包含photos字段）
     * Requirements: 2.2, 2.3, 3.4
     */
    @Test
    void testPhotoMessageConversion() {
        // 准备测试数据
        PhotoMessageEntity entity = createPhotoMessage();
        PluginContext context = new PluginContext(null);
        
        // 执行测试
        plugin.handle(entity, context);
        
        // 验证结果
        ArgumentCaptor<MessageBroadcastDTO> dtoCaptor = ArgumentCaptor.forClass(MessageBroadcastDTO.class);
        verify(messagingTemplate).convertAndSend(anyString(), dtoCaptor.capture());
        
        MessageBroadcastDTO dto = dtoCaptor.getValue();
        
        // 验证基础字段
        assertEquals(12345L, dto.getMessageId());
        assertEquals("PHOTO", dto.getContentType());
        assertEquals("Beautiful photo", dto.getTextContent());
        
        // 验证photos字段
        assertNotNull(dto.getPhotos());
        assertEquals(1, dto.getPhotos().size());
        assertEquals("111", dto.getPhotos().get(0).getFileId());
        assertEquals(1024000L, dto.getPhotos().get(0).getFileSize());
    }
    
    /**
     * 测试VIDEO消息转换（包含video字段）
     * Requirements: 2.2, 2.3, 3.4
     */
    @Test
    void testVideoMessageConversion() {
        // 准备测试数据
        VideoMessageEntity entity = createVideoMessage();
        PluginContext context = new PluginContext(null);
        
        // 执行测试
        plugin.handle(entity, context);
        
        // 验证结果
        ArgumentCaptor<MessageBroadcastDTO> dtoCaptor = ArgumentCaptor.forClass(MessageBroadcastDTO.class);
        verify(messagingTemplate).convertAndSend(anyString(), dtoCaptor.capture());
        
        MessageBroadcastDTO dto = dtoCaptor.getValue();
        
        // 验证基础字段
        assertEquals(12345L, dto.getMessageId());
        assertEquals("VIDEO", dto.getContentType());
        assertEquals("Amazing video", dto.getTextContent());
        
        // 验证video字段
        assertNotNull(dto.getVideo());
        assertEquals("222", dto.getVideo().getFileId());
        assertEquals(5120000L, dto.getVideo().getFileSize());
    }
    
    /**
     * 测试TELEGRAPH消息转换（包含webPage字段）
     * Requirements: 2.2, 2.3, 3.5
     */
    @Test
    void testTelegraphMessageConversion() {
        // 准备测试数据
        TelegraphMessageEntity entity = createTelegraphMessage();
        PluginContext context = new PluginContext(null);
        
        // 执行测试
        plugin.handle(entity, context);
        
        // 验证结果
        ArgumentCaptor<MessageBroadcastDTO> dtoCaptor = ArgumentCaptor.forClass(MessageBroadcastDTO.class);
        verify(messagingTemplate).convertAndSend(anyString(), dtoCaptor.capture());
        
        MessageBroadcastDTO dto = dtoCaptor.getValue();
        
        // 验证基础字段
        assertEquals(12345L, dto.getMessageId());
        assertEquals("TELEGRAPH", dto.getContentType());
        assertEquals("Check out this article", dto.getTextContent());
        
        // 验证webPage字段
        assertNotNull(dto.getWebPage());
        assertEquals("Amazing Article", dto.getWebPage().getTitle());
        assertEquals("John Doe", dto.getWebPage().getAuthor());
        assertEquals("https://example.com/article", dto.getWebPage().getUrl());
        assertEquals("Example Site", dto.getWebPage().getSiteName());
        assertTrue(dto.getWebPage().getHasInstantView());
    }
    
    /**
     * 测试MEDIA_GROUP消息转换（包含媒体组字段）
     * Requirements: 2.2, 2.3, 3.6
     */
    @Test
    void testMediaGroupMessageConversion() {
        // 准备测试数据
        MediaGroupMessageEntity entity = createMediaGroupMessage();
        PluginContext context = new PluginContext(null);
        
        // 执行测试
        plugin.handle(entity, context);
        
        // 验证结果
        ArgumentCaptor<MessageBroadcastDTO> dtoCaptor = ArgumentCaptor.forClass(MessageBroadcastDTO.class);
        verify(messagingTemplate).convertAndSend(anyString(), dtoCaptor.capture());
        
        MessageBroadcastDTO dto = dtoCaptor.getValue();
        
        // 验证基础字段
        assertEquals(12345L, dto.getMessageId());
        assertEquals("MEDIA_GROUP", dto.getContentType());
        
        // 验证媒体组字段
        assertEquals(99999L, dto.getMediaAlbumId());
        assertTrue(dto.getIsMediaGroup());
        assertEquals(2, dto.getItemCount());
        assertNotNull(dto.getItems());
        assertEquals(2, dto.getItems().size());
        
        // 验证媒体组中的第一条消息
        MessageBroadcastDTO item1 = dto.getItems().get(0);
        assertEquals(12346L, item1.getMessageId());
        assertEquals("Photo 1", item1.getTextContent());
        
        // 验证媒体组中的第二条消息
        MessageBroadcastDTO item2 = dto.getItems().get(1);
        assertEquals(12347L, item2.getMessageId());
        assertEquals("Photo 2", item2.getTextContent());
    }

    /**
     * 测试消息实体为null时的处理
     * Requirements: 2.5, 5.2, 5.3
     */
    @Test
    void testNullEntityHandling() {
        // 准备测试数据
        PluginContext context = new PluginContext(null);
        
        // 执行测试
        PluginResult result = plugin.handle(null, context);
        
        // 验证结果
        assertEquals(PluginResult.CONTINUE, result);
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(MessageBroadcastDTO.class));
    }
    
    /**
     * 测试DTO转换失败时返回CONTINUE
     * Requirements: 2.5, 5.2, 5.3
     */
    @Test
    void testDtoConversionFailureReturnsContinue() {
        // 准备测试数据 - 创建一个会导致转换失败的消息实体
        BaseMessageEntity entity = new BaseMessageEntity() {
            @Override
            public org.xlyo.cocomonyab.domain.enums.MessageType getType() {
                throw new RuntimeException("Conversion error");
            }
        };
        entity.setMessageId(12345L);
        entity.setChatId(-1001234567890L);
        entity.setDate(1000000000);
        
        PluginContext context = new PluginContext(null);
        
        // 执行测试
        PluginResult result = plugin.handle(entity, context);
        
        // 验证结果
        assertEquals(PluginResult.CONTINUE, result);
    }
    
    /**
     * 测试异常不会向外抛出
     * Requirements: 2.5, 5.2, 5.3
     */
    @Test
    void testExceptionsDoNotPropagate() {
        // 准备测试数据
        TextMessageEntity entity = createTextMessage();
        PluginContext context = new PluginContext(null);
        
        doThrow(new RuntimeException("Unexpected error"))
            .when(messagingTemplate).convertAndSend(anyString(), any(MessageBroadcastDTO.class));
        
        // 执行测试并验证 - 不应该抛出异常
        assertDoesNotThrow(() -> plugin.handle(entity, context));
    }
    
    /**
     * 测试所有错误场景都返回CONTINUE
     * Requirements: 2.5, 5.2, 5.3
     */
    @Test
    void testAlwaysReturnsContinueOnError() {
        // 测试1: null entity
        assertEquals(PluginResult.CONTINUE, plugin.handle(null, new PluginContext(null)));
        
        // 测试2: broadcast failure
        TextMessageEntity entity = createTextMessage();
        doThrow(new RuntimeException("Broadcast failed"))
            .when(messagingTemplate).convertAndSend(anyString(), any(MessageBroadcastDTO.class));
        assertEquals(PluginResult.CONTINUE, plugin.handle(entity, new PluginContext(null)));
    }

    /**
     * 测试CHANNEL_ADDED事件广播
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
     */
    @Test
    void testChannelAddedEventBroadcast() {
        // 准备测试数据
        ChannelMonitoringEvent event = ChannelMonitoringEvent.channelAdded(
            this, -1001234567890L, true);
        
        // 执行测试
        plugin.handleChannelMonitoringEvent(event);
        
        // 验证结果
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ChannelMonitoringNotificationDTO> dtoCaptor = 
            ArgumentCaptor.forClass(ChannelMonitoringNotificationDTO.class);
        
        verify(messagingTemplate).convertAndSend(topicCaptor.capture(), dtoCaptor.capture());
        
        // 验证topic路径
        assertEquals("/topic/channel/monitoring/added", topicCaptor.getValue());
        
        // 验证通知DTO
        ChannelMonitoringNotificationDTO dto = dtoCaptor.getValue();
        assertEquals("CHANNEL_ADDED", dto.getEventType());
        assertEquals(-1001234567890L, dto.getChannelId());
        assertTrue(dto.getMonitoringStatus());
        assertNotNull(dto.getTimestamp());
    }
    
    /**
     * 测试CHANNEL_REMOVED事件广播
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
     */
    @Test
    void testChannelRemovedEventBroadcast() {
        // 准备测试数据
        ChannelMonitoringEvent event = ChannelMonitoringEvent.channelRemoved(
            this, -1001234567890L);
        
        // 执行测试
        plugin.handleChannelMonitoringEvent(event);
        
        // 验证结果
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ChannelMonitoringNotificationDTO> dtoCaptor = 
            ArgumentCaptor.forClass(ChannelMonitoringNotificationDTO.class);
        
        verify(messagingTemplate).convertAndSend(topicCaptor.capture(), dtoCaptor.capture());
        
        // 验证topic路径
        assertEquals("/topic/channel/monitoring/removed", topicCaptor.getValue());
        
        // 验证通知DTO
        ChannelMonitoringNotificationDTO dto = dtoCaptor.getValue();
        assertEquals("CHANNEL_REMOVED", dto.getEventType());
        assertEquals(-1001234567890L, dto.getChannelId());
        assertNull(dto.getMonitoringStatus());
    }
    
    /**
     * 测试CHANNEL_UPDATED事件广播
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
     */
    @Test
    void testChannelUpdatedEventBroadcast() {
        // 准备测试数据
        ChannelMonitoringEvent event = ChannelMonitoringEvent.channelUpdated(
            this, -1001234567890L, false);
        
        // 执行测试
        plugin.handleChannelMonitoringEvent(event);
        
        // 验证结果
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ChannelMonitoringNotificationDTO> dtoCaptor = 
            ArgumentCaptor.forClass(ChannelMonitoringNotificationDTO.class);
        
        verify(messagingTemplate).convertAndSend(topicCaptor.capture(), dtoCaptor.capture());
        
        // 验证topic路径
        assertEquals("/topic/channel/monitoring/updated", topicCaptor.getValue());
        
        // 验证通知DTO
        ChannelMonitoringNotificationDTO dto = dtoCaptor.getValue();
        assertEquals("CHANNEL_UPDATED", dto.getEventType());
        assertEquals(-1001234567890L, dto.getChannelId());
        assertFalse(dto.getMonitoringStatus());
    }
    
    /**
     * 测试RELOAD_ALL事件广播
     * Requirements: 4.1, 4.2, 4.3, 4.4
     */
    @Test
    void testReloadAllEventBroadcast() {
        // 准备测试数据
        ChannelMonitoringEvent event = ChannelMonitoringEvent.reloadAll(this);
        
        // 执行测试
        plugin.handleChannelMonitoringEvent(event);
        
        // 验证结果
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSend(topicCaptor.capture(), any(ChannelMonitoringNotificationDTO.class));
        
        // 验证topic路径
        assertEquals("/topic/channel/monitoring/reload", topicCaptor.getValue());
    }
    
    /**
     * 测试事件处理失败不抛出异常
     * Requirements: 4.1, 5.2, 5.3
     */
    @Test
    void testEventHandlingFailureDoesNotPropagate() {
        // 准备测试数据
        ChannelMonitoringEvent event = ChannelMonitoringEvent.channelAdded(
            this, -1001234567890L, true);
        
        doThrow(new RuntimeException("Broadcast failed"))
            .when(messagingTemplate).convertAndSend(anyString(), any(ChannelMonitoringNotificationDTO.class));
        
        // 执行测试并验证 - 不应该抛出异常
        assertDoesNotThrow(() -> plugin.handleChannelMonitoringEvent(event));
    }

    // ========== 辅助方法 ==========
    
    private TextMessageEntity createTextMessage() {
        TextMessageEntity entity = new TextMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(-1001234567890L);
        entity.setChannelUsername("testchannel");
        entity.setChannelTitle("Test Channel");
        entity.setDate(1000000000);
        entity.setTextContent("Hello, World!");
        entity.setViews(100);
        entity.setForwards(10);
        return entity;
    }
    
    private PhotoMessageEntity createPhotoMessage() {
        PhotoMessageEntity entity = new PhotoMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(-1001234567890L);
        entity.setChannelUsername("testchannel");
        entity.setChannelTitle("Test Channel");
        entity.setDate(1000000000);
        entity.setCaption("Beautiful photo");
        entity.setViews(100);
        entity.setForwards(10);
        
        MediaFile photo = new MediaFile();
        photo.setFileId(111);
        photo.setFileSize(1024000L);
        photo.setMimeType("image/jpeg");
        photo.setWidth(1920);
        photo.setHeight(1080);
        entity.setPhotos(List.of(photo));
        
        return entity;
    }
    
    private VideoMessageEntity createVideoMessage() {
        VideoMessageEntity entity = new VideoMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(-1001234567890L);
        entity.setChannelUsername("testchannel");
        entity.setChannelTitle("Test Channel");
        entity.setDate(1000000000);
        entity.setCaption("Amazing video");
        entity.setViews(100);
        entity.setForwards(10);
        
        MediaFile video = new MediaFile();
        video.setFileId(222);
        video.setFileSize(5120000L);
        video.setMimeType("video/mp4");
        video.setWidth(1920);
        video.setHeight(1080);
        video.setDuration(120);
        entity.setVideo(video);
        
        return entity;
    }
    
    private TelegraphMessageEntity createTelegraphMessage() {
        TelegraphMessageEntity entity = new TelegraphMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(-1001234567890L);
        entity.setChannelUsername("testchannel");
        entity.setChannelTitle("Test Channel");
        entity.setDate(1000000000);
        entity.setTextContent("Check out this article");
        entity.setViews(100);
        entity.setForwards(10);
        
        WebPageInfo webPage = new WebPageInfo();
        webPage.setTitle("Amazing Article");
        webPage.setAuthor("John Doe");
        webPage.setUrl("https://example.com/article");
        webPage.setSiteName("Example Site");
        webPage.setDescription("This is an amazing article");
        webPage.setHasInstantView(true);
        webPage.setInstantViewVersion(2);
        entity.setWebPage(webPage);
        
        return entity;
    }
    
    private MediaGroupMessageEntity createMediaGroupMessage() {
        MediaGroupMessageEntity entity = new MediaGroupMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(-1001234567890L);
        entity.setChannelUsername("testchannel");
        entity.setChannelTitle("Test Channel");
        entity.setDate(1000000000);
        entity.setMediaAlbumId(99999L);
        entity.setIsMediaGroup(true);
        entity.setViews(200);
        entity.setForwards(20);
        
        // 创建媒体组中的消息
        PhotoMessageEntity photo1 = new PhotoMessageEntity();
        photo1.setMessageId(12346L);
        photo1.setChatId(-1001234567890L);
        photo1.setChannelUsername("testchannel");
        photo1.setChannelTitle("Test Channel");
        photo1.setDate(1000000000);
        photo1.setCaption("Photo 1");
        MediaFile file1 = new MediaFile();
        file1.setFileId(111);
        file1.setFileSize(1024000L);
        photo1.setPhotos(List.of(file1));
        
        PhotoMessageEntity photo2 = new PhotoMessageEntity();
        photo2.setMessageId(12347L);
        photo2.setChatId(-1001234567890L);
        photo2.setChannelUsername("testchannel");
        photo2.setChannelTitle("Test Channel");
        photo2.setDate(1000000000);
        photo2.setCaption("Photo 2");
        MediaFile file2 = new MediaFile();
        file2.setFileId(222);
        file2.setFileSize(2048000L);
        photo2.setPhotos(List.of(file2));
        
        entity.setItems(List.of(photo1, photo2));
        
        return entity;
    }
}
