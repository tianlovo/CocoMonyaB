package org.xlyo.cocomonyab.plugin.impl.websocket;

import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.xlyo.cocomonyab.domain.entity.message.*;
import org.xlyo.cocomonyab.plugin.PluginContext;
import org.xlyo.cocomonyab.plugin.PluginResult;
import org.xlyo.cocomonyab.plugin.impl.websocket.config.WebSocketBroadcastProperties;
import org.xlyo.cocomonyab.plugin.impl.websocket.dto.MessageBroadcastDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * WebSocketBroadcastPlugin 属性测试
 * 使用jqwik进行基于属性的测试，每个属性测试100次迭代
 */
class WebSocketBroadcastPluginPropertyTest {
    
    private static final String TOPIC_PREFIX = "/topic/channel/real";
    private static final String MONITORING_TOPIC_PREFIX = "/topic/channel/monitoring";
    
    // ========== 属性测试 ==========
    
    /**
     * 属性 1: 消息广播到正确的Topic
     * 验证: 需求 2.1, 2.2
     * 
     * 对于任意接收到的消息实体，插件应该将其转换为MessageBroadcastDTO并广播到格式为`/topic/channel/real/{chatId}`的topic
     */
    @Property(tries = 100)
    @Label("Feature: websocket-broadcast-plugin, Property 1: 消息广播到正确的Topic")
    void shouldBroadcastToCorrectTopicForAnyMessage(@ForAll("messages") BaseMessageEntity entity) {
        // Given
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBroadcastProperties properties = mock(WebSocketBroadcastProperties.class);
        when(properties.getTopicPrefix()).thenReturn(TOPIC_PREFIX);
        
        WebSocketBroadcastPlugin plugin = new WebSocketBroadcastPlugin(template, properties);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        
        // When
        PluginResult result = plugin.handle(entity, new PluginContext(null));
        
        // Then
        assertEquals(PluginResult.CONTINUE, result);
        verify(template).convertAndSend(topicCaptor.capture(), any(MessageBroadcastDTO.class));
        
        String expectedTopic = TOPIC_PREFIX + "/" + entity.getChatId();
        assertEquals(expectedTopic, topicCaptor.getValue());
    }
    
    /**
     * 属性 2: DTO包含所有必需字段
     * 验证: 需求 2.3, 3.2, 3.3
     * 
     * 对于任意BaseMessageEntity，转换后的MessageBroadcastDTO应该包含所有基础字段
     */
    @Property(tries = 100)
    @Label("Feature: websocket-broadcast-plugin, Property 2: DTO包含所有必需字段")
    void shouldIncludeAllRequiredFieldsInDTO(@ForAll("messages") BaseMessageEntity entity) {
        // Given
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBroadcastProperties properties = mock(WebSocketBroadcastProperties.class);
        when(properties.getTopicPrefix()).thenReturn(TOPIC_PREFIX);
        
        WebSocketBroadcastPlugin plugin = new WebSocketBroadcastPlugin(template, properties);
        ArgumentCaptor<MessageBroadcastDTO> dtoCaptor = ArgumentCaptor.forClass(MessageBroadcastDTO.class);
        
        // When
        plugin.handle(entity, new PluginContext(null));
        
        // Then
        verify(template).convertAndSend(any(String.class), dtoCaptor.capture());
        MessageBroadcastDTO dto = dtoCaptor.getValue();
        
        // 验证所有必需字段
        assertNotNull(dto.getMessageId());
        assertNotNull(dto.getChatId());
        assertNotNull(dto.getChannelUsername());
        assertNotNull(dto.getChannelTitle());
        assertNotNull(dto.getDate());
        assertNotNull(dto.getContentType());
        // textContent可以为null（某些消息类型如STICKER）
        // views和forwards可以为null（新消息）
    }
    
    /**
     * 属性 3: 处理所有消息类型
     * 验证: 需求 2.4
     * 
     * 对于任意消息类型，插件应该能够成功处理并广播，不抛出异常
     */
    @Property(tries = 100)
    @Label("Feature: websocket-broadcast-plugin, Property 3: 处理所有消息类型")
    void shouldHandleAllMessageTypesWithoutException(@ForAll("messages") BaseMessageEntity entity) {
        // Given
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBroadcastProperties properties = mock(WebSocketBroadcastProperties.class);
        when(properties.getTopicPrefix()).thenReturn(TOPIC_PREFIX);
        
        WebSocketBroadcastPlugin plugin = new WebSocketBroadcastPlugin(template, properties);
        
        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> plugin.handle(entity, new PluginContext(null)));
        
