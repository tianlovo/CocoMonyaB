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
import org.xlyo.cocomonyab.domain.dto.UnreadMessageBufferQueryDTO;
import org.xlyo.cocomonyab.domain.entity.UnreadMessageBuffer;
import org.xlyo.cocomonyab.domain.enums.BufferStatus;
import org.xlyo.cocomonyab.domain.vo.UnreadMessageBufferStatsVO;
import org.xlyo.cocomonyab.domain.vo.UnreadMessageBufferVO;
import org.xlyo.cocomonyab.repository.UnreadMessageBufferRepository;

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
 * UnreadMessageBufferService 单元测试
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UnreadMessageBufferService 单元测试")
class UnreadMessageBufferServiceTest {
    
    @Mock
    private UnreadMessageBufferRepository unreadMessageBufferRepository;
    
    @InjectMocks
    private UnreadMessageBufferService unreadMessageBufferService;
    
    private UnreadMessageBuffer testBuffer;
    private Long testChatId;
    private Long testMessageId;
    
    @BeforeEach
    void setUp() {
        testChatId = -1001234567890L;
        testMessageId = 12345L;
        
        testBuffer = UnreadMessageBuffer.builder()
                .id("65f8a1b2c3d4e5f6a7b8c9d0")
                .chatId(testChatId)
                .messageId(testMessageId)
                .fetchTime(LocalDateTime.now())
                .status(BufferStatus.PENDING)
                .rawMessage("{}")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }
    
    @Test
    @DisplayName("根据TG ID查询 - 成功")
    void getByTgId_ShouldReturnBuffer_WhenExists() {
        // Given
        when(unreadMessageBufferRepository.findByChatIdAndMessageId(testChatId, testMessageId))
                .thenReturn(Optional.of(testBuffer));
        
        // When
        UnreadMessageBufferVO result = unreadMessageBufferService.getByTgId(testChatId, testMessageId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testBuffer.getId());
        assertThat(result.getChatId()).isEqualTo(testChatId);
        assertThat(result.getMessageId()).isEqualTo(testMessageId);
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }
    
