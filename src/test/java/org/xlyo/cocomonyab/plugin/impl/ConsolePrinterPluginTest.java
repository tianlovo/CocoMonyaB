package org.xlyo.cocomonyab.plugin.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.xlyo.cocomonyab.domain.entity.message.*;
import org.xlyo.cocomonyab.plugin.PluginContext;
import org.xlyo.cocomonyab.plugin.PluginResult;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConsolePrinterPlugin 单元测试
 * 测试单条消息输出格式、媒体组输出格式、Telegraph输出格式
 */
class ConsolePrinterPluginTest {
    
    private ConsolePrinterPlugin plugin;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;
    
    @BeforeEach
    void setUp() {
        plugin = new ConsolePrinterPlugin();
        
        // 设置日志捕获
        logger = (Logger) LoggerFactory.getLogger(ConsolePrinterPlugin.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }
    
    /**
     * 测试插件基本属性
     * Requirements: 6.1
     */
    @Test
    void testPluginProperties() {
        assertEquals("ConsolePrinterPlugin", plugin.getName());
        assertEquals(0, plugin.getPriority()); // 最低优先级
        assertTrue(plugin.isEnabled());
    }
    
    /**
     * 测试文本消息输出格式
     * Requirements: 6.3, 15.3
     */
    @Test
    void testTextMessageOutput() {
        TextMessageEntity entity = new TextMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(67890L);
        entity.setChannelUsername("testchannel");
        entity.setChannelTitle("Test Channel");
        entity.setDate(1000000000);
        entity.setTextContent("Hello, World!");
        entity.setViews(100);
        entity.setForwards(10);
        
        TdApi.Message message = createTestMessage(entity);
        PluginContext context = new PluginContext(message);
        
        PluginResult result = plugin.handle(entity, context);
        
        assertEquals(PluginResult.CONTINUE, result);
        
        List<String> logMessages = getLogMessages();
        String allLogs = String.join("\n", logMessages);
        
        // 验证输出包含必要信息
        assertTrue(allLogs.contains("收到新消息"));
        assertTrue(allLogs.contains("Test Channel"));
        assertTrue(allLogs.contains("testchannel"));
        assertTrue(allLogs.contains("12345"));
        assertTrue(allLogs.contains("67890"));
        assertTrue(allLogs.contains("Hello, World!"));
        assertTrue(allLogs.contains("100"));
        assertTrue(allLogs.contains("10"));
        assertTrue(allLogs.contains("互动"));
    }
    
    /**
     * 测试图片消息输出格式
     * Requirements: 6.3, 15.3
     */
    @Test
    void testPhotoMessageOutput() {
        PhotoMessageEntity entity = new PhotoMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(67890L);
        entity.setChannelUsername("testchannel");
        entity.setChannelTitle("Test Channel");
        entity.setDate(1000000000);
        entity.setCaption("Beautiful photo");
        
        MediaFile photo = new MediaFile();
        photo.setFileId(111);
        photo.setFileSize(1024000L);
        entity.setPhotos(List.of(photo));
        
        TdApi.Message message = createTestMessage(entity);
        PluginContext context = new PluginContext(message);
        
        PluginResult result = plugin.handle(entity, context);
        
        assertEquals(PluginResult.CONTINUE, result);
        
        List<String> logMessages = getLogMessages();
        String allLogs = String.join("\n", logMessages);
        
        // 验证输出包含必要信息
        assertTrue(allLogs.contains("收到新消息"));
        assertTrue(allLogs.contains("Beautiful photo"));
        assertTrue(allLogs.contains("媒体文件"));
        assertTrue(allLogs.contains("1024000"));
    }
    
    /**
     * 测试视频消息输出格式
     * Requirements: 6.3, 15.3
     */
    @Test
    void testVideoMessageOutput() {
        VideoMessageEntity entity = new VideoMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(67890L);
        entity.setChannelUsername("testchannel");
        entity.setChannelTitle("Test Channel");
        entity.setDate(1000000000);
        entity.setCaption("Amazing video");
        
        MediaFile video = new MediaFile();
        video.setFileId(222);
        video.setFileSize(5120000L);
        entity.setVideo(video);
        
        TdApi.Message message = createTestMessage(entity);
        PluginContext context = new PluginContext(message);
        
        PluginResult result = plugin.handle(entity, context);
        
        assertEquals(PluginResult.CONTINUE, result);
        
        List<String> logMessages = getLogMessages();
        String allLogs = String.join("\n", logMessages);
        
        // 验证输出包含必要信息
        assertTrue(allLogs.contains("收到新消息"));
        assertTrue(allLogs.contains("Amazing video"));
        assertTrue(allLogs.contains("媒体文件"));
        assertTrue(allLogs.contains("5120000"));
    }
    
    /**
     * 测试投票消息输出格式
     * Requirements: 6.3, 15.3
     */
    @Test
    void testPollMessageOutput() {
        PollMessageEntity entity = new PollMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(67890L);
        entity.setChannelUsername("testchannel");
        entity.setChannelTitle("Test Channel");
        entity.setDate(1000000000);
        entity.setQuestion("What is your favorite color?");
        
        TdApi.Message message = createTestMessage(entity);
        PluginContext context = new PluginContext(message);
        
        PluginResult result = plugin.handle(entity, context);
        
        assertEquals(PluginResult.CONTINUE, result);
        
        List<String> logMessages = getLogMessages();
        String allLogs = String.join("\n", logMessages);
        
        // 验证输出包含必要信息
        assertTrue(allLogs.contains("收到新消息"));
        assertTrue(allLogs.contains("What is your favorite color?"));
    }
    
    /**
     * 测试媒体组消息输出格式
     * Requirements: 6.4, 15.3
     */
    @Test
    void testMediaGroupOutput() {
        MediaGroupMessageEntity entity = new MediaGroupMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(67890L);
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
        photo1.setCaption("Photo 1");
        MediaFile file1 = new MediaFile();
        file1.setFileId(111);
        file1.setFileSize(1024000L);
        photo1.setPhotos(List.of(file1));
        
        PhotoMessageEntity photo2 = new PhotoMessageEntity();
        photo2.setMessageId(12347L);
        photo2.setCaption("Photo 2");
        MediaFile file2 = new MediaFile();
        file2.setFileId(222);
        file2.setFileSize(2048000L);
        photo2.setPhotos(List.of(file2));
        
        entity.setItems(List.of(photo1, photo2));
        
        TdApi.Message message = createTestMessage(entity);
        PluginContext context = new PluginContext(message);
        
        PluginResult result = plugin.handle(entity, context);
        
        assertEquals(PluginResult.CONTINUE, result);
        
        List<String> logMessages = getLogMessages();
        String allLogs = String.join("\n", logMessages);
        
        // 验证输出包含必要信息
        assertTrue(allLogs.contains("收到媒体组消息"));
        assertTrue(allLogs.contains("Test Channel"));
        assertTrue(allLogs.contains("99999")); // 媒体组ID
        assertTrue(allLogs.contains("2 条")); // 消息数量
        assertTrue(allLogs.contains("媒体组内容"));
        assertTrue(allLogs.contains("12346")); // 第一条消息ID
        assertTrue(allLogs.contains("12347")); // 第二条消息ID
        assertTrue(allLogs.contains("互动"));
        assertTrue(allLogs.contains("200"));
        assertTrue(allLogs.contains("20"));
    }
    
    /**
     * 测试Telegraph消息输出格式
     * Requirements: 6.5, 15.3
     */
    @Test
    void testTelegraphOutput() {
        TelegraphMessageEntity entity = new TelegraphMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(67890L);
        entity.setChannelUsername("testchannel");
        entity.setChannelTitle("Test Channel");
        entity.setDate(1000000000);
        entity.setTextContent("Check out this article");
        
        WebPageInfo webPage = new WebPageInfo();
        webPage.setTitle("Amazing Article");
        webPage.setAuthor("John Doe");
        webPage.setUrl("https://example.com/article");
        webPage.setSiteName("Example Site");
        webPage.setDescription("This is an amazing article about something interesting");
        webPage.setHasInstantView(true);
        webPage.setInstantViewVersion(2);
        entity.setWebPage(webPage);
        
        TdApi.Message message = createTestMessage(entity);
        PluginContext context = new PluginContext(message);
        
        PluginResult result = plugin.handle(entity, context);
        
        assertEquals(PluginResult.CONTINUE, result);
        
        List<String> logMessages = getLogMessages();
        String allLogs = String.join("\n", logMessages);
        
        // 验证输出包含必要信息
        assertTrue(allLogs.contains("收到新消息"));
        assertTrue(allLogs.contains("Check out this article"));
        assertTrue(allLogs.contains("WebPage 信息"));
        assertTrue(allLogs.contains("Telegraph 文章"));
        assertTrue(allLogs.contains("Amazing Article"));
        assertTrue(allLogs.contains("John Doe"));
        assertTrue(allLogs.contains("https://example.com/article"));
        assertTrue(allLogs.contains("Example Site"));
        assertTrue(allLogs.contains("即时预览"));
    }
    
    /**
     * 测试没有互动信息的消息
     * Requirements: 6.6
     */
    @Test
    void testMessageWithoutInteractionInfo() {
        TextMessageEntity entity = new TextMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(67890L);
        entity.setChannelUsername("testchannel");
        entity.setChannelTitle("Test Channel");
        entity.setDate(1000000000);
        entity.setTextContent("Hello");
        // 不设置views和forwards
        
        TdApi.Message message = createTestMessage(entity);
        PluginContext context = new PluginContext(message);
        
        PluginResult result = plugin.handle(entity, context);
        
        assertEquals(PluginResult.CONTINUE, result);
        
        List<String> logMessages = getLogMessages();
        String allLogs = String.join("\n", logMessages);
        
        // 验证输出不包含互动信息
        assertFalse(allLogs.contains("互动"));
    }
    
    /**
     * 测试空内容消息
     * Requirements: 6.3
     */
    @Test
    void testMessageWithEmptyContent() {
        TextMessageEntity entity = new TextMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(67890L);
        entity.setChannelUsername("testchannel");
        entity.setChannelTitle("Test Channel");
        entity.setDate(1000000000);
        entity.setTextContent(""); // 空内容
        
        TdApi.Message message = createTestMessage(entity);
        PluginContext context = new PluginContext(message);
        
        // 应该不抛出异常
        assertDoesNotThrow(() -> plugin.handle(entity, context));
    }
    
    /**
     * 测试null字段处理
     * Requirements: 6.3
     */
    @Test
    void testMessageWithNullFields() {
        TextMessageEntity entity = new TextMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(67890L);
        entity.setDate(1000000000);
        // channelUsername, channelTitle, textContent都为null
        
        TdApi.Message message = createTestMessage(entity);
        PluginContext context = new PluginContext(message);
        
        // 应该不抛出异常
        assertDoesNotThrow(() -> plugin.handle(entity, context));
    }
    
    /**
     * 测试插件总是返回CONTINUE
     * Requirements: 6.1
     */
    @Test
    void testPluginAlwaysReturnsContinue() {
        TextMessageEntity entity = new TextMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(67890L);
        entity.setDate(1000000000);
        
        TdApi.Message message = createTestMessage(entity);
        PluginContext context = new PluginContext(message);
        
        PluginResult result = plugin.handle(entity, context);
        
        assertEquals(PluginResult.CONTINUE, result);
    }
    
    /**
     * 测试长文本截断（媒体组中）
     * Requirements: 6.4
     */
    @Test
    void testLongCaptionTruncationInMediaGroup() {
        MediaGroupMessageEntity entity = new MediaGroupMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(67890L);
        entity.setChannelUsername("testchannel");
        entity.setChannelTitle("Test Channel");
        entity.setDate(1000000000);
        entity.setMediaAlbumId(99999L);
        entity.setIsMediaGroup(true);
        
        PhotoMessageEntity photo = new PhotoMessageEntity();
        photo.setMessageId(12346L);
        // 创建超过50个字符的caption
        photo.setCaption("A".repeat(60));
        MediaFile file = new MediaFile();
        file.setFileId(111);
        file.setFileSize(1024000L);
        photo.setPhotos(List.of(file));
        
        entity.setItems(List.of(photo));
        
        TdApi.Message message = createTestMessage(entity);
        PluginContext context = new PluginContext(message);
        
        plugin.handle(entity, context);
        
        List<String> logMessages = getLogMessages();
        String allLogs = String.join("\n", logMessages);
        
        // 验证长文本被截断
        assertTrue(allLogs.contains("..."));
    }
    
    /**
     * 测试WebPage描述截断
     * Requirements: 6.5
     */
    @Test
    void testWebPageDescriptionTruncation() {
        TelegraphMessageEntity entity = new TelegraphMessageEntity();
        entity.setMessageId(12345L);
        entity.setChatId(67890L);
        entity.setChannelUsername("testchannel");
        entity.setChannelTitle("Test Channel");
        entity.setDate(1000000000);
        entity.setTextContent("Article");
        
        WebPageInfo webPage = new WebPageInfo();
        webPage.setTitle("Article");
        // 创建超过100个字符的描述
        webPage.setDescription("A".repeat(120));
        webPage.setHasInstantView(true);
        webPage.setInstantViewVersion(1);
        entity.setWebPage(webPage);
        
        TdApi.Message message = createTestMessage(entity);
        PluginContext context = new PluginContext(message);
        
        plugin.handle(entity, context);
        
        List<String> logMessages = getLogMessages();
        String allLogs = String.join("\n", logMessages);
        
        // 验证描述被截断
        assertTrue(allLogs.contains("..."));
    }
    
    // ========== 辅助方法 ==========
    
    private List<String> getLogMessages() {
        return listAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .collect(Collectors.toList());
    }
    
    private TdApi.Message createTestMessage(BaseMessageEntity entity) {
        TdApi.Message message = new TdApi.Message();
        message.id = entity.getMessageId();
        message.chatId = entity.getChatId();
        message.date = entity.getDate();
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Test", new TdApi.TextEntity[0]), null, null);
        return message;
    }
}
