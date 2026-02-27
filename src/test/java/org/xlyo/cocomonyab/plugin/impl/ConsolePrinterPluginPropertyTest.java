package org.xlyo.cocomonyab.plugin.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import it.tdlight.jni.TdApi;
import net.jqwik.api.*;
import org.slf4j.LoggerFactory;
import org.xlyo.cocomonyab.domain.entity.message.*;
import org.xlyo.cocomonyab.domain.enums.MessageType;
import org.xlyo.cocomonyab.plugin.PluginContext;
import org.xlyo.cocomonyab.plugin.PluginResult;
import org.xlyo.cocomonyab.plugin.console.ConsolePrinterPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ConsolePrinterPlugin 属性测试
 * 使用 jqwik 进行基于属性的测试
 */
class ConsolePrinterPluginPropertyTest {
    
    /**
     * Property 9: Console Output Completeness
     * 验证控制台输出包含消息类型、内容、媒体文件和互动统计
     * 
     * **Validates: Requirements 6.2, 6.6**
     */
    @Property(tries = 100)
    @Label("Property 9: Console Output Completeness")
    void consoleOutputShouldContainAllRequiredFields(
            @ForAll("messageEntities") BaseMessageEntity entity) {
        
        ConsolePrinterPlugin plugin = new ConsolePrinterPlugin();
        
        // 设置日志捕获
        Logger logger = (Logger) LoggerFactory.getLogger(ConsolePrinterPlugin.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        
        try {
            // 执行插件
            TdApi.Message message = createTestMessage(entity);
            PluginContext context = new PluginContext(message);
            PluginResult result = plugin.handle(entity, context);
            
            // 验证返回值
            if (result != PluginResult.CONTINUE) {
                throw new AssertionError("ConsolePrinterPlugin should always return CONTINUE");
            }
            
            // 获取日志输出
            List<String> logMessages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
            
            String allLogs = String.join("\n", logMessages);
            
            // 验证：输出应该包含消息类型
            if (!containsMessageType(allLogs, entity.getType())) {
                throw new AssertionError(
                    String.format("Console output should contain message type '%s', but got: %s",
                        entity.getType().getDescription(), allLogs));
            }
            
            // 验证：输出应该包含频道信息
            if (entity.getChannelTitle() != null && !allLogs.contains(entity.getChannelTitle())) {
                throw new AssertionError(
                    String.format("Console output should contain channel title '%s'", 
                        entity.getChannelTitle()));
            }
            
            if (entity.getChannelUsername() != null && !allLogs.contains(entity.getChannelUsername())) {
                throw new AssertionError(
                    String.format("Console output should contain channel username '%s'", 
                        entity.getChannelUsername()));
            }
            
            // 验证：输出应该包含消息ID或媒体组ID
            if (entity instanceof MediaGroupMessageEntity mediaGroup) {
                // 对于媒体组，检查媒体组ID
                if (mediaGroup.getMediaAlbumId() != null && 
                    !allLogs.contains(String.valueOf(mediaGroup.getMediaAlbumId()))) {
                    throw new AssertionError(
                        String.format("Console output should contain media album ID '%d'", 
                            mediaGroup.getMediaAlbumId()));
                }
            } else {
                // 对于单条消息，检查消息ID
                if (!allLogs.contains(String.valueOf(entity.getMessageId()))) {
                    throw new AssertionError(
                        String.format("Console output should contain message ID '%d'", 
                            entity.getMessageId()));
                }
            }
            
            // 验证：输出应该包含频道ID
            if (!allLogs.contains(String.valueOf(entity.getChatId()))) {
                throw new AssertionError(
                    String.format("Console output should contain chat ID '%d'", 
                        entity.getChatId()));
            }
            
            // 验证：如果有内容，输出应该包含内容
            verifyContentInOutput(entity, allLogs);
            
            // 验证：如果有媒体文件，输出应该包含媒体文件信息
            verifyMediaFilesInOutput(entity, allLogs);
            
            // 验证：如果有互动统计，输出应该包含互动统计
            if (entity.getViews() != null || entity.getForwards() != null) {
                if (!allLogs.contains("互动")) {
                    throw new AssertionError("Console output should contain interaction statistics");
                }
                
                if (entity.getViews() != null && !allLogs.contains(String.valueOf(entity.getViews()))) {
                    throw new AssertionError(
                        String.format("Console output should contain views count '%d'", 
                            entity.getViews()));
                }
                
                if (entity.getForwards() != null && !allLogs.contains(String.valueOf(entity.getForwards()))) {
                    throw new AssertionError(
                        String.format("Console output should contain forwards count '%d'", 
                            entity.getForwards()));
                }
            }
            
        } finally {
            logger.detachAppender(listAppender);
        }
    }
    
    // ========== 辅助方法 ==========
    
    private boolean containsMessageType(String logs, MessageType type) {
        // 检查是否包含类型描述或类型代码
        return logs.contains(type.getDescription()) || logs.contains(type.getCode());
    }
    
    private void verifyContentInOutput(BaseMessageEntity entity, String allLogs) {
        switch (entity) {
            case TextMessageEntity text -> {
                if (text.getTextContent() != null && !text.getTextContent().isEmpty()) {
                    if (!allLogs.contains(text.getTextContent())) {
                        throw new AssertionError(
                            String.format("Console output should contain text content '%s'", 
                                text.getTextContent()));
                    }
                }
            }
            case TelegraphMessageEntity telegraph -> {
                if (telegraph.getTextContent() != null && !telegraph.getTextContent().isEmpty()) {
                    if (!allLogs.contains(telegraph.getTextContent())) {
                        throw new AssertionError(
                            String.format("Console output should contain telegraph text content '%s'", 
                                telegraph.getTextContent()));
                    }
                }
                if (telegraph.getWebPage() != null) {
                    WebPageInfo webPage = telegraph.getWebPage();
                    if (webPage.getTitle() != null && !allLogs.contains(webPage.getTitle())) {
                        throw new AssertionError(
                            String.format("Console output should contain webpage title '%s'", 
                                webPage.getTitle()));
                    }
                }
            }
            case PhotoMessageEntity photo -> {
                if (photo.getCaption() != null && !photo.getCaption().isEmpty()) {
                    if (!allLogs.contains(photo.getCaption())) {
                        throw new AssertionError(
                            String.format("Console output should contain photo caption '%s'", 
                                photo.getCaption()));
                    }
                }
            }
            case VideoMessageEntity video -> {
                if (video.getCaption() != null && !video.getCaption().isEmpty()) {
                    if (!allLogs.contains(video.getCaption())) {
                        throw new AssertionError(
                            String.format("Console output should contain video caption '%s'", 
                                video.getCaption()));
                    }
                }
            }
            case PollMessageEntity poll -> {
                if (poll.getQuestion() != null && !poll.getQuestion().isEmpty()) {
                    if (!allLogs.contains(poll.getQuestion())) {
                        throw new AssertionError(
                            String.format("Console output should contain poll question '%s'", 
                                poll.getQuestion()));
                    }
                }
            }
            default -> {
                // 其他类型不验证内容
            }
        }
    }
    
    private void verifyMediaFilesInOutput(BaseMessageEntity entity, String allLogs) {
        switch (entity) {
            case PhotoMessageEntity photo -> {
                if (photo.getPhotos() != null && !photo.getPhotos().isEmpty()) {
                    if (!allLogs.contains("媒体文件")) {
                        throw new AssertionError("Console output should contain media files section for photo");
                    }
                }
            }
            case VideoMessageEntity video -> {
                if (video.getVideo() != null) {
                    if (!allLogs.contains("媒体文件")) {
                        throw new AssertionError("Console output should contain media files section for video");
                    }
                }
            }
            case DocumentMessageEntity doc -> {
                if (doc.getDocument() != null) {
                    if (!allLogs.contains("媒体文件")) {
                        throw new AssertionError("Console output should contain media files section for document");
                    }
                }
            }
            case AudioMessageEntity audio -> {
                if (audio.getAudio() != null) {
                    if (!allLogs.contains("媒体文件")) {
                        throw new AssertionError("Console output should contain media files section for audio");
                    }
                }
            }
            case MediaGroupMessageEntity mediaGroup -> {
                if (mediaGroup.getItems() != null && !mediaGroup.getItems().isEmpty()) {
                    if (!allLogs.contains("媒体组内容")) {
                        throw new AssertionError("Console output should contain media group content section");
                    }
                }
            }
            default -> {
                // 其他类型不验证媒体文件
            }
        }
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
    
    // ========== 数据生成器 ==========
    
    @Provide
    Arbitrary<BaseMessageEntity> messageEntities() {
        return Arbitraries.oneOf(
            textMessages(),
            telegraphMessages(),
            photoMessages(),
            videoMessages(),
            pollMessages(),
            mediaGroupMessages()
        );
    }
    
    @Provide
    Arbitrary<TextMessageEntity> textMessages() {
        return Combinators.combine(
            baseFields(),
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100)
        ).as((base, content) -> {
            TextMessageEntity entity = new TextMessageEntity();
            applyBaseFields(entity, base);
            entity.setTextContent(content);
            return entity;
        });
    }
    
    @Provide
    Arbitrary<TelegraphMessageEntity> telegraphMessages() {
        return Combinators.combine(
            baseFields(),
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100),
            webPageInfo()
        ).as((base, content, webPage) -> {
            TelegraphMessageEntity entity = new TelegraphMessageEntity();
            applyBaseFields(entity, base);
            entity.setTextContent(content);
            entity.setWebPage(webPage);
            return entity;
        });
    }
    
