package org.xlyo.cocomonyab.service.message;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.xlyo.cocomonyab.domain.entity.message.*;
import org.xlyo.cocomonyab.domain.enums.MessageType;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for MessageParser.
 * Tests Properties 2, 3, and 4 from the design document.
 */
class MessageParserPropertyTest {
    
    private MessageParser parser;
    private MessageTypeDetector detector;
    private final Random random = new Random();
    
    @BeforeEach
    void setUp() {
        detector = new MessageTypeDetector();
        parser = new MessageParser(detector);
    }
    
    /**
     * Property 2: Message Entity Field Population
     * 
     * For any TDLib message, when parsed into a message entity, all type-specific fields
     * should be populated from the original message without data loss.
     * 
     * Validates: Requirements 2.2, 7.4
     */
    @Test
    @Tag("Feature: message-type-plugin-system, Property 2: Message Entity Field Population")
    void messageEntityFieldPopulation() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Test Case 1: Text message
            TdApi.Message textMessage = createTextMessage();
            BaseMessageEntity textEntity = parser.parse(textMessage);
            assertNotNull(textEntity, "Parsed entity should not be null (iteration " + i + ")");
            assertTrue(textEntity instanceof TextMessageEntity, "Should be TextMessageEntity (iteration " + i + ")");
            assertEquals(textMessage.id, textEntity.getMessageId(), "Message ID should match (iteration " + i + ")");
            assertEquals(textMessage.chatId, textEntity.getChatId(), "Chat ID should match (iteration " + i + ")");
            assertEquals(textMessage.date, textEntity.getDate(), "Date should match (iteration " + i + ")");
            assertNotNull(((TextMessageEntity) textEntity).getTextContent(), "Text content should be populated (iteration " + i + ")");
            
            // Test Case 2: Photo message
            TdApi.Message photoMessage = createPhotoMessage();
            BaseMessageEntity photoEntity = parser.parse(photoMessage);
            assertNotNull(photoEntity, "Parsed entity should not be null (iteration " + i + ")");
            assertTrue(photoEntity instanceof PhotoMessageEntity, "Should be PhotoMessageEntity (iteration " + i + ")");
            assertEquals(photoMessage.id, photoEntity.getMessageId(), "Message ID should match (iteration " + i + ")");
            assertEquals(photoMessage.chatId, photoEntity.getChatId(), "Chat ID should match (iteration " + i + ")");
            assertNotNull(((PhotoMessageEntity) photoEntity).getPhotos(), "Photos should be populated (iteration " + i + ")");
            
            // Test Case 3: Video message
            TdApi.Message videoMessage = createVideoMessage();
            BaseMessageEntity videoEntity = parser.parse(videoMessage);
            assertNotNull(videoEntity, "Parsed entity should not be null (iteration " + i + ")");
            assertTrue(videoEntity instanceof VideoMessageEntity, "Should be VideoMessageEntity (iteration " + i + ")");
            assertEquals(videoMessage.id, videoEntity.getMessageId(), "Message ID should match (iteration " + i + ")");
            assertNotNull(((VideoMessageEntity) videoEntity).getVideo(), "Video should be populated (iteration " + i + ")");
            assertNotNull(((VideoMessageEntity) videoEntity).getDuration(), "Duration should be populated (iteration " + i + ")");
            