        // 验证消息被广播
        verify(template).convertAndSend(any(String.class), any(MessageBroadcastDTO.class));
    }
    
    /**
     * 属性 4: 媒体信息正确包含
     * 验证: 需求 3.4
     * 
     * 对于任意包含媒体文件的消息，转换后的DTO应该包含相应的媒体文件信息
     */
    @Property(tries = 100)
    @Label("Feature: websocket-broadcast-plugin, Property 4: 媒体信息正确包含")
    void shouldIncludeMediaInformationForMediaMessages(@ForAll("mediaMessages") BaseMessageEntity entity) {
        // Given
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBroadcastProperties properties = mock(WebSocketBroadcastProperties.class);
        when(properties.getTopicPrefix()).thenReturn(TOPIC_PREFIX);
        
        WebSocketBroadcastPlugin plugin = new WebSocketBroadcastPlugin(template, properties);
        ArgumentCaptor<MessageBroadcastDTO> dtoCaptor = ArgumentCaptor.forClass(MessageBroadcastDTO.class);
        
        // When
        plugin.handle(entity, new PluginContext(null));
        
        // Then
        verify(template).convertAndSend(any(String.class), dtoCaptor.capture());
        MessageBroadcastDTO dto = dtoCaptor.getValue();
        
        // 验证媒体字段存在
        if (entity instanceof PhotoMessageEntity) {
            assertNotNull(dto.getPhotos(), "PHOTO消息应该包含photos字段");
        } else if (entity instanceof VideoMessageEntity) {
            assertNotNull(dto.getVideo(), "VIDEO消息应该包含video字段");
        }
    }
    
    /**
     * 属性 5: JSON序列化往返一致性
     * 验证: 需求 3.7
     * 
     * 对于任意MessageBroadcastDTO对象，序列化为JSON后再反序列化应该得到等价的对象
     */
    @Property(tries = 100)
    @Label("Feature: websocket-broadcast-plugin, Property 5: JSON序列化往返一致性")
    void shouldPreserveDataAfterJsonRoundTrip(@ForAll("dtos") MessageBroadcastDTO original) {
        // Given
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        
        // When & Then - 序列化和反序列化应该成功
        assertDoesNotThrow(() -> {
            String json = mapper.writeValueAsString(original);
            MessageBroadcastDTO deserialized = mapper.readValue(json, MessageBroadcastDTO.class);
            
            // 验证关键字段相等
            assertEquals(original.getMessageId(), deserialized.getMessageId());
            assertEquals(original.getChatId(), deserialized.getChatId());
            assertEquals(original.getChannelUsername(), deserialized.getChannelUsername());
            assertEquals(original.getChannelTitle(), deserialized.getChannelTitle());
            assertEquals(original.getDate(), deserialized.getDate());
            assertEquals(original.getContentType(), deserialized.getContentType());
        });
    }
    
    /**
     * 属性 7: 错误不中断插件链
     * 验证: 需求 2.5, 5.2, 5.3
     * 
     * 对于任意处理过程中发生的异常，插件应该捕获异常、记录日志并返回PluginResult.CONTINUE
     */
    @Property(tries = 100)
    @Label("Feature: websocket-broadcast-plugin, Property 7: 错误不中断插件链")
    void shouldAlwaysReturnContinueOnError(@ForAll("messages") BaseMessageEntity entity) {
        // Given - 模拟广播失败
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        WebSocketBroadcastProperties properties = mock(WebSocketBroadcastProperties.class);
        when(properties.getTopicPrefix()).thenReturn(TOPIC_PREFIX);
        
        doThrow(new RuntimeException("Broadcast failed"))
            .when(template).convertAndSend(any(String.class), any(MessageBroadcastDTO.class));
        
        WebSocketBroadcastPlugin plugin = new WebSocketBroadcastPlugin(template, properties);
        
        // When
        PluginResult result = plugin.handle(entity, new PluginContext(null));
        
        // Then - 应该返回CONTINUE，不抛出异常
        assertEquals(PluginResult.CONTINUE, result);
        assertDoesNotThrow(() -> plugin.handle(entity, new PluginContext(null)));
    }
    
    // ========== 测试数据生成器 ==========
    
    /**
     * 生成随机BaseMessageEntity
     * 包含所有消息类型：TEXT, PHOTO, VIDEO, DOCUMENT, AUDIO, VOICE, VIDEO_NOTE, ANIMATION, STICKER, POLL, TELEGRAPH, MEDIA_GROUP
     */
    @Provide
    Arbitrary<BaseMessageEntity> messages() {
        return Arbitraries.frequencyOf(
            Tuple.of(5, textMessages()),           // 50% 文本消息
            Tuple.of(2, photoMessages()),          // 20% 图片消息
            Tuple.of(1, videoMessages()),          // 10% 视频消息
            Tuple.of(1, telegraphMessages()),      // 10% Telegraph消息
            Tuple.of(1, mediaGroupMessages())      // 10% 媒体组消息
        );
    }
    
    /**
     * 生成TEXT类型消息
     */
    @Provide
    Arbitrary<TextMessageEntity> textMessages() {
        return Combinators.combine(
            Arbitraries.longs().greaterOrEqual(1),
            Arbitraries.longs().between(-1002000000000L, -1001000000000L),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50),
            Arbitraries.strings().ofMinLength(1).ofMaxLength(200),
            Arbitraries.integers().between(0, 10000),
            Arbitraries.integers().between(0, 1000)
        ).as((msgId, chatId, username, title, text, views, forwards) -> {
            TextMessageEntity entity = new TextMessageEntity();
            entity.setMessageId(msgId);
            entity.setChatId(chatId);
            entity.setChannelUsername(username);
            entity.setChannelTitle(title);
            entity.setTextContent(text);
            entity.setDate((int) (System.currentTimeMillis() / 1000));
            entity.setViews(views);
            entity.setForwards(forwards);
            return entity;
        });
    }
    
    /**
     * 生成PHOTO类型消息
     */
    @Provide
    Arbitrary<PhotoMessageEntity> photoMessages() {
        return Combinators.combine(
            Arbitraries.longs().greaterOrEqual(1),
            Arbitraries.longs().between(-1002000000000L, -1001000000000L),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50),
            Arbitraries.strings().ofMinLength(0).ofMaxLength(200),
            mediaFiles()
        ).as((msgId, chatId, username, title, caption, photos) -> {
            PhotoMessageEntity entity = new PhotoMessageEntity();
            entity.setMessageId(msgId);
            entity.setChatId(chatId);
            entity.setChannelUsername(username);
            entity.setChannelTitle(title);
            entity.setCaption(caption);
            entity.setDate((int) (System.currentTimeMillis() / 1000));
            entity.setPhotos(photos);
            entity.setViews(Arbitraries.integers().between(0, 10000).sample());
            entity.setForwards(Arbitraries.integers().between(0, 1000).sample());
            return entity;
        });
    }
    
    /**
     * 生成VIDEO类型消息
     */
    @Provide
    Arbitrary<VideoMessageEntity> videoMessages() {
        return Combinators.combine(
            Arbitraries.longs().greaterOrEqual(1),
            Arbitraries.longs().between(-1002000000000L, -1001000000000L),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50),
            Arbitraries.strings().ofMinLength(0).ofMaxLength(200),
            mediaFile()
        ).as((msgId, chatId, username, title, caption, video) -> {
            VideoMessageEntity entity = new VideoMessageEntity();
            entity.setMessageId(msgId);
            entity.setChatId(chatId);
            entity.setChannelUsername(username);
            entity.setChannelTitle(title);
            entity.setCaption(caption);
            entity.setDate((int) (System.currentTimeMillis() / 1000));
            entity.setVideo(video);
            entity.setViews(Arbitraries.integers().between(0, 10000).sample());
            entity.setForwards(Arbitraries.integers().between(0, 1000).sample());
            return entity;
        });
    }
    
    /**
     * 生成TELEGRAPH类型消息
     */
    @Provide
    Arbitrary<TelegraphMessageEntity> telegraphMessages() {
        return Combinators.combine(
            Arbitraries.longs().greaterOrEqual(1),
            Arbitraries.longs().between(-1002000000000L, -1001000000000L),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50),
            Arbitraries.strings().ofMinLength(1).ofMaxLength(200),
            webPage()
        ).as((msgId, chatId, username, title, text, webPage) -> {
            TelegraphMessageEntity entity = new TelegraphMessageEntity();
            entity.setMessageId(msgId);
            entity.setChatId(chatId);
            entity.setChannelUsername(username);
            entity.setChannelTitle(title);
            entity.setTextContent(text);
            entity.setDate((int) (System.currentTimeMillis() / 1000));
            entity.setWebPage(webPage);
            entity.setViews(Arbitraries.integers().between(0, 10000).sample());
            entity.setForwards(Arbitraries.integers().between(0, 1000).sample());
            return entity;
        });
    }
    
    /**
     * 生成MEDIA_GROUP类型消息
     */
    @Provide
    Arbitrary<MediaGroupMessageEntity> mediaGroupMessages() {
        return Combinators.combine(
            Arbitraries.longs().greaterOrEqual(1),
            Arbitraries.longs().between(-1002000000000L, -1001000000000L),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50),
            Arbitraries.longs().greaterOrEqual(1),
            photoMessages().list().ofMinSize(1).ofMaxSize(5)
        ).as((msgId, chatId, username, title, albumId, items) -> {
            MediaGroupMessageEntity entity = new MediaGroupMessageEntity();
            entity.setMessageId(msgId);
            entity.setChatId(chatId);
            entity.setChannelUsername(username);
            entity.setChannelTitle(title);
            entity.setDate((int) (System.currentTimeMillis() / 1000));
            entity.setMediaAlbumId(albumId);
            entity.setIsMediaGroup(true);
            // Convert List<PhotoMessageEntity> to List<BaseMessageEntity>
            entity.setItems(items.stream()
                .map(photo -> (BaseMessageEntity) photo)
                .collect(java.util.stream.Collectors.toList()));
            entity.setViews(Arbitraries.integers().between(0, 10000).sample());
            entity.setForwards(Arbitraries.integers().between(0, 1000).sample());
            return entity;
        });
    }
    
    /**
     * 生成MediaFile
     */
    @Provide
    Arbitrary<MediaFile> mediaFile() {
        return Combinators.combine(
            Arbitraries.integers().greaterOrEqual(1),
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(50),
            Arbitraries.longs().between(1000L, 10000000L),
            Arbitraries.of("image/jpeg", "image/png", "video/mp4", "audio/mpeg"),
            Arbitraries.integers().between(100, 4096),
            Arbitraries.integers().between(100, 4096)
        ).as((fileId, uniqueId, size, mimeType, width, height) -> {
            MediaFile file = new MediaFile();
            file.setFileId(fileId);
            file.setFileUniqueId(uniqueId);
            file.setFileSize(size);
            file.setMimeType(mimeType);
            file.setWidth(width);
            file.setHeight(height);
            return file;
        });
    }
    
    /**
     * 生成MediaFile列表
     */
    @Provide
    Arbitrary<List<MediaFile>> mediaFiles() {
        return mediaFile().list().ofMinSize(1).ofMaxSize(3);
    }
    
    /**
     * 生成WebPageInfo
     */
    @Provide
    Arbitrary<WebPageInfo> webPage() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(100),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50),
            Arbitraries.strings().withCharRange('a', 'z').ofMinLength(20).ofMaxLength(200),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(30),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(30),
            Arbitraries.of(true, false)
        ).as((title, author, description, siteName, url, hasInstantView) -> {
            WebPageInfo webPage = new WebPageInfo();
            webPage.setTitle(title);
            webPage.setAuthor(author);
            webPage.setDescription(description);
            webPage.setSiteName(siteName);
            webPage.setUrl("https://" + url + ".com");
            webPage.setHasInstantView(hasInstantView);
            webPage.setInstantViewVersion(hasInstantView ? 2 : null);
            return webPage;
        });
    }
    
    /**
     * 生成MessageBroadcastDTO
     */
    @Provide
    Arbitrary<MessageBroadcastDTO> dtos() {
        return Combinators.combine(
            Arbitraries.longs().greaterOrEqual(1),
            Arbitraries.longs().between(-1002000000000L, -1001000000000L),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50),
            Arbitraries.integers().greaterOrEqual(1000000000),
            Arbitraries.of("TEXT", "PHOTO", "VIDEO", "TELEGRAPH", "MEDIA_GROUP"),
            Arbitraries.strings().ofMinLength(0).ofMaxLength(200),
            Arbitraries.integers().between(0, 10000)
        ).as((msgId, chatId, username, title, date, contentType, text, views) -> 
            MessageBroadcastDTO.builder()
                .messageId(msgId)
                .chatId(chatId)
                .channelUsername(username)
                .channelTitle(title)
                .date(date)
                .contentType(contentType)
                .textContent(text)
                .views(views)
                .forwards(Arbitraries.integers().between(0, 1000).sample())
                .build()
        );
    }
    
    /**
     * 生成包含媒体的消息（PHOTO, VIDEO, DOCUMENT等）
     */
    @Provide
    Arbitrary<BaseMessageEntity> mediaMessages() {
        return Arbitraries.frequencyOf(
            Tuple.of(1, photoMessages()),
            Tuple.of(1, videoMessages())
        );
    }
}
