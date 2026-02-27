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
import org.xlyo.cocomonyab.domain.dto.ProcessedMessageQueryDTO;
import org.xlyo.cocomonyab.domain.entity.ProcessedMessage;
import org.xlyo.cocomonyab.domain.vo.ProcessedMessageVO;
import org.xlyo.cocomonyab.repository.ProcessedMessageRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * ProcessedMessageService 单元测试
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessedMessageService 单元测试")
class ProcessedMessageServiceTest {
    
    @Mock
    private ProcessedMessageRepository processedMessageRepository;
    
    @InjectMocks
    private ProcessedMessageService processedMessageService;
    
    private ProcessedMessage testMessage;
    private Long testChatId;
    private Long testMessageId;
    
    @BeforeEach
    void setUp() {
        testChatId = -1001234567890L;
        testMessageId = 12345L;
        
        testMessage = ProcessedMessage.builder()
                .id("65f8a1b2c3d4e5f6a7b8c9d0")
                .chatId(testChatId)
                .messageId(testMessageId)
                .messageType("TEXT")
                .isRead(false)
                .isMatched(true)
                .matchedTags(new String[]{"tag1", "tag2"})
                .processTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }
    
    @Test
    @DisplayName("根据TG ID查询 - 成功")
    void getByTgId_ShouldReturnMessage_WhenExists() {
        // Given
        when(processedMessageRepository.findByChatIdAndMessageId(testChatId, testMessageId))
                .thenReturn(Optional.of(testMessage));
        
        // When
        ProcessedMessageVO result = processedMessageService.getByTgId(testChatId, testMessageId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testMessage.getId());
        assertThat(result.getChatId()).isEqualTo(testChatId);
        assertThat(result.getMessageId()).isEqualTo(testMessageId);
        assertThat(result.getMessageType()).isEqualTo("TEXT");
        assertThat(result.getIsRead()).isFalse();
        assertThat(result.getIsMatched()).isTrue();
        assertThat(result.getMatchedTags()).containsExactly("tag1", "tag2");
    }
    
    @Test
    @DisplayName("根据TG ID查询 - 记录不存在")
    void getByTgId_ShouldThrowException_WhenNotExists() {
        // Given
        when(processedMessageRepository.findByChatIdAndMessageId(testChatId, testMessageId))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> processedMessageService.getByTgId(testChatId, testMessageId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已处理消息记录不存在")
                .extracting("code")
                .isEqualTo(ResponseCode.DATA_NOT_FOUND);
    }
    
    @Test
    @DisplayName("分页查询 - 无过滤条件")
    void page_ShouldReturnAllMessages_WhenNoFilter() {
        // Given
        List<ProcessedMessage> messages = Arrays.asList(testMessage);
        Page<ProcessedMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 20), 1);
        when(processedMessageRepository.findAll(any(Pageable.class)))
                .thenReturn(messagePage);
        
