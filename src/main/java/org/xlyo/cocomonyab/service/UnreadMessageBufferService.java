package org.xlyo.cocomonyab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.domain.dto.UnreadMessageBufferQueryDTO;
import org.xlyo.cocomonyab.domain.entity.UnreadMessageBuffer;
import org.xlyo.cocomonyab.domain.enums.BufferStatus;
import org.xlyo.cocomonyab.domain.vo.UnreadMessageBufferStatsVO;
import org.xlyo.cocomonyab.domain.vo.UnreadMessageBufferVO;
import org.xlyo.cocomonyab.repository.UnreadMessageBufferRepository;

/**
 * 未读消息缓冲区服务类
 * 提供未读消息缓冲区的查询、统计和业务逻辑处理
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnreadMessageBufferService {
    
    private final UnreadMessageBufferRepository unreadMessageBufferRepository;
    
    /**
     * 根据chatId和messageId查询缓冲记录
     *
     * @param chatId 频道ID
     * @param messageId 消息ID
     * @return 未读消息缓冲区VO
     * @throws BusinessException 当记录不存在时抛出
     */
    public UnreadMessageBufferVO getByTgId(Long chatId, Long messageId) {
        log.debug("查询未读消息缓冲记录: chatId={}, messageId={}", chatId, messageId);
        
        UnreadMessageBuffer buffer = unreadMessageBufferRepository.findByChatIdAndMessageId(chatId, messageId)
                .orElseThrow(() -> new BusinessException(
                        ResponseCode.DATA_NOT_FOUND,
                        String.format("未读消息缓冲记录不存在: chatId=%d, messageId=%d", chatId, messageId)
                ));
        
        return convertToVO(buffer);
    }
    
    /**
     * 分页查询缓冲记录，支持过滤条件
     *
     * @param current 当前页码（从1开始）
     * @param size 每页大小
     * @param query 查询条件
     * @return 未读消息缓冲区分页结果
     */
    public Page<UnreadMessageBufferVO> page(Long current, Long size, UnreadMessageBufferQueryDTO query) {
        log.debug("分页查询未读消息缓冲区: current={}, size={}, query={}", current, size, query);
        
        // 创建分页参数（Spring Data页码从0开始）
        Pageable pageable = PageRequest.of(current.intValue() - 1, size.intValue());
        
        Page<UnreadMessageBuffer> bufferPage;
        
        // 根据查询条件选择合适的查询方法
        if (query.getChatId() != null && query.getStatus() != null) {
            // 按频道ID和状态查询
            BufferStatus status = BufferStatus.valueOf(query.getStatus());
            bufferPage = unreadMessageBufferRepository.findByChatIdAndStatusOrderByFetchTimeAsc(
                    query.getChatId(), status, pageable);
        } else if (query.getChatId() != null) {
            // 仅按频道ID查询
            bufferPage = unreadMessageBufferRepository.findByChatIdOrderByFetchTimeAsc(
                    query.getChatId(), pageable);
        } else if (query.getStatus() != null) {
            // 仅按状态查询
            BufferStatus status = BufferStatus.valueOf(query.getStatus());
            bufferPage = unreadMessageBufferRepository.findByStatusOrderByFetchTimeAsc(status, pageable);
        } else {
            // 查询所有记录
            bufferPage = unreadMessageBufferRepository.findAll(pageable);
        }
        
        // 转换为VO
        return bufferPage.map(this::convertToVO);
    }
    
    /**
     * 查询待处理消息数量
     *
     * @param chatId 频道ID（可选）
     * @return 待处理消息数量
     */
    public Long getPendingCount(Long chatId) {
        log.debug("查询待处理消息数量: chatId={}", chatId);
        
        if (chatId != null) {
            // 查询特定频道的待处理数量
            return unreadMessageBufferRepository.countByChatIdAndStatus(chatId, BufferStatus.PENDING);
        } else {
            // 查询所有频道的待处理数量
            return unreadMessageBufferRepository.countByStatus(BufferStatus.PENDING);
        }
    }
    
    /**
     * 获取各状态消息统计信息
     *
     * @return 未读消息缓冲区统计VO
     */
    public UnreadMessageBufferStatsVO getStats() {
        log.debug("查询未读消息缓冲区统计信息");
        
        Long pendingCount = unreadMessageBufferRepository.countByStatus(BufferStatus.PENDING);
        Long processedCount = unreadMessageBufferRepository.countByStatus(BufferStatus.PROCESSED);
        Long failedCount = unreadMessageBufferRepository.countByStatus(BufferStatus.FAILED);
        Long totalCount = unreadMessageBufferRepository.count();
        
        return UnreadMessageBufferStatsVO.builder()
                .pendingCount(pendingCount)
                .processedCount(processedCount)
                .failedCount(failedCount)
                .totalCount(totalCount)
                .build();
    }
    
    /**
     * 将Entity转换为VO
     *
     * @param entity 未读消息缓冲区实体
     * @return 未读消息缓冲区VO
     */
    public UnreadMessageBufferVO convertToVO(UnreadMessageBuffer entity) {
        if (entity == null) {
            return null;
        }
        
        UnreadMessageBufferVO vo = new UnreadMessageBufferVO();
        vo.setId(entity.getId());
        vo.setChatId(entity.getChatId());
        vo.setMessageId(entity.getMessageId());
        vo.setFetchTime(entity.getFetchTime());
        vo.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        
        return vo;
    }
}