    @Provide
    Arbitrary<PhotoMessageEntity> photoMessages() {
        return Combinators.combine(
            baseFields(),
            Arbitraries.strings().alpha().ofMaxLength(50),
            mediaFiles().list().ofMinSize(1).ofMaxSize(3)
        ).as((base, caption, photos) -> {
            PhotoMessageEntity entity = new PhotoMessageEntity();
            applyBaseFields(entity, base);
            entity.setCaption(caption);
            entity.setPhotos(photos);
            return entity;
        });
    }
    
    @Provide
    Arbitrary<VideoMessageEntity> videoMessages() {
        return Combinators.combine(
            baseFields(),
            Arbitraries.strings().alpha().ofMaxLength(50),
            mediaFiles()
        ).as((base, caption, video) -> {
            VideoMessageEntity entity = new VideoMessageEntity();
            applyBaseFields(entity, base);
            entity.setCaption(caption);
            entity.setVideo(video);
            return entity;
        });
    }
    
    @Provide
    Arbitrary<PollMessageEntity> pollMessages() {
        return Combinators.combine(
            baseFields(),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(100)
        ).as((base, question) -> {
            PollMessageEntity entity = new PollMessageEntity();
            applyBaseFields(entity, base);
            entity.setQuestion(question);
            return entity;
        });
    }
    