        // When
        Page<ProcessedMessageVO> result = processedMessageService.page(1L, 20L, new ProcessedMessageQueryDTO());
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(testMessage.getId());
    }
    
    @Test
    @DisplayName("分页查询 - 按频道ID过滤")
    void page_ShouldReturnFilteredMessages_WhenChatIdProvided() {
        // Given
        ProcessedMessageQueryDTO query = new ProcessedMessageQueryDTO();
        query.setChatId(testChatId);
        
        List<ProcessedMessage> messages = Arrays.asList(testMessage);
        Page<ProcessedMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 20), 1);
        when(processedMessageRepository.findByChatIdOrderByProcessTimeDesc(eq(testChatId), any(Pageable.class)))
                .thenReturn(messagePage);
        
        // When
        Page<ProcessedMessageVO> result = processedMessageService.page(1L, 20L, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChatId()).isEqualTo(testChatId);
    }
    
    @Test
    @DisplayName("分页查询 - 按已读状态过滤")
    void page_ShouldReturnFilteredMessages_WhenIsReadProvided() {
        // Given
        ProcessedMessageQueryDTO query = new ProcessedMessageQueryDTO();
        query.setIsRead(false);
        
        List<ProcessedMessage> messages = Arrays.asList(testMessage);
        Page<ProcessedMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 20), 1);
        when(processedMessageRepository.findByIsReadOrderByProcessTimeDesc(eq(false), any(Pageable.class)))
                .thenReturn(messagePage);
        
        // When
        Page<ProcessedMessageVO> result = processedMessageService.page(1L, 20L, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsRead()).isFalse();
    }
    
    @Test
    @DisplayName("分页查询 - 按匹配状态过滤")
    void page_ShouldReturnFilteredMessages_WhenIsMatchedProvided() {
        // Given
        ProcessedMessageQueryDTO query = new ProcessedMessageQueryDTO();
        query.setIsMatched(true);
        
        List<ProcessedMessage> messages = Arrays.asList(testMessage);
        Page<ProcessedMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 20), 1);
        when(processedMessageRepository.findByIsMatchedOrderByProcessTimeDesc(eq(true), any(Pageable.class)))
                .thenReturn(messagePage);
        
        // When
        Page<ProcessedMessageVO> result = processedMessageService.page(1L, 20L, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsMatched()).isTrue();
    }
    
    @Test
    @DisplayName("分页查询 - 按频道ID和已读状态过滤")
    void page_ShouldReturnFilteredMessages_WhenChatIdAndIsReadProvided() {
        // Given
        ProcessedMessageQueryDTO query = new ProcessedMessageQueryDTO();
        query.setChatId(testChatId);
        query.setIsRead(false);
        
        List<ProcessedMessage> messages = Arrays.asList(testMessage);
        Page<ProcessedMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 20), 1);
        when(processedMessageRepository.findByChatIdAndIsReadOrderByProcessTimeDesc(
                eq(testChatId), eq(false), any(Pageable.class)))
                .thenReturn(messagePage);
        
        // When
        Page<ProcessedMessageVO> result = processedMessageService.page(1L, 20L, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChatId()).isEqualTo(testChatId);
        assertThat(result.getContent().get(0).getIsRead()).isFalse();
    }
    
    @Test
    @DisplayName("分页查询 - 按频道ID和匹配状态过滤")
    void page_ShouldReturnFilteredMessages_WhenChatIdAndIsMatchedProvided() {
        // Given
        ProcessedMessageQueryDTO query = new ProcessedMessageQueryDTO();
        query.setChatId(testChatId);
        query.setIsMatched(true);
        
        List<ProcessedMessage> messages = Arrays.asList(testMessage);
        Page<ProcessedMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 20), 1);
        when(processedMessageRepository.findByChatIdAndIsMatchedOrderByProcessTimeDesc(
                eq(testChatId), eq(true), any(Pageable.class)))
                .thenReturn(messagePage);
        
        // When
        Page<ProcessedMessageVO> result = processedMessageService.page(1L, 20L, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChatId()).isEqualTo(testChatId);
        assertThat(result.getContent().get(0).getIsMatched()).isTrue();
    }
    
    @Test
    @DisplayName("查询未读消息 - 无频道ID过滤")
    void getUnreadMessages_ShouldReturnUnreadMessages_WhenNoChatIdProvided() {
        // Given
        List<ProcessedMessage> messages = Arrays.asList(testMessage);
        Page<ProcessedMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 20), 1);
        when(processedMessageRepository.findByIsReadOrderByProcessTimeDesc(eq(false), any(Pageable.class)))
                .thenReturn(messagePage);
        
        // When
        Page<ProcessedMessageVO> result = processedMessageService.getUnreadMessages(1L, 20L, null);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsRead()).isFalse();
    }
    
    @Test
    @DisplayName("查询未读消息 - 按频道ID过滤")
    void getUnreadMessages_ShouldReturnUnreadMessages_WhenChatIdProvided() {
        // Given
        List<ProcessedMessage> messages = Arrays.asList(testMessage);
        Page<ProcessedMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 20), 1);
        when(processedMessageRepository.findByChatIdAndIsReadOrderByProcessTimeDesc(
                eq(testChatId), eq(false), any(Pageable.class)))
                .thenReturn(messagePage);
        
        // When
        Page<ProcessedMessageVO> result = processedMessageService.getUnreadMessages(1L, 20L, testChatId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChatId()).isEqualTo(testChatId);
        assertThat(result.getContent().get(0).getIsRead()).isFalse();
    }
    
    @Test
    @DisplayName("查询匹配标签的消息 - 无频道ID过滤")
    void getMatchedMessages_ShouldReturnMatchedMessages_WhenNoChatIdProvided() {
        // Given
        List<ProcessedMessage> messages = Arrays.asList(testMessage);
        Page<ProcessedMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 20), 1);
        when(processedMessageRepository.findByIsMatchedOrderByProcessTimeDesc(eq(true), any(Pageable.class)))
                .thenReturn(messagePage);
        
        // When
        Page<ProcessedMessageVO> result = processedMessageService.getMatchedMessages(1L, 20L, null);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsMatched()).isTrue();
    }
    
    @Test
    @DisplayName("查询匹配标签的消息 - 按频道ID过滤")
    void getMatchedMessages_ShouldReturnMatchedMessages_WhenChatIdProvided() {
        // Given
        List<ProcessedMessage> messages = Arrays.asList(testMessage);
        Page<ProcessedMessage> messagePage = new PageImpl<>(messages, PageRequest.of(0, 20), 1);
        when(processedMessageRepository.findByChatIdAndIsMatchedOrderByProcessTimeDesc(
                eq(testChatId), eq(true), any(Pageable.class)))
                .thenReturn(messagePage);
        
        // When
        Page<ProcessedMessageVO> result = processedMessageService.getMatchedMessages(1L, 20L, testChatId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChatId()).isEqualTo(testChatId);
        assertThat(result.getContent().get(0).getIsMatched()).isTrue();
    }
    
    @Test
    @DisplayName("数据转换 - Entity转VO")
    void convertToVO_ShouldConvertCorrectly() {
        // When
        ProcessedMessageVO result = processedMessageService.convertToVO(testMessage);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testMessage.getId());
        assertThat(result.getChatId()).isEqualTo(testMessage.getChatId());
        assertThat(result.getMessageId()).isEqualTo(testMessage.getMessageId());
        assertThat(result.getMessageType()).isEqualTo(testMessage.getMessageType());
        assertThat(result.getIsRead()).isEqualTo(testMessage.getIsRead());
        assertThat(result.getIsMatched()).isEqualTo(testMessage.getIsMatched());
        assertThat(result.getMatchedTags()).containsExactly("tag1", "tag2");
        assertThat(result.getProcessTime()).isEqualTo(testMessage.getProcessTime());
        assertThat(result.getReadTime()).isEqualTo(testMessage.getReadTime());
        assertThat(result.getCreateTime()).isEqualTo(testMessage.getCreateTime());
        assertThat(result.getUpdateTime()).isEqualTo(testMessage.getUpdateTime());
    }
    
    @Test
    @DisplayName("数据转换 - null Entity")
    void convertToVO_ShouldReturnNull_WhenEntityIsNull() {
        // When
        ProcessedMessageVO result = processedMessageService.convertToVO(null);
        
        // Then
        assertThat(result).isNull();
    }
    
    @Test
    @DisplayName("数据转换 - matchedTags为null")
    void convertToVO_ShouldHandleNullMatchedTags() {
        // Given
        testMessage.setMatchedTags(null);
        
        // When
        ProcessedMessageVO result = processedMessageService.convertToVO(testMessage);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMatchedTags()).isEmpty();
    }
}
