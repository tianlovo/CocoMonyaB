package org.xlyo.cocomonyab.service.message;

import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.xlyo.cocomonyab.domain.enums.MessageType;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for MessageTypeDetector.
 * Tests Property 1 from the design document.
 * 
 * Note: Using JUnit @Test with manual property generation instead of jqwik
 * because we need to generate complex TDLib message objects.
 */
class MessageTypeDetectorPropertyTest {
    
    private final MessageTypeDetector detector = new MessageTypeDetector();
    private final Random random = new Random();
    
    /**
     * Property 1: Message Type Detection Correctness
     * 
     * For any TDLib message, the MessageTypeDetector should map it to the correct MessageType
     * enum value based on its content type and attributes (e.g., messages with mediaAlbumId != 0
     * should be MEDIA_GROUP, text messages with instantViewVersion > 0 should be TELEGRAPH).
     * 
     * Validates: Requirements 1.2, 1.3, 7.6, 11.1
     */
    @Test
    @Tag("Feature: message-type-plugin-system, Property 1: Message Type Detection Correctness")
    void messageTypeDetectionCorrectness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Test Case 1: Media group messages (mediaAlbumId != 0)
            TdApi.Message mediaGroupMessage = createMessageWithMediaAlbumId();
            MessageType mediaGroupType = detector.detectType(mediaGroupMessage);
            assertEquals(MessageType.MEDIA_GROUP, mediaGroupType,
                "Message with mediaAlbumId != 0 should be detected as MEDIA_GROUP (iteration " + i + ")");
            
            // Test Case 2: Text messages
            TdApi.Message textMessage = createTextMessage(false);
            MessageType textType = detector.detectType(textMessage);
            assertEquals(MessageType.TEXT, textType,
                "MessageText without webPage should be detected as TEXT (iteration " + i + ")");
            
            // Test Case 3: Telegraph messages (text with instantViewVersion > 0)
            TdApi.Message telegraphMessage = createTelegraphMessage();
            MessageType telegraphType = detector.detectType(telegraphMessage);
            assertEquals(MessageType.TELEGRAPH, telegraphType,
                "MessageText with instantViewVersion > 0 should be detected as TELEGRAPH (iteration " + i + ")");
            
            // Test Case 4: Photo messages
            TdApi.Message photoMessage = createPhotoMessage();
            MessageType photoType = detector.detectType(photoMessage);
            assertEquals(MessageType.PHOTO, photoType,
                "MessagePhoto should be detected as PHOTO (iteration " + i + ")");
            
            // Test Case 5: Video messages
            TdApi.Message videoMessage = createVideoMessage();
            MessageType videoType = detector.detectType(videoMessage);
            assertEquals(MessageType.VIDEO, videoType,
                "MessageVideo should be detected as VIDEO (iteration " + i + ")");
            
            // Test Case 6: Document messages
            TdApi.Message documentMessage = createDocumentMessage();
            MessageType documentType = detector.detectType(documentMessage);
            assertEquals(MessageType.DOCUMENT, documentType,
                "MessageDocument should be detected as DOCUMENT (iteration " + i + ")");
            
            // Test Case 7: Audio messages
            TdApi.Message audioMessage = createAudioMessage();
            MessageType audioType = detector.detectType(audioMessage);
            assertEquals(MessageType.AUDIO, audioType,
                "MessageAudio should be detected as AUDIO (iteration " + i + ")");
            
            // Test Case 8: Voice messages
            TdApi.Message voiceMessage = createVoiceMessage();
            MessageType voiceType = detector.detectType(voiceMessage);
            assertEquals(MessageType.VOICE, voiceType,
                "MessageVoiceNote should be detected as VOICE (iteration " + i + ")");
            
            // Test Case 9: Video note messages
            TdApi.Message videoNoteMessage = createVideoNoteMessage();
            MessageType videoNoteType = detector.detectType(videoNoteMessage);
            assertEquals(MessageType.VIDEO_NOTE, videoNoteType,
                "MessageVideoNote should be detected as VIDEO_NOTE (iteration " + i + ")");
            