    @Provide
    Arbitrary<MediaGroupMessageEntity> mediaGroupMessages() {
        return Combinators.combine(
            baseFields(),
            Arbitraries.longs().greaterOrEqual(1L),
            photoMessages().list().ofMinSize(2).ofMaxSize(5)
        ).as((base, albumId, items) -> {
            MediaGroupMessageEntity entity = new MediaGroupMessageEntity();
            applyBaseFields(entity, base);
            entity.setMediaAlbumId(albumId);
            entity.setIsMediaGroup(true);
            entity.setItems(new ArrayList<>(items));
            return entity;
        });
    }
    
    @Provide
    Arbitrary<BaseFields> baseFields() {
        return Combinators.combine(
            Arbitraries.longs().greaterOrEqual(1L),
            Arbitraries.longs().greaterOrEqual(1L),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(30),
            Arbitraries.integers().greaterOrEqual(1000000000),
            Arbitraries.integers().between(0, 1000).injectNull(0.5),
            Arbitraries.integers().between(0, 1000).injectNull(0.5)
        ).as(BaseFields::new);
    }
    
    @Provide
    Arbitrary<MediaFile> mediaFiles() {
        return Combinators.combine(
            Arbitraries.integers().between(1, 999999),
            Arbitraries.longs().between(1000L, 10000000L)
        ).as((fileId, fileSize) -> {
            MediaFile file = new MediaFile();
            file.setFileId(fileId);
            file.setFileSize(fileSize);
            return file;
        });
    }
    
    @Provide
    Arbitrary<WebPageInfo> webPageInfo() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(30),
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20)
        ).as((title, url, author) -> {
            WebPageInfo info = new WebPageInfo();
            info.setTitle(title);
            info.setUrl(url);
            info.setAuthor(author);
            info.setHasInstantView(true);
            info.setInstantViewVersion(1);
            return info;
        });
    }
    
    private void applyBaseFields(BaseMessageEntity entity, BaseFields fields) {
        entity.setMessageId(fields.messageId);
        entity.setChatId(fields.chatId);
        entity.setChannelUsername(fields.channelUsername);
        entity.setChannelTitle(fields.channelTitle);
        entity.setDate(fields.date);
        entity.setViews(fields.views);
        entity.setForwards(fields.forwards);
    }
    
    record BaseFields(
        Long messageId,
        Long chatId,
        String channelUsername,
        String channelTitle,
        Integer date,
        Integer views,
        Integer forwards
    ) {}
}
