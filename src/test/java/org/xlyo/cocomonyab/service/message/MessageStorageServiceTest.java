package org.xlyo.cocomonyab.service.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.tdlight.jni.TdApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xlyo.cocomonyab.domain.entity.RawMessage;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageStorageService.
 * Tests message saving, deduplication, JSON serialization, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class MessageStorageServiceTest {
    
    @Mock
    private RawMessageRepository rawMessageRepository;
    
    @Mock
    private ObjectMapper objectMapper;
    
    private MessageStorageService messageStorageService;
    
    @BeforeEach
    void setUp() {
        messageStorageService = new MessageStorageService(rawMessageRepository, objectMapper);
    }
    
    @Test
    void saveMessage_SingleMessage_Success() throws Exception {
        // Arrange
        TdApi.Message message = createTestMessage(123L, 456L, 0L);
        String expectedJson = "{\"id\":123,\"chatId\":456}";
        
        when(rawMessageRepository.existsByChatIdAndMessageId(456L, 123L)).thenReturn(false);
        when(objectMapper.writeValueAsString(message)).thenReturn(expectedJson);
        when(rawMessageRepository.save(any(RawMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        boolean result = messageStorageService.saveMessage(message);
        
        // Assert
        assertTrue(result);
        
        ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(rawMessageRepository).save(captor.capture());
        
        RawMessage savedMessage = captor.getValue();
        assertEquals(456L, savedMessage.getChatId());
        assertEquals(123L, savedMessage.getMessageId());
        assertNull(savedMessage.getMediaAlbumId());
        assertEquals(expectedJson, savedMessage.getRawJson());
        assertNotNull(savedMessage.getCreateTime());
        assertNotNull(savedMessage.getUpdateTime());
    }
    
    @Test
    void saveMessage_MediaGroupMessage_Success() throws Exception {
        // Arrange
        TdApi.Message message = createTestMessage(123L, 456L, 789L);
        String expectedJson = "{\"id\":123,\"chatId\":456,\"mediaAlbumId\":789}";
        
        when(rawMessageRepository.existsByChatIdAndMediaAlbumId(456L, 789L)).thenReturn(false);
        when(objectMapper.writeValueAsString(message)).thenReturn(expectedJson);
        when(rawMessageRepository.save(any(RawMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        boolean result = messageStorageService.saveMessage(message);
        
        // Assert
        assertTrue(result);
        
        ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(rawMessageRepository).save(captor.capture());
        
        RawMessage savedMessage = captor.getValue();
        assertEquals(456L, savedMessage.getChatId());
        assertEquals(123L, savedMessage.getMessageId());
        assertEquals(789L, savedMessage.getMediaAlbumId());
        assertEquals(expectedJson, savedMessage.getRawJson());
    }
    
    @Test
    void saveMessage_DuplicateSingleMessage_ReturnsFalse() {
        // Arrange
        TdApi.Message message = createTestMessage(123L, 456L, 0L);
        
        when(rawMessageRepository.existsByChatIdAndMessageId(456L, 123L)).thenReturn(true);
        
        // Act
        boolean result = messageStorageService.saveMessage(message);
        
        // Assert
        assertFalse(result);
        verify(rawMessageRepository, never()).save(any());
    }
    
    @Test
    void saveMessage_DuplicateMediaGroupMessage_ReturnsFalse() {
        // Arrange
        TdApi.Message message = createTestMessage(123L, 456L, 789L);
        
        when(rawMessageRepository.existsByChatIdAndMediaAlbumId(456L, 789L)).thenReturn(true);
        
        // Act
        boolean result = messageStorageService.saveMessage(message);
        
        // Assert
        assertFalse(result);
        verify(rawMessageRepository, never()).save(any());
    }
    
    @Test
    void saveMessage_JsonSerializationFails_ReturnsFalse() throws Exception {
        // Arrange
        TdApi.Message message = createTestMessage(123L, 456L, 0L);
        
        when(rawMessageRepository.existsByChatIdAndMessageId(456L, 123L)).thenReturn(false);
        when(objectMapper.writeValueAsString(message)).thenThrow(new RuntimeException("Serialization error"));
        
        // Act
        boolean result = messageStorageService.saveMessage(message);
        
        // Assert
        assertFalse(result);
        verify(rawMessageRepository, never()).save(any());
    }
    
    @Test
    void saveMessage_DatabaseSaveFails_ReturnsFalse() throws Exception {
        // Arrange
        TdApi.Message message = createTestMessage(123L, 456L, 0L);
        String expectedJson = "{\"id\":123,\"chatId\":456}";
        
        when(rawMessageRepository.existsByChatIdAndMessageId(456L, 123L)).thenReturn(false);
        when(objectMapper.writeValueAsString(message)).thenReturn(expectedJson);
        when(rawMessageRepository.save(any(RawMessage.class))).thenThrow(new RuntimeException("Database error"));
        
        // Act
        boolean result = messageStorageService.saveMessage(message);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void deserializeFromJson_Success() throws Exception {
        // Arrange
        String json = "{\"id\":123,\"chatId\":456}";
        TdApi.Message expectedMessage = createTestMessage(123L, 456L, 0L);
        
        when(objectMapper.readValue(json, TdApi.Message.class)).thenReturn(expectedMessage);
        
        // Act
        TdApi.Message result = messageStorageService.deserializeFromJson(json);
        
        // Assert
        assertNotNull(result);
        assertEquals(123L, result.id);
        assertEquals(456L, result.chatId);
    }
    
    @Test
    void getRawMessage_Success() throws Exception {
        // Arrange
        String json = "{\"id\":123,\"chatId\":456}";
        TdApi.Message expectedMessage = createTestMessage(123L, 456L, 0L);
        
        RawMessage rawMessage = new RawMessage();
        rawMessage.setRawJson(json);
        
        when(rawMessageRepository.findByChatIdAndMessageId(456L, 123L)).thenReturn(Optional.of(rawMessage));
        when(objectMapper.readValue(json, TdApi.Message.class)).thenReturn(expectedMessage);
        
        // Act
        TdApi.Message result = messageStorageService.getRawMessage(456L, 123L);
        
        // Assert
        assertNotNull(result);
        assertEquals(123L, result.id);
        assertEquals(456L, result.chatId);
    }
    
    @Test
    void getRawMessage_NotFound_ReturnsNull() {
        // Arrange
        when(rawMessageRepository.findByChatIdAndMessageId(456L, 123L)).thenReturn(Optional.empty());
        
        // Act
        TdApi.Message result = messageStorageService.getRawMessage(456L, 123L);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void getRawMessage_DeserializationFails_ReturnsNull() throws Exception {
        // Arrange
        String json = "{\"id\":123,\"chatId\":456}";
        
        RawMessage rawMessage = new RawMessage();
        rawMessage.setRawJson(json);
        
        when(rawMessageRepository.findByChatIdAndMessageId(456L, 123L)).thenReturn(Optional.of(rawMessage));
        when(objectMapper.readValue(json, TdApi.Message.class)).thenThrow(new RuntimeException("Deserialization error"));
        
        // Act
        TdApi.Message result = messageStorageService.getRawMessage(456L, 123L);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void saveMessage_WithDate_PreservesDate() throws Exception {
        // Arrange
        TdApi.Message message = createTestMessage(123L, 456L, 0L);
        message.date = 1234567890;
        String expectedJson = "{\"id\":123,\"chatId\":456,\"date\":1234567890}";
        
        when(rawMessageRepository.existsByChatIdAndMessageId(456L, 123L)).thenReturn(false);
        when(objectMapper.writeValueAsString(message)).thenReturn(expectedJson);
        when(rawMessageRepository.save(any(RawMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        boolean result = messageStorageService.saveMessage(message);
        
        // Assert
        assertTrue(result);
        
        ArgumentCaptor<RawMessage> captor = ArgumentCaptor.forClass(RawMessage.class);
        verify(rawMessageRepository).save(captor.capture());
        
        RawMessage savedMessage = captor.getValue();
        assertEquals(1234567890, savedMessage.getDate());
    }
    
    // Helper method to create test messages
    private TdApi.Message createTestMessage(long messageId, long chatId, long mediaAlbumId) {
        TdApi.Message message = new TdApi.Message();
        message.id = messageId;
        message.chatId = chatId;
        message.mediaAlbumId = mediaAlbumId;
        message.date = 1234567890;
        message.content = new TdApi.MessageText(
            new TdApi.FormattedText("Test message", new TdApi.TextEntity[0]),
            null,
            null
        );
        return message;
    }
}