    @Test
    @DisplayName("根据TG ID查询 - 记录不存在")
    void getByTgId_ShouldThrowException_WhenNotExists() {
        // Given
        when(unreadMessageBufferRepository.findByChatIdAndMessageId(testChatId, testMessageId))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> unreadMessageBufferService.getByTgId(testChatId, testMessageId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未读消息缓冲记录不存在")
                .extracting("code")
                .isEqualTo(ResponseCode.DATA_NOT_FOUND);
    }
    
    @Test
    @DisplayName("分页查询 - 无过滤条件")
    void page_ShouldReturnAllBuffers_WhenNoFilter() {
        // Given
        List<UnreadMessageBuffer> buffers = Arrays.asList(testBuffer);
        Page<UnreadMessageBuffer> bufferPage = new PageImpl<>(buffers, PageRequest.of(0, 20), 1);
        when(unreadMessageBufferRepository.findAll(any(Pageable.class)))
                .thenReturn(bufferPage);
        
        // When
        Page<UnreadMessageBufferVO> result = unreadMessageBufferService.page(1L, 20L, new UnreadMessageBufferQueryDTO());
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(testBuffer.getId());
    }
    
    @Test
    @DisplayName("分页查询 - 按频道ID过滤")
    void page_ShouldReturnFilteredBuffers_WhenChatIdProvided() {
        // Given
        UnreadMessageBufferQueryDTO query = new UnreadMessageBufferQueryDTO();
        query.setChatId(testChatId);
        
        List<UnreadMessageBuffer> buffers = Arrays.asList(testBuffer);
        Page<UnreadMessageBuffer> bufferPage = new PageImpl<>(buffers, PageRequest.of(0, 20), 1);
        when(unreadMessageBufferRepository.findByChatIdOrderByFetchTimeAsc(eq(testChatId), any(Pageable.class)))
                .thenReturn(bufferPage);
        
        // When
        Page<UnreadMessageBufferVO> result = unreadMessageBufferService.page(1L, 20L, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChatId()).isEqualTo(testChatId);
    }
    
    @Test
    @DisplayName("分页查询 - 按状态过滤")
    void page_ShouldReturnFilteredBuffers_WhenStatusProvided() {
        // Given
        UnreadMessageBufferQueryDTO query = new UnreadMessageBufferQueryDTO();
        query.setStatus("PENDING");
        
        List<UnreadMessageBuffer> buffers = Arrays.asList(testBuffer);
        Page<UnreadMessageBuffer> bufferPage = new PageImpl<>(buffers, PageRequest.of(0, 20), 1);
        when(unreadMessageBufferRepository.findByStatusOrderByFetchTimeAsc(eq(BufferStatus.PENDING), any(Pageable.class)))
                .thenReturn(bufferPage);
        
        // When
        Page<UnreadMessageBufferVO> result = unreadMessageBufferService.page(1L, 20L, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("PENDING");
    }
    
    @Test
    @DisplayName("分页查询 - 按频道ID和状态过滤")
    void page_ShouldReturnFilteredBuffers_WhenChatIdAndStatusProvided() {
        // Given
        UnreadMessageBufferQueryDTO query = new UnreadMessageBufferQueryDTO();
        query.setChatId(testChatId);
        query.setStatus("PENDING");
        
        List<UnreadMessageBuffer> buffers = Arrays.asList(testBuffer);
        Page<UnreadMessageBuffer> bufferPage = new PageImpl<>(buffers, PageRequest.of(0, 20), 1);
        when(unreadMessageBufferRepository.findByChatIdAndStatusOrderByFetchTimeAsc(
                eq(testChatId), eq(BufferStatus.PENDING), any(Pageable.class)))
                .thenReturn(bufferPage);
        
        // When
        Page<UnreadMessageBufferVO> result = unreadMessageBufferService.page(1L, 20L, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChatId()).isEqualTo(testChatId);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("PENDING");
    }
    
    @Test
    @DisplayName("查询待处理消息数量 - 无频道ID过滤")
    void getPendingCount_ShouldReturnCount_WhenNoChatIdProvided() {
        // Given
        when(unreadMessageBufferRepository.countByStatus(BufferStatus.PENDING))
                .thenReturn(10L);
        
        // When
        Long result = unreadMessageBufferService.getPendingCount(null);
        
        // Then
        assertThat(result).isEqualTo(10L);
    }
    
    @Test
    @DisplayName("查询待处理消息数量 - 按频道ID过滤")
    void getPendingCount_ShouldReturnCount_WhenChatIdProvided() {
        // Given
        when(unreadMessageBufferRepository.countByChatIdAndStatus(testChatId, BufferStatus.PENDING))
                .thenReturn(5L);
        
        // When
        Long result = unreadMessageBufferService.getPendingCount(testChatId);
        
        // Then
        assertThat(result).isEqualTo(5L);
    }
    
    @Test
    @DisplayName("查询统计信息 - 成功")
    void getStats_ShouldReturnStats() {
        // Given
        when(unreadMessageBufferRepository.countByStatus(BufferStatus.PENDING)).thenReturn(10L);
        when(unreadMessageBufferRepository.countByStatus(BufferStatus.PROCESSED)).thenReturn(50L);
        when(unreadMessageBufferRepository.countByStatus(BufferStatus.FAILED)).thenReturn(5L);
        when(unreadMessageBufferRepository.count()).thenReturn(65L);
        
        // When
        UnreadMessageBufferStatsVO result = unreadMessageBufferService.getStats();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPendingCount()).isEqualTo(10L);
        assertThat(result.getProcessedCount()).isEqualTo(50L);
        assertThat(result.getFailedCount()).isEqualTo(5L);
        assertThat(result.getTotalCount()).isEqualTo(65L);
    }
    
    @Test
    @DisplayName("数据转换 - Entity转VO")
    void convertToVO_ShouldConvertCorrectly() {
        // When
        UnreadMessageBufferVO result = unreadMessageBufferService.convertToVO(testBuffer);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testBuffer.getId());
        assertThat(result.getChatId()).isEqualTo(testBuffer.getChatId());
        assertThat(result.getMessageId()).isEqualTo(testBuffer.getMessageId());
        assertThat(result.getFetchTime()).isEqualTo(testBuffer.getFetchTime());
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getErrorMessage()).isEqualTo(testBuffer.getErrorMessage());
        assertThat(result.getCreateTime()).isEqualTo(testBuffer.getCreateTime());
        assertThat(result.getUpdateTime()).isEqualTo(testBuffer.getUpdateTime());
    }
    
    @Test
    @DisplayName("数据转换 - null Entity")
    void convertToVO_ShouldReturnNull_WhenEntityIsNull() {
        // When
        UnreadMessageBufferVO result = unreadMessageBufferService.convertToVO(null);
        
        // Then
        assertThat(result).isNull();
    }
    
    @Test
    @DisplayName("数据转换 - status为null")
    void convertToVO_ShouldHandleNullStatus() {
        // Given
        testBuffer.setStatus(null);
        
        // When
        UnreadMessageBufferVO result = unreadMessageBufferService.convertToVO(testBuffer);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isNull();
    }
    
    @Test
    @DisplayName("分页查询 - 测试PROCESSED状态")
    void page_ShouldReturnProcessedBuffers_WhenStatusIsProcessed() {
        // Given
        testBuffer.setStatus(BufferStatus.PROCESSED);
        UnreadMessageBufferQueryDTO query = new UnreadMessageBufferQueryDTO();
        query.setStatus("PROCESSED");
        
        List<UnreadMessageBuffer> buffers = Arrays.asList(testBuffer);
        Page<UnreadMessageBuffer> bufferPage = new PageImpl<>(buffers, PageRequest.of(0, 20), 1);
        when(unreadMessageBufferRepository.findByStatusOrderByFetchTimeAsc(eq(BufferStatus.PROCESSED), any(Pageable.class)))
                .thenReturn(bufferPage);
        
        // When
        Page<UnreadMessageBufferVO> result = unreadMessageBufferService.page(1L, 20L, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("PROCESSED");
    }
    
    @Test
    @DisplayName("分页查询 - 测试FAILED状态")
    void page_ShouldReturnFailedBuffers_WhenStatusIsFailed() {
        // Given
        testBuffer.setStatus(BufferStatus.FAILED);
        testBuffer.setErrorMessage("Test error");
        UnreadMessageBufferQueryDTO query = new UnreadMessageBufferQueryDTO();
        query.setStatus("FAILED");
        
        List<UnreadMessageBuffer> buffers = Arrays.asList(testBuffer);
        Page<UnreadMessageBuffer> bufferPage = new PageImpl<>(buffers, PageRequest.of(0, 20), 1);
        when(unreadMessageBufferRepository.findByStatusOrderByFetchTimeAsc(eq(BufferStatus.FAILED), any(Pageable.class)))
                .thenReturn(bufferPage);
        
        // When
        Page<UnreadMessageBufferVO> result = unreadMessageBufferService.page(1L, 20L, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("FAILED");
        assertThat(result.getContent().get(0).getErrorMessage()).isEqualTo("Test error");
    }
}
