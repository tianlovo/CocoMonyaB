package org.xlyo.cocomonyab.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.domain.dto.ForwardQueueQueryDTO;
import org.xlyo.cocomonyab.domain.vo.ForwardQueueStatsVO;
import org.xlyo.cocomonyab.domain.vo.ForwardQueueVO;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardQueueItem;
import org.xlyo.cocomonyab.plugin.tagforward.model.ForwardStatus;
import org.xlyo.cocomonyab.repository.ForwardQueueRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 转发队列服务类
 * 提供转发队列的查询、统计和业务逻辑处理
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ForwardQueueService {
    
    private final ForwardQueueRepository forwardQueueRepository;
    
    /**
     * 根据MongoDB ID查询单条队列记录
     *
     * @param id MongoDB文档ID
     * @return 转发队列VO
     * @throws BusinessException 当记录不存在时抛出
     */
    public ForwardQueueVO getById(String id) {
        log.debug("查询转发队列记录: id={}", id);
        
        ForwardQueueItem item = forwardQueueRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ResponseCode.DATA_NOT_FOUND,
                        "转发队列记录不存在: " + id
                ));
        
        return convertToVO(item);
    }
    
    /**
     * 根据源频道ID和源消息ID查询队列记录
     *
     * @param sourceChatId 源频道ID
     * @param sourceMessageId 源消息ID
     * @return 转发队列VO
     * @throws BusinessException 当记录不存在时抛出
     */
    public ForwardQueueVO getBySource(Long sourceChatId, Long sourceMessageId) {
        log.debug("查询转发队列记录: sourceChatId={}, sourceMessageId={}", sourceChatId, sourceMessageId);
        
        ForwardQueueItem item = forwardQueueRepository.findBySourceChatIdAndSourceMessageId(
                sourceChatId, sourceMessageId)
                .orElseThrow(() -> new BusinessException(
                        ResponseCode.DATA_NOT_FOUND,
                        String.format("转发队列记录不存在: sourceChatId=%d, sourceMessageId=%d", 
                                sourceChatId, sourceMessageId)
                ));
        
        return convertToVO(item);
    }
    
    /**
     * 分页查询队列记录，支持过滤条件
     *
     * @param current 当前页码（从1开始）
     * @param size 每页大小
     * @param query 查询条件
     * @return 转发队列分页结果
     */
    public Page<@NonNull ForwardQueueVO> page(Long current, Long size, ForwardQueueQueryDTO query) {
        log.debug("分页查询转发队列: current={}, size={}, query={}", current, size, query);
        
        // 创建分页参数（Spring Data页码从0开始）
        Pageable pageable = PageRequest.of(current.intValue() - 1, size.intValue());
        
        Page<@NonNull ForwardQueueItem> itemPage;
        
        // 根据查询条件选择合适的查询方法
        if (query.getSourceChatId() != null && query.getStatus() != null) {
            // 按源频道ID和状态查询
            ForwardStatus status = ForwardStatus.valueOf(query.getStatus());
            itemPage = forwardQueueRepository.findBySourceChatIdAndStatusOrderByCreateTimeAsc(
                    query.getSourceChatId(), status, pageable);
        } else if (query.getSourceChatId() != null) {
            // 仅按源频道ID查询
            itemPage = forwardQueueRepository.findBySourceChatIdOrderByCreateTimeAsc(
                    query.getSourceChatId(), pageable);
        } else if (query.getStatus() != null) {
            // 仅按状态查询
            ForwardStatus status = ForwardStatus.valueOf(query.getStatus());
            itemPage = forwardQueueRepository.findByStatusOrderByCreateTimeAsc(status, pageable);
        } else {
            // 查询所有记录
            itemPage = forwardQueueRepository.findAll(pageable);
        }
        
        // 转换为VO
        return itemPage.map(this::convertToVO);
    }
    
    /**
     * 获取转发统计信息
     *
     * @return 转发统计VO
     */
    public ForwardQueueStatsVO getStats() {
        log.debug("查询转发队列统计信息");
        
        Long pendingCount = forwardQueueRepository.countByStatus(ForwardStatus.PENDING);
        Long successCount = forwardQueueRepository.countByStatus(ForwardStatus.SUCCESS);
        Long failedCount = forwardQueueRepository.countByStatus(ForwardStatus.FAILED);
        Long totalCount = forwardQueueRepository.count();
        
        ForwardQueueStatsVO stats = new ForwardQueueStatsVO();
        stats.setPendingCount(pendingCount);
        stats.setSuccessCount(successCount);
        stats.setFailedCount(failedCount);
        stats.setTotalCount(totalCount);
        
        return stats;
    }
    
    /**
     * 将Entity转换为VO
     *
     * @param entity 转发队列实体
     * @return 转发队列VO
     */
    public ForwardQueueVO convertToVO(ForwardQueueItem entity) {
        if (entity == null) {
            return null;
        }
        
        ForwardQueueVO vo = new ForwardQueueVO();
        vo.setId(entity.getId());
        vo.setSourceChatId(entity.getSourceChatId());
        vo.setSourceMessageId(entity.getSourceMessageId());
        vo.setMediaGroupMessageIds(entity.getMediaGroupMessageIds());
        vo.setMatchedTags(entity.getMatchedTags());
        vo.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        vo.setRetryCount(entity.getRetryCount());
        vo.setErrorMessage(entity.getErrorMessage());
        
        // 转换Instant到LocalDateTime
        if (entity.getCreateTime() != null) {
            vo.setCreateTime(LocalDateTime.ofInstant(entity.getCreateTime(), ZoneId.systemDefault()));
        }
        if (entity.getUpdateTime() != null) {
            vo.setUpdateTime(LocalDateTime.ofInstant(entity.getUpdateTime(), ZoneId.systemDefault()));
        }
        if (entity.getForwardTime() != null) {
            vo.setForwardTime(LocalDateTime.ofInstant(entity.getForwardTime(), ZoneId.systemDefault()));
        }
        
        return vo;
    }
}