            // Test Case 10: Animation messages
            TdApi.Message animationMessage = createAnimationMessage();
            MessageType animationType = detector.detectType(animationMessage);
            assertEquals(MessageType.ANIMATION, animationType,
                "MessageAnimation should be detected as ANIMATION (iteration " + i + ")");
            
            // Test Case 11: Sticker messages
            TdApi.Message stickerMessage = createStickerMessage();
            MessageType stickerType = detector.detectType(stickerMessage);
            assertEquals(MessageType.STICKER, stickerType,
                "MessageSticker should be detected as STICKER (iteration " + i + ")");
            
            // Test Case 12: Poll messages
            TdApi.Message pollMessage = createPollMessage();
            MessageType pollType = detector.detectType(pollMessage);
            assertEquals(MessageType.POLL, pollType,
                "MessagePoll should be detected as POLL (iteration " + i + ")");
            
            // Test Case 13: Other message types
            TdApi.Message otherMessage = createOtherMessage();
            MessageType otherType = detector.detectType(otherMessage);
            assertEquals(MessageType.OTHER, otherType,
                "Unsupported message content should be detected as OTHER (iteration " + i + ")");
        }
    }
    
    /**
     * Test that media group detection takes precedence over content type
     */
    @Test
    @Tag("Feature: message-type-plugin-system, Property 1: Message Type Detection Correctness")
    void mediaGroupDetectionTakesPrecedence() {
        for (int i = 0; i < 100; i++) {
            // Create a photo message with mediaAlbumId (media group)
            TdApi.Message message = new TdApi.Message();
            message.id = generateRandomMessageId();
            message.chatId = generateRandomChatId();
            message.date = generateRandomDate();
            message.mediaAlbumId = generateRandomMediaAlbumId(); // Non-zero
            message.content = new TdApi.MessagePhoto(
                new TdApi.Photo(false, null, new TdApi.PhotoSize[0]),
                new TdApi.FormattedText("", new TdApi.TextEntity[0]),
                false,
                false
            );
            
            MessageType type = detector.detectType(message);
            assertEquals(MessageType.MEDIA_GROUP, type,
                "Message with mediaAlbumId should be MEDIA_GROUP regardless of content type (iteration " + i + ")");
        }
    }
    
    /**
     * Test Telegraph detection requires both webPage and instantViewVersion > 0
     */
    @Test
    @Tag("Feature: message-type-plugin-system, Property 1: Message Type Detection Correctness")
    void telegraphDetectionRequiresInstantView() {
        for (int i = 0; i < 100; i++) {
            // Test Case 1: Text with webPage but instantViewVersion = 0
            TdApi.Message textWithWebPage = new TdApi.Message();
            textWithWebPage.id = generateRandomMessageId();
            textWithWebPage.chatId = generateRandomChatId();
            textWithWebPage.date = generateRandomDate();
            textWithWebPage.mediaAlbumId = 0;
            
            TdApi.WebPage webPage = new TdApi.WebPage();
            webPage.instantViewVersion = 0; // No instant view
            webPage.url = "https://example.com";
            
            textWithWebPage.content = new TdApi.MessageText(
                new TdApi.FormattedText("Text with link", new TdApi.TextEntity[0]),
                webPage,
                null
            );
            
            MessageType type1 = detector.detectType(textWithWebPage);
            assertEquals(MessageType.TEXT, type1,
                "Text with webPage but instantViewVersion=0 should be TEXT (iteration " + i + ")");
            
            // Test Case 2: Text without webPage
            TdApi.Message textWithoutWebPage = new TdApi.Message();
            textWithoutWebPage.id = generateRandomMessageId();
            textWithoutWebPage.chatId = generateRandomChatId();
            textWithoutWebPage.date = generateRandomDate();
            textWithoutWebPage.mediaAlbumId = 0;
            textWithoutWebPage.content = new TdApi.MessageText(
                new TdApi.FormattedText("Plain text", new TdApi.TextEntity[0]),
                null,
                null
            );
            
            MessageType type2 = detector.detectType(textWithoutWebPage);
            assertEquals(MessageType.TEXT, type2,
                "Text without webPage should be TEXT (iteration " + i + ")");
        }
    }
    
    // Helper methods to create test messages
    
    private TdApi.Message createMessageWithMediaAlbumId() {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.mediaAlbumId = generateRandomMediaAlbumId();
        message.content = new TdApi.MessagePhoto(
            new TdApi.Photo(false, null, new TdApi.PhotoSize[0]),
            new TdApi.FormattedText("", new TdApi.TextEntity[0]),
            false,
            false
        );
        return message;
    }
    
    private TdApi.Message createTextMessage(boolean withWebPage) {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Test text", new TdApi.TextEntity[0]),
            null,
            null
        );
        return message;
    }
    
    private TdApi.Message createTelegraphMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.mediaAlbumId = 0;
        
        TdApi.WebPage webPage = new TdApi.WebPage();
        webPage.instantViewVersion = random.nextInt(10) + 1; // 1-10
        webPage.url = "https://telegra.ph/article-" + random.nextInt(1000);
        webPage.displayUrl = webPage.url;
        webPage.type = "article";
        webPage.siteName = "Telegraph";
        webPage.title = "Test Article";
        webPage.description = new TdApi.FormattedText("Test description", new TdApi.TextEntity[0]);
        
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Telegraph article", new TdApi.TextEntity[0]),
            webPage,
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
            new TdApi.Photo(false, null, new TdApi.PhotoSize[0]),
            new TdApi.FormattedText("Photo caption", new TdApi.TextEntity[0]),
            false,
            false
        );
        return message;
    }
    
    private TdApi.Message createVideoMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageVideo(
            new TdApi.Video(0, 1920, 1080, "video.mp4", "video/mp4", false, false, null, null, null),
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
        message.content = new TdApi.MessageDocument(
            new TdApi.Document("document.pdf", "application/pdf", null, null, null),
            new TdApi.FormattedText("Document caption", new TdApi.TextEntity[0])
        );
        return message;
    }
    
    private TdApi.Message createAudioMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageAudio(
            new TdApi.Audio(180, "Song Title", "Artist", "audio.mp3", "audio/mpeg", null, null, null, null),
            new TdApi.FormattedText("Audio caption", new TdApi.TextEntity[0])
        );
        return message;
    }
    
    private TdApi.Message createVoiceMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageVoiceNote(
            new TdApi.VoiceNote(30, new byte[0], "audio/ogg", null, null),
            new TdApi.FormattedText("Voice caption", new TdApi.TextEntity[0]),
            false
        );
        return message;
    }
    
    private TdApi.Message createVideoNoteMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageVideoNote(
            new TdApi.VideoNote(10, new byte[0], 240, null, null, null, null),
            false,
            false
        );
        return message;
    }
    
    private TdApi.Message createAnimationMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageAnimation(
            new TdApi.Animation(5, 480, 360, "animation.gif", "animation.gif", false, null, null, null),
            new TdApi.FormattedText("Animation caption", new TdApi.TextEntity[0]),
            false,
            false
        );
        return message;
    }
    
    private TdApi.Message createStickerMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessageSticker(
            new TdApi.Sticker(0, 0, 512, 512, "😀", new TdApi.StickerFormatWebp(), new TdApi.StickerFullTypeRegular((TdApi.File) null), new TdApi.ClosedVectorPath[0], null, null),
            false
        );
        return message;
    }
    
    private TdApi.Message createPollMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.mediaAlbumId = 0;
        message.content = new TdApi.MessagePoll(
            new TdApi.Poll(0, "Poll question?", new TdApi.PollOption[0], 0, new TdApi.MessageSender[0], false, new TdApi.PollTypeRegular(false), 0, 0, false)
        );
        return message;
    }
    
    private TdApi.Message createOtherMessage() {
        TdApi.Message message = new TdApi.Message();
        message.id = generateRandomMessageId();
        message.chatId = generateRandomChatId();
        message.date = generateRandomDate();
        message.mediaAlbumId = 0;
        // Use a message type that's not explicitly handled
        message.content = new TdApi.MessageContact(
            new TdApi.Contact("", "", "", "", 0)
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
    
    private long generateRandomMediaAlbumId() {
        return Math.abs(random.nextLong()) + 1; // Non-zero
    }
}
