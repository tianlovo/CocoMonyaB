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
import org.xlyo.cocomonyab.domain.dto.ForwardQueueQueryDTO;
import org.xlyo.cocomonyab.domain.vo.ForwardQueueStatsVO;
import org.xlyo.cocomonyab.domain.vo.ForwardQueueVO;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardQueueItem;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardStatus;
import org.xlyo.cocomonyab.repository.ForwardQueueRepository;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * ForwardQueueService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("转发队列服务测试")
class ForwardQueueServiceTest {
    
    @Mock
    private ForwardQueueRepository forwardQueueRepository;
    
    @InjectMocks
    private ForwardQueueService forwardQueueService;
    
    private ForwardQueueItem testItem;
    
    @BeforeEach
    void setUp() {
        testItem = ForwardQueueItem.builder()
                .id("65f8a1b2c3d4e5f6a7b8c9d0")
                .sourceChatId(-1001234567890L)
                .sourceMessageId(123456L)
                .matchedTags(Arrays.asList("tag1", "tag2"))
                .status(ForwardStatus.PENDING)
                .createTime(Instant.now())
                .updateTime(Instant.now())
                .retryCount(0)
                .build();
    }
    
    @Test
    @DisplayName("根据ID查询 - 成功")
    void getById_ShouldReturnItem_WhenIdExists() {
        // Given
        String id = "65f8a1b2c3d4e5f6a7b8c9d0";
        when(forwardQueueRepository.findById(id)).thenReturn(Optional.of(testItem));
        
        // When
        ForwardQueueVO result = forwardQueueService.getById(id);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }
    
    @Test
    @DisplayName("根据ID查询 - 记录不存在")
    void getById_ShouldThrowException_WhenIdNotExists() {
        // Given
        String id = "65f8a1b2c3d4e5f6a7b8c9d0";
        when(forwardQueueRepository.findById(id)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> forwardQueueService.getById(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("转发队列记录不存在")
                .extracting("code")
                .isEqualTo(ResponseCode.DATA_NOT_FOUND.getCode());
    }
    
    @Test
    @DisplayName("根据源信息查询 - 成功")
    void getBySource_ShouldReturnItem_WhenExists() {
        // Given
        Long sourceChatId = -1001234567890L;
        Long sourceMessageId = 123456L;
        when(forwardQueueRepository.findBySourceChatIdAndSourceMessageId(sourceChatId, sourceMessageId))
                .thenReturn(Optional.of(testItem));
        
        // When
        ForwardQueueVO result = forwardQueueService.getBySource(sourceChatId, sourceMessageId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSourceChatId()).isEqualTo(sourceChatId);
        assertThat(result.getSourceMessageId()).isEqualTo(sourceMessageId);
    }
    
    @Test
    @DisplayName("分页查询 - 无过滤条件")
    void page_ShouldReturnAllItems_WhenNoFilter() {
        // Given
        Long current = 1L;
        Long size = 10L;
        ForwardQueueQueryDTO query = new ForwardQueueQueryDTO();
        
        List<ForwardQueueItem> items = Arrays.asList(testItem);
        Page<ForwardQueueItem> itemPage = new PageImpl<>(items, PageRequest.of(0, 10), 1);
        
        when(forwardQueueRepository.findAll(any(Pageable.class))).thenReturn(itemPage);
        
        // When
        Page<ForwardQueueVO> result = forwardQueueService.page(current, size, query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("获取统计信息")
    void getStats_ShouldReturnCorrectStats() {
        // Given
        when(forwardQueueRepository.countByStatus(ForwardStatus.PENDING)).thenReturn(10L);
        when(forwardQueueRepository.countByStatus(ForwardStatus.SUCCESS)).thenReturn(20L);
        when(forwardQueueRepository.countByStatus(ForwardStatus.FAILED)).thenReturn(5L);
        when(forwardQueueRepository.count()).thenReturn(35L);
        
        // When
        ForwardQueueStatsVO result = forwardQueueService.getStats();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPendingCount()).isEqualTo(10L);
        assertThat(result.getSuccessCount()).isEqualTo(20L);
        assertThat(result.getFailedCount()).isEqualTo(5L);
        assertThat(result.getTotalCount()).isEqualTo(35L);
    }
    
    @Test
    @DisplayName("Entity转VO - 完整数据")
    void convertToVO_ShouldConvertAllFields() {
        // When
        ForwardQueueVO result = forwardQueueService.convertToVO(testItem);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testItem.getId());
        assertThat(result.getSourceChatId()).isEqualTo(testItem.getSourceChatId());
        assertThat(result.getSourceMessageId()).isEqualTo(testItem.getSourceMessageId());
        assertThat(result.getMatchedTags()).containsExactly("tag1", "tag2");
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }
    
    @Test
    @DisplayName("Entity转VO - 空数据")
    void convertToVO_ShouldReturnNull_WhenEntityIsNull() {
        // When
        ForwardQueueVO result = forwardQueueService.convertToVO(null);
        
        // Then
        assertThat(result).isNull();
    }
}
