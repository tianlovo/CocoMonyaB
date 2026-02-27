package org.xlyo.cocomonyab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.domain.dto.ChannelMessageQueryDTO;
import org.xlyo.cocomonyab.domain.entity.ChannelMessage;
import org.xlyo.cocomonyab.domain.vo.ChannelMessageVO;
import org.xlyo.cocomonyab.repository.ChannelMessageRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * ChannelMessageService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("频道消息服务测试")
class ChannelMessageServiceTest {
    
    @Mock
    private ChannelMessageRepository channelMessageRepository;
    
    @InjectMocks
    private ChannelMessageService channelMessageService;
    
    private ChannelMessage testMessage;
    
    @BeforeEach
    void setUp() {
        testMessage = new ChannelMessage();
        testMessage.setId("65f8a1b2c3d4e5f6a7b8c9d0");
        testMessage.setMessageId(123456L);
        testMessage.setChatId(-1001234567890L);
        testMessage.setChannelUsername("test_channel");
        testMessage.setChannelTitle("Test Channel");
        testMessage.setDate(1700000000);
        testMessage.setContentType("TEXT");
        testMessage.setTextContent("Test message content");
        testMessage.setStatus(ChannelMessage.MessageStatus.PENDING);
        testMessage.setCreateTime(LocalDateTime.now());
        testMessage.setUpdateTime(LocalDateTime.now());
    }
    
    @Test
    @DisplayName("根据ID查询消息 - 成功")
    void getById_ShouldReturnMessage_WhenIdExists() {
        // Given
        String id = "65f8a1b2c3d4e5f6a7b8c9d0";
        when(channelMessageRepository.findById(id)).thenReturn(Optional.of(testMessage));
        
        // When
        ChannelMessageVO result = channelMessageService.getById(id);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getMessageId()).isEqualTo(123456L);
        assertThat(result.getChatId()).isEqualTo(-1001234567890L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }
    
    @Test
    @DisplayName("根据ID查询消息 - 消息不存在")
    void getById_ShouldThrowException_WhenIdNotExists() {
        // Given
        String id = "65f8a1b2c3d4e5f6a7b8c9d0";
        when(channelMessageRepository.findById(id)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> channelMessageService.getById(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("频道消息不存在")
                .extracting("code")
                .isEqualTo(ResponseCode.DATA_NOT_FOUND.getCode());
    }
    
    @Test
    @DisplayName("根据TG ID查询消息 - 成功")
    void getByTgId_ShouldReturnMessage_WhenExists() {
        // Given
        Long chatId = -1001234567890L;
        Long messageId = 123456L;
        when(channelMessageRepository.findByChatIdAndMessageId(chatId, messageId))
                .thenReturn(Optional.of(testMessage));
        
        // When
        ChannelMessageVO result = channelMessageService.getByTgId(chatId, messageId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getChatId()).isEqualTo(chatId);
        assertThat(result.getMessageId()).isEqualTo(messageId);
    }
    
    @Test
    @DisplayName("根据TG ID查询消息 - 消息不存在")
    void getByTgId_ShouldThrowException_WhenNotExists() {
        // Given
        Long chatId = -1001234567890L;
        Long messageId = 123456L;
        when(channelMessageRepository.findByChatIdAndMessageId(chatId, messageId))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> channelMessageService.getByTgId(chatId, messageId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("频道消息不存在")
                .extracting("code")
                .isEqualTo(ResponseCode.DATA_NOT_FOUND.getCode());
    }
    
    @Test
    @DisplayName("分页查询 - 无过滤条件")
    void page_ShouldReturnAllMessages_WhenNoFilter() {
        // Given
        Long current = 1L;
        Long size = 10L;
        ChannelMessageQueryDTO query = new ChannelMessageQueryDTO();
        
        List<ChannelMessage> messages = Arrays.asList(testMessage);
        Page<ChannelMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 10), 1);
        
        when(channelMessageRepository.findAll(any(Pageable.class))).thenReturn(messagePage);
        
        // When
        Page<ChannelMessageVO> result = channelMessageService.page(current, size, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getNumber()).isEqualTo(0); // Spring Data页码从0开始
    }
    
    @Test
    @DisplayName("分页查询 - 按频道ID过滤")
    void page_ShouldReturnFilteredMessages_WhenChatIdProvided() {
        // Given
        Long current = 1L;
        Long size = 10L;
        ChannelMessageQueryDTO query = new ChannelMessageQueryDTO();
        query.setChatId(-1001234567890L);
        
        List<ChannelMessage> messages = Arrays.asList(testMessage);
        Page<ChannelMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 10), 1);
        
        when(channelMessageRepository.findByChatIdOrderByDateDesc(eq(-1001234567890L), any(Pageable.class)))
                .thenReturn(messagePage);
        