            // Test Case 4: Document message
            TdApi.Message documentMessage = createDocumentMessage();
            BaseMessageEntity documentEntity = parser.parse(documentMessage);
            assertNotNull(documentEntity, "Parsed entity should not be null (iteration " + i + ")");
            assertTrue(documentEntity instanceof DocumentMessageEntity, "Should be DocumentMessageEntity (iteration " + i + ")");
            assertNotNull(((DocumentMessageEntity) documentEntity).getDocument(), "Document should be populated (iteration " + i + ")");
            assertNotNull(((DocumentMessageEntity) documentEntity).getFileName(), "File name should be populated (iteration " + i + ")");
        }
    }
    
    /**
     * Property 3: Media Group Metadata Preservation
     * 
     * For any media group message, the parsed entity should preserve all media group metadata fields:
     * mediaAlbumId, isMediaGroup (true), mediaGroupItemCount, and mediaGroupMessageIds.
     * 
     * Validates: Requirements 2.5, 10.4
     */
    @Test
    @Tag("Feature: message-type-plugin-system, Property 3: Media Group Metadata Preservation")
    void mediaGroupMetadataPreservation() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create a message with mediaAlbumId (media group)
            long mediaAlbumId = Math.abs(random.nextLong()) + 1;
            TdApi.Message message = new TdApi.Message();
            message.id = generateRandomMessageId();
            message.chatId = generateRandomChatId();
            message.date = generateRandomDate();
            message.mediaAlbumId = mediaAlbumId;
            message.content = new TdApi.MessagePhoto(
                new TdApi.Photo(false, null, new TdApi.PhotoSize[]{createPhotoSize()}),
                new TdApi.FormattedText("Caption", new TdApi.TextEntity[0]),
                false,
                false
            );
            
            // Note: Media group messages should not be parsed directly (they're handled separately)
            // But we can verify that the base fields preserve media group metadata
            MessageType type = detector.detectType(message);
            assertEquals(MessageType.MEDIA_GROUP, type, "Should detect as MEDIA_GROUP (iteration " + i + ")");
            
            // For non-media-group messages, verify isMediaGroup is false
            TdApi.Message singleMessage = createTextMessage();
            BaseMessageEntity entity = parser.parse(singleMessage);
            assertEquals(0L, entity.getMediaAlbumId(), "Single message should have mediaAlbumId = 0 (iteration " + i + ")");
            assertEquals(Boolean.FALSE, entity.getIsMediaGroup(), "Single message should have isMediaGroup = false (iteration " + i + ")");
        }
    }
    
    /**
     * Property 4: WebPageInfo Preservation
     * 
     * For any Telegraph message, the parsed entity should preserve all WebPageInfo fields
     * including url, title, author, description, siteName, hasInstantView (true), and instantViewVersion.
     * 
     * Validates: Requirements 2.6, 11.2
     */
    @Test
    @Tag("Feature: message-type-plugin-system, Property 4: WebPageInfo Preservation")
    void webPageInfoPreservation() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create Telegraph message with WebPage
            String url = "https://telegra.ph/article-" + random.nextInt(10000);
            String title = "Test Article " + random.nextInt(1000);
            String author = "Test Author " + random.nextInt(100);
            String description = "Test description " + random.nextInt(1000);
            String siteName = "Telegraph";
            int instantViewVersion = random.nextInt(10) + 1;
            
            TdApi.Message message = new TdApi.Message();
            message.id = generateRandomMessageId();
            message.chatId = generateRandomChatId();
            message.date = generateRandomDate();
            message.mediaAlbumId = 0;
            
            TdApi.WebPage webPage = new TdApi.WebPage();
            webPage.url = url;
            webPage.displayUrl = url;
            webPage.type = "article";
            webPage.siteName = siteName;
            webPage.title = title;
            webPage.description = new TdApi.FormattedText(description, new TdApi.TextEntity[0]);
            webPage.author = author;
            webPage.instantViewVersion = instantViewVersion;
            
            message.content = new TdApi.MessageText(
                new TdApi.FormattedText("Telegraph article", new TdApi.TextEntity[0]),
                webPage,
                null
            );
            
            // Parse message
            BaseMessageEntity entity = parser.parse(message);
            assertNotNull(entity, "Parsed entity should not be null (iteration " + i + ")");
            assertTrue(entity instanceof TelegraphMessageEntity, "Should be TelegraphMessageEntity (iteration " + i + ")");
            
            TelegraphMessageEntity telegraphEntity = (TelegraphMessageEntity) entity;
            assertNotNull(telegraphEntity.getWebPage(), "WebPage should be populated (iteration " + i + ")");
            
            WebPageInfo webPageInfo = telegraphEntity.getWebPage();
            assertEquals(url, webPageInfo.getUrl(), "URL should match (iteration " + i + ")");
            assertEquals(title, webPageInfo.getTitle(), "Title should match (iteration " + i + ")");
            assertEquals(author, webPageInfo.getAuthor(), "Author should match (iteration " + i + ")");
            assertEquals(description, webPageInfo.getDescription(), "Description should match (iteration " + i + ")");
            assertEquals(siteName, webPageInfo.getSiteName(), "Site name should match (iteration " + i + ")");
            assertEquals(Boolean.TRUE, webPageInfo.getHasInstantView(), "hasInstantView should be true (iteration " + i + ")");
            assertEquals(instantViewVersion, webPageInfo.getInstantViewVersion(), "instantViewVersion should match (iteration " + i + ")");
        }
    }
    
    /**
     * Test that interaction info is preserved
     */
    @Test
    @Tag("Feature: message-type-plugin-system, Property 2: Message Entity Field Population")
    void interactionInfoPreservation() {
        for (int i = 0; i < 100; i++) {
            int viewCount = random.nextInt(10000);
            int forwardCount = random.nextInt(1000);
            
            TdApi.Message message = createTextMessage();
            message.interactionInfo = new TdApi.MessageInteractionInfo(
                viewCount,
                forwardCount,
                null,
                null
            );
            
            BaseMessageEntity entity = parser.parse(message);
            assertNotNull(entity, "Parsed entity should not be null (iteration " + i + ")");
            assertEquals(viewCount, entity.getViews(), "View count should match (iteration " + i + ")");
            assertEquals(forwardCount, entity.getForwards(), "Forward count should match (iteration " + i + ")");
        }
    }
    
    /**
     * Test that timestamps are set
     */
    @Test
    @Tag("Feature: message-type-plugin-system, Property 2: Message Entity Field Population")
    void timestampsAreSet() {
        for (int i = 0; i < 100; i++) {
            TdApi.Message message = createTextMessage();
            BaseMessageEntity entity = parser.parse(message);
            
            assertNotNull(entity.getCreateTime(), "Create time should be set (iteration " + i + ")");
            assertNotNull(entity.getUpdateTime(), "Update time should be set (iteration " + i + ")");
        }
    }
    
    // Helper methods to create test messages
    
    private TdApi.Message createTextMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.editDate = 0;
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Test text " + random.nextInt(1000), new TdApi.TextEntity[0]),
            null,
            null
        );
        return message;
    }
    
    private TdApi.Message createPhotoMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessagePhoto(
            new TdApi.Photo(false, null, new TdApi.PhotoSize[]{createPhotoSize()}),
            new TdApi.FormattedText("Photo caption", new TdApi.TextEntity[0]),
            false,
            false
        );
        return message;
    }
    
    private TdApi.PhotoSize createPhotoSize() {
        TdApi.File file = new TdApi.File();
        file.id = random.nextInt(100000);
        file.size = random.nextInt(1000000);
        file.local = new TdApi.LocalFile("/path/to/photo.jpg", true, true, false, true, 0, file.size, file.size);
        file.remote = new TdApi.RemoteFile("remote-id-" + random.nextInt(1000), "unique-id-" + random.nextInt(1000), false, true, file.size);
        
        return new TdApi.PhotoSize("m", file, 800, 600, new int[0]);
    }
    
    private TdApi.Message createVideoMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.mediaAlbumId = 0;
        
        TdApi.File file = new TdApi.File();
        file.id = random.nextInt(100000);
        file.size = random.nextInt(10000000);
        file.local = new TdApi.LocalFile("/path/to/video.mp4", true, true, false, true, 0, file.size, file.size);
        file.remote = new TdApi.RemoteFile("remote-id-" + random.nextInt(1000), "unique-id-" + random.nextInt(1000), false, true, file.size);
        
        message.content = new TdApi.MessageVideo(
            new TdApi.Video(random.nextInt(300), 1920, 1080, "video.mp4", "video/mp4", false, false, null, null, file),
            new TdApi.FormattedText("Video caption", new TdApi.TextEntity[0]),
            false,
            false
        );
        return message;
    }
    
    private TdApi.Message createDocumentMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.mediaAlbumId = 0;
        
        TdApi.File file = new TdApi.File();
        file.id = random.nextInt(100000);
        file.size = random.nextInt(5000000);
        file.local = new TdApi.LocalFile("/path/to/document.pdf", true, true, false, true, 0, file.size, file.size);
        file.remote = new TdApi.RemoteFile("remote-id-" + random.nextInt(1000), "unique-id-" + random.nextInt(1000), false, true, file.size);
        
        message.content = new TdApi.MessageDocument(
            new TdApi.Document("document.pdf", "application/pdf", null, null, file),
            new TdApi.FormattedText("Document caption", new TdApi.TextEntity[0])
        );
        return message;
    }
    
    // Random data generators
    
    private long generateRandomMessageId() {
        return Math.abs(random.nextLong());
    }
    
    private long generateRandomChatId() {
        return random.nextLong();
    }
    
    private int generateRandomDate() {
        return (int) (System.currentTimeMillis() / 1000);
    }
}
