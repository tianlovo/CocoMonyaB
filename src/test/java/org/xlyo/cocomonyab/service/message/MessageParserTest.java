package org.xlyo.cocomonyab.service.message;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xlyo.cocomonyab.domain.entity.message.*;
import org.xlyo.cocomonyab.domain.enums.MessageType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MessageParser.
 * Tests specific scenarios and edge cases.
 */
class MessageParserTest {
    
    private MessageParser parser;
    private MessageTypeDetector detector;
    
    @BeforeEach
    void setUp() {
        detector = new MessageTypeDetector();
        parser = new MessageParser(detector);
    }
    
    @Test
    void parseTextMessage_Success() {
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.date = 1234567890;
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Hello World", new TdApi.TextEntity[0]),
            null,
            null
        );
        
        BaseMessageEntity entity = parser.parse(message);
        
        assertNotNull(entity);
        assertTrue(entity instanceof TextMessageEntity);
        assertEquals(MessageType.TEXT, entity.getType());
        assertEquals(123L, entity.getMessageId());
        assertEquals(456L, entity.getChatId());
        assertEquals("Hello World", ((TextMessageEntity) entity).getTextContent());
    }
    
    @Test
    void parseTelegraphMessage_Success() {
        TdApi.WebPage webPage = new TdApi.WebPage();
        webPage.url = "https://telegra.ph/article";
        webPage.title = "Test Article";
        webPage.author = "Test Author";
        webPage.instantViewVersion = 2;
        webPage.description = new TdApi.FormattedText("Description", new TdApi.TextEntity[0]);
        
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.date = 1234567890;
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Telegraph article", new TdApi.TextEntity[0]),
            webPage,
            null
        );
        
        BaseMessageEntity entity = parser.parse(message);
        
        assertNotNull(entity);
        assertTrue(entity instanceof TelegraphMessageEntity);
        assertEquals(MessageType.TELEGRAPH, entity.getType());
        
        TelegraphMessageEntity telegraphEntity = (TelegraphMessageEntity) entity;
        assertNotNull(telegraphEntity.getWebPage());
        assertEquals("https://telegra.ph/article", telegraphEntity.getWebPage().getUrl());
        assertEquals("Test Article", telegraphEntity.getWebPage().getTitle());
        assertEquals("Test Author", telegraphEntity.getWebPage().getAuthor());
        assertEquals(2, telegraphEntity.getWebPage().getInstantViewVersion());
        assertTrue(telegraphEntity.getWebPage().getHasInstantView());
    }
    
    @Test
    void parsePhotoMessage_Success() {
        TdApi.File file = new TdApi.File();
        file.id = 1;
        file.size = 1000;
        file.local = new TdApi.LocalFile("/path/photo.jpg", true, true, false, true, 0, 1000, 1000);
        file.remote = new TdApi.RemoteFile("remote-id", "unique-id", false, true, 1000);
        
        TdApi.PhotoSize photoSize = new TdApi.PhotoSize("m", file, 800, 600, new int[0]);
        
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.date = 1234567890;
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessagePhoto(
            new TdApi.Photo(false, null, new TdApi.PhotoSize[]{photoSize}),
            new TdApi.FormattedText("Photo caption", new TdApi.TextEntity[0]),
            false,
            false
        );
        
        BaseMessageEntity entity = parser.parse(message);
        
        assertNotNull(entity);
        assertTrue(entity instanceof PhotoMessageEntity);
        assertEquals(MessageType.PHOTO, entity.getType());
        
        PhotoMessageEntity photoEntity = (PhotoMessageEntity) entity;
        assertEquals("Photo caption", photoEntity.getCaption());
        assertNotNull(photoEntity.getPhotos());
        assertEquals(1, photoEntity.getPhotos().size());
        assertEquals(800, photoEntity.getPhotos().get(0).getWidth());
        assertEquals(600, photoEntity.getPhotos().get(0).getHeight());
    }
    
    @Test
    void parseMessage_WithInteractionInfo() {
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.date = 1234567890;
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Test", new TdApi.TextEntity[0]),
            null,
            null
        );
        message.interactionInfo = new TdApi.MessageInteractionInfo(
            1000,
            50,
            null,
            null
        );
        
        BaseMessageEntity entity = parser.parse(message);
        
        assertNotNull(entity);
        assertEquals(1000, entity.getViews());
        assertEquals(50, entity.getForwards());
    }
    
    @Test
    void parseMessage_WithoutInteractionInfo() {
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.date = 1234567890;
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Test", new TdApi.TextEntity[0]),
            null,
            null
        );
        message.interactionInfo = null;
        
        BaseMessageEntity entity = parser.parse(message);
        
        assertNotNull(entity);
        assertNull(entity.getViews());
        assertNull(entity.getForwards());
    }
    
    @Test
    void parseMessage_MediaGroupThrowsException() {
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.date = 1234567890;
        message.mediaAlbumId = 999L; // Non-zero = media group
        message.content = new TdApi.MessagePhoto(
            new TdApi.Photo(false, null, new TdApi.PhotoSize[0]),
            new TdApi.FormattedText("", new TdApi.TextEntity[0]),
            false,
            false
        );
        
        assertThrows(IllegalStateException.class, () -> parser.parse(message));
    }
    
    @Test
    void parseOtherMessage_Success() {
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.date = 1234567890;
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageContact(
            new TdApi.Contact("", "", "", "", 0)
        );
        
        BaseMessageEntity entity = parser.parse(message);
        
        assertNotNull(entity);
        assertTrue(entity instanceof OtherMessageEntity);
        assertEquals(MessageType.OTHER, entity.getType());
        assertEquals("MessageContact", ((OtherMessageEntity) entity).getContentTypeName());
    }
    
    @Test
    void parseMessage_TimestampsAreSet() {
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.date = 1234567890;
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Test", new TdApi.TextEntity[0]),
            null,
            null
        );
        
        BaseMessageEntity entity = parser.parse(message);
        
        assertNotNull(entity.getCreateTime());
        assertNotNull(entity.getUpdateTime());
    }
    
    @Test
    void parseMessage_MediaAlbumIdPreserved() {
        TdApi.Message message = new TdApi.Message();
        message.id = 123L;
        message.chatId = 456L;
        message.date = 1234567890;
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Test", new TdApi.TextEntity[0]),
            null,
            null
        );
        
        BaseMessageEntity entity = parser.parse(message);
        
        assertEquals(0L, entity.getMediaAlbumId());
        assertEquals(Boolean.FALSE, entity.getIsMediaGroup());
    }
}