        // When
        Page<ChannelMessageVO> result = channelMessageService.page(current, size, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChatId()).isEqualTo(-1001234567890L);
    }
    
    @Test
    @DisplayName("分页查询 - 按状态过滤")
    void page_ShouldReturnFilteredMessages_WhenStatusProvided() {
        // Given
        Long current = 1L;
        Long size = 10L;
        ChannelMessageQueryDTO query = new ChannelMessageQueryDTO();
        query.setStatus("PENDING");
        
        List<ChannelMessage> messages = Arrays.asList(testMessage);
        Page<ChannelMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 10), 1);
        
        when(channelMessageRepository.findByStatusOrderByCreateTimeDesc(
                eq(ChannelMessage.MessageStatus.PENDING), any(Pageable.class)))
                .thenReturn(messagePage);
        
        // When
        Page<ChannelMessageVO> result = channelMessageService.page(current, size, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("PENDING");
    }
    
    @Test
    @DisplayName("分页查询 - 按频道ID和状态过滤")
    void page_ShouldReturnFilteredMessages_WhenChatIdAndStatusProvided() {
        // Given
        Long current = 1L;
        Long size = 10L;
        ChannelMessageQueryDTO query = new ChannelMessageQueryDTO();
        query.setChatId(-1001234567890L);
        query.setStatus("PENDING");
        
        List<ChannelMessage> messages = Arrays.asList(testMessage);
        Page<ChannelMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 10), 1);
        
        when(channelMessageRepository.findByChatIdAndStatusOrderByDateDesc(
                eq(-1001234567890L), eq(ChannelMessage.MessageStatus.PENDING), any(Pageable.class)))
                .thenReturn(messagePage);
        
        // When
        Page<ChannelMessageVO> result = channelMessageService.page(current, size, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }
    
    @Test
    @DisplayName("分页查询 - 按日期范围过滤")
    void page_ShouldReturnFilteredMessages_WhenDateRangeProvided() {
        // Given
        Long current = 1L;
        Long size = 10L;
        ChannelMessageQueryDTO query = new ChannelMessageQueryDTO();
        query.setChatId(-1001234567890L);
        query.setStartDate(1600000000);
        query.setEndDate(1700000000);
        
        List<ChannelMessage> messages = Arrays.asList(testMessage);
        Page<ChannelMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 10), 1);
        
        when(channelMessageRepository.findByChatIdAndDateBetweenOrderByDateDesc(
                eq(-1001234567890L), eq(1600000000), eq(1700000000), any(Pageable.class)))
                .thenReturn(messagePage);
        
        // When
        Page<ChannelMessageVO> result = channelMessageService.page(current, size, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }
    
    @Test
    @DisplayName("查询媒体组消息 - 成功")
    void getMediaAlbum_ShouldReturnMessages_WhenMediaAlbumExists() {
        // Given
        Long chatId = -1001234567890L;
        Long mediaAlbumId = 999L;
        
        ChannelMessage message1 = new ChannelMessage();
        message1.setId("id1");
        message1.setChatId(chatId);
        message1.setMessageId(123456L);
        message1.setMediaAlbumId(mediaAlbumId);
        
        ChannelMessage message2 = new ChannelMessage();
        message2.setId("id2");
        message2.setChatId(chatId);
        message2.setMessageId(123457L);
        message2.setMediaAlbumId(mediaAlbumId);
        
        List<ChannelMessage> messages = Arrays.asList(message1, message2);
        when(channelMessageRepository.findAllByChatIdAndMediaAlbumId(chatId, mediaAlbumId))
                .thenReturn(messages);
        
        // When
        List<ChannelMessageVO> result = channelMessageService.getMediaAlbum(chatId, mediaAlbumId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMediaAlbumId()).isEqualTo(mediaAlbumId);
        assertThat(result.get(1).getMediaAlbumId()).isEqualTo(mediaAlbumId);
    }
    
    @Test
    @DisplayName("查询媒体组消息 - 媒体组不存在")
    void getMediaAlbum_ShouldThrowException_WhenMediaAlbumNotExists() {
        // Given
        Long chatId = -1001234567890L;
        Long mediaAlbumId = 999L;
        when(channelMessageRepository.findAllByChatIdAndMediaAlbumId(chatId, mediaAlbumId))
                .thenReturn(Collections.emptyList());
        
        // When & Then
        assertThatThrownBy(() -> channelMessageService.getMediaAlbum(chatId, mediaAlbumId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("媒体组不存在")
                .extracting("code")
                .isEqualTo(ResponseCode.DATA_NOT_FOUND.getCode());
    }
    
    @Test
    @DisplayName("Entity转VO - 完整数据")
    void convertToVO_ShouldConvertAllFields_WhenEntityHasCompleteData() {
        // Given
        ChannelMessage.MediaFile mediaFile = new ChannelMessage.MediaFile();
        mediaFile.setFileId("file123");
        mediaFile.setFileType("PHOTO");
        mediaFile.setFileSize(1024L);
        testMessage.setMediaFiles(Arrays.asList(mediaFile));
        
        ChannelMessage.WebPageInfo webPage = new ChannelMessage.WebPageInfo();
        webPage.setUrl("https://example.com");
        webPage.setTitle("Example");
        testMessage.setWebPage(webPage);
        
        // When
        ChannelMessageVO result = channelMessageService.convertToVO(testMessage);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testMessage.getId());
        assertThat(result.getMessageId()).isEqualTo(testMessage.getMessageId());
        assertThat(result.getChatId()).isEqualTo(testMessage.getChatId());
        assertThat(result.getMediaFiles()).hasSize(1);
        assertThat(result.getMediaFiles().get(0).getFileId()).isEqualTo("file123");
        assertThat(result.getWebPage()).isNotNull();
        assertThat(result.getWebPage().getUrl()).isEqualTo("https://example.com");
    }
    
    @Test
    @DisplayName("Entity转VO - 空数据")
    void convertToVO_ShouldReturnNull_WhenEntityIsNull() {
        // When
        ChannelMessageVO result = channelMessageService.convertToVO(null);
        
        // Then
        assertThat(result).isNull();
    }
}
