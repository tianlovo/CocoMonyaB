package org.xlyo.cocomonyab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.domain.dto.MessageQueryDTO;
import org.xlyo.cocomonyab.domain.entity.RawMessage;
import org.xlyo.cocomonyab.domain.vo.MessageVO;
import org.xlyo.cocomonyab.repository.RawMessageRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息查询服务
 * 提供消息的查询、分页等功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    
    private final RawMessageRepository rawMessageRepository;
    
    /**
     * 根据MongoDB ID查询单条消息
     * 
     * @param id MongoDB文档ID
     * @return 消息视图对象
     * @throws BusinessException 当ID格式无效或消息不存在时
     */
    public MessageVO getById(String id) {
        // 验证MongoDB ID格式
        if (!ObjectId.isValid(id)) {
            log.warn("无效的MongoDB ID格式: {}", id);
            throw new BusinessException(ResponseCode.VALIDATION_ERROR, "无效的MongoDB ID格式: " + id);
        }
        
        // 查询消息
        RawMessage message = rawMessageRepository.findById(id)
            .orElseThrow(() -> {
                log.warn("消息不存在: {}", id);
                return new BusinessException(ResponseCode.DATA_NOT_FOUND, "消息不存在: " + id);
            });
        
        return convertToVO(message);
    }
    
    /**
     * 根据ChatId和MessageId查询消息
     * 
     * @param chatId Telegram频道ID
     * @param messageId Telegram消息ID
     * @return 消息视图对象
     * @throws BusinessException 当消息不存在时
     */
    public MessageVO getByTgId(Long chatId, Long messageId) {
        RawMessage message = rawMessageRepository.findByChatIdAndMessageId(chatId, messageId)
            .orElseThrow(() -> {
                log.warn("消息不存在: chatId={}, messageId={}", chatId, messageId);
                return new BusinessException(ResponseCode.DATA_NOT_FOUND, 
                    String.format("消息不存在: chatId=%d, messageId=%d", chatId, messageId));
            });
        
        return convertToVO(message);
    }
    
    /**
     * 分页查询消息列表
     * 
     * @param current 当前页码（从1开始）
     * @param size 每页大小
     * @param query 查询条件
     * @return 消息列表
     * @throws BusinessException 当分页参数无效时
     */
    public Page<MessageVO> page(Long current, Long size, MessageQueryDTO query) {
        // 验证分页参数
        if (current < 1) {
            throw new BusinessException(ResponseCode.VALIDATION_ERROR, "页码必须大于等于1");
        }
        if (size < 1) {
            throw new BusinessException(ResponseCode.VALIDATION_ERROR, "每页大小必须大于等于1");
        }
        if (size > 100) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "每页大小不能超过100");
        }
        
        // 创建分页对象（Spring Data的页码从0开始）
        Pageable pageable = PageRequest.of(current.intValue() - 1, size.intValue());
        
        // 应用过滤条件查询
        Page<RawMessage> messagePage = applyFilters(query, pageable);
        
        // 转换为VO
        return messagePage.map(this::convertToVO);
    }
    
    /**
     * 查询媒体组消息
     * 
     * @param chatId Telegram频道ID
     * @param mediaAlbumId 媒体组ID
     * @return 媒体组消息列表（按messageId升序）
     */
    public List<MessageVO> getMediaAlbum(Long chatId, Long mediaAlbumId) {
        List<RawMessage> messages = rawMessageRepository.findAllByChatIdAndMediaAlbumId(chatId, mediaAlbumId);
        
        // 按messageId升序排序并转换为VO
        return messages.stream()
            .sorted((m1, m2) -> Long.compare(m1.getMessageId(), m2.getMessageId()))
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 根据查询条件应用过滤器
     * 
     * @param query 查询条件
     * @param pageable 分页参数
     * @return 分页结果
     */
    private Page<RawMessage> applyFilters(MessageQueryDTO query, Pageable pageable) {
        Long chatId = query.getChatId();
        Integer startDate = query.getStartDate();
        Integer endDate = query.getEndDate();
        
        // 根据不同的查询条件组合选择合适的Repository方法
        if (chatId != null && startDate != null && endDate != null) {
            return rawMessageRepository.findByChatIdAndDateBetweenOrderByDateDesc(
                chatId, startDate, endDate, pageable);
        } else if (chatId != null) {
            return rawMessageRepository.findByChatIdOrderByDateDesc(chatId, pageable);
        } else if (startDate != null && endDate != null) {
            return rawMessageRepository.findByDateBetweenOrderByDateDesc(
                startDate, endDate, pageable);
        } else {
            // 无过滤条件，查询所有消息（按日期降序）
            return rawMessageRepository.findAll(pageable);
        }
    }
    
    /**
     * 将实体转换为视图对象
     * 
     * @param entity 原始消息实体
     * @return 消息视图对象
     */
    private MessageVO convertToVO(RawMessage entity) {
        MessageVO vo = new MessageVO();
        vo.setId(entity.getId());
        vo.setChatId(entity.getChatId());
        vo.setMessageId(entity.getMessageId());
        vo.setMediaAlbumId(entity.getMediaAlbumId());
        vo.setDate(entity.getDate());
        vo.setRawJson(entity.getRawJson());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
