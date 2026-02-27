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
import org.xlyo.cocomonyab.domain.dto.ProcessedMessageQueryDTO;
import org.xlyo.cocomonyab.domain.entity.ProcessedMessage;
import org.xlyo.cocomonyab.domain.vo.ProcessedMessageVO;
import org.xlyo.cocomonyab.repository.ProcessedMessageRepository;

import java.util.Arrays;
import java.util.Collections;

/**
 * 已处理消息服务类
 * 提供已处理消息的查询、转换和业务逻辑处理
 * 
 * @author tianluoqaq
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessedMessageService {
    
    private final ProcessedMessageRepository processedMessageRepository;
    
    /**
     * 根据chatId和messageId查询处理记录
     *
     * @param chatId 频道ID
     * @param messageId 消息ID
     * @return 已处理消息VO
     * @throws BusinessException 当记录不存在时抛出
     */
    public ProcessedMessageVO getByTgId(Long chatId, Long messageId) {
        log.debug("查询已处理消息记录: chatId={}, messageId={}", chatId, messageId);
        
        ProcessedMessage message = processedMessageRepository.findByChatIdAndMessageId(chatId, messageId)
                .orElseThrow(() -> new BusinessException(
                        ResponseCode.DATA_NOT_FOUND,
                        String.format("已处理消息记录不存在: chatId=%d, messageId=%d", chatId, messageId)
                ));
        
        return convertToVO(message);
    }
    
    /**
     * 分页查询处理记录，支持过滤条件
     *
     * @param current 当前页码（从1开始）
     * @param size 每页大小
     * @param query 查询条件
     * @return 已处理消息分页结果
     */
    public Page<@NonNull ProcessedMessageVO> page(Long current, Long size, ProcessedMessageQueryDTO query) {
        log.debug("分页查询已处理消息: current={}, size={}, query={}", current, size, query);
        
        // 创建分页参数（Spring Data页码从0开始）
        Pageable pageable = PageRequest.of(current.intValue() - 1, size.intValue());
        
        Page<@NonNull ProcessedMessage> messagePage;
        
        // 根据查询条件选择合适的查询方法
        if (query.getChatId() != null && query.getIsRead() != null) {
            // 按频道ID和已读状态查询
            messagePage = processedMessageRepository.findByChatIdAndIsReadOrderByProcessTimeDesc(
                    query.getChatId(), query.getIsRead(), pageable);
        } else if (query.getChatId() != null && query.getIsMatched() != null) {
            // 按频道ID和匹配状态查询
            messagePage = processedMessageRepository.findByChatIdAndIsMatchedOrderByProcessTimeDesc(
                    query.getChatId(), query.getIsMatched(), pageable);
        } else if (query.getChatId() != null) {
            // 仅按频道ID查询
            messagePage = processedMessageRepository.findByChatIdOrderByProcessTimeDesc(
                    query.getChatId(), pageable);
        } else if (query.getIsRead() != null) {
            // 仅按已读状态查询
            messagePage = processedMessageRepository.findByIsReadOrderByProcessTimeDesc(
                    query.getIsRead(), pageable);
        } else if (query.getIsMatched() != null) {
            // 仅按匹配状态查询
            messagePage = processedMessageRepository.findByIsMatchedOrderByProcessTimeDesc(
                    query.getIsMatched(), pageable);
        } else {
            // 查询所有记录
            messagePage = processedMessageRepository.findAll(pageable);
        }
        
        // 转换为VO
        return messagePage.map(this::convertToVO);
    }
    
    /**
     * 查询未读消息列表
     *
     * @param current 当前页码（从1开始）
     * @param size 每页大小
     * @param chatId 频道ID（可选）
     * @return 未读消息分页结果
     */
    public Page<@NonNull ProcessedMessageVO> getUnreadMessages(Long current, Long size, Long chatId) {
        log.debug("查询未读消息列表: current={}, size={}, chatId={}", current, size, chatId);
        
        // 创建分页参数（Spring Data页码从0开始）
        Pageable pageable = PageRequest.of(current.intValue() - 1, size.intValue());
        
        Page<@NonNull ProcessedMessage> messagePage;
        
        if (chatId != null) {
            // 按频道ID和未读状态查询
            messagePage = processedMessageRepository.findByChatIdAndIsReadOrderByProcessTimeDesc(
                    chatId, false, pageable);
        } else {
            // 仅按未读状态查询
            messagePage = processedMessageRepository.findByIsReadOrderByProcessTimeDesc(
                    false, pageable);
        }
        
        // 转换为VO
        return messagePage.map(this::convertToVO);
    }
    
    /**
     * 查询匹配标签的消息列表
     *
     * @param current 当前页码（从1开始）
     * @param size 每页大小
     * @param chatId 频道ID（可选）
     * @return 匹配标签的消息分页结果
     */
    public Page<@NonNull ProcessedMessageVO> getMatchedMessages(Long current, Long size, Long chatId) {
        log.debug("查询匹配标签的消息列表: current={}, size={}, chatId={}", current, size, chatId);
        
        // 创建分页参数（Spring Data页码从0开始）
        Pageable pageable = PageRequest.of(current.intValue() - 1, size.intValue());
        
        Page<@NonNull ProcessedMessage> messagePage;
        
        if (chatId != null) {
            // 按频道ID和匹配状态查询
            messagePage = processedMessageRepository.findByChatIdAndIsMatchedOrderByProcessTimeDesc(
                    chatId, true, pageable);
        } else {
            // 仅按匹配状态查询
            messagePage = processedMessageRepository.findByIsMatchedOrderByProcessTimeDesc(
                    true, pageable);
        }
        
        // 转换为VO
        return messagePage.map(this::convertToVO);
    }
    
    /**
     * 将Entity转换为VO
     *
     * @param entity 已处理消息实体
     * @return 已处理消息VO
     */
    public ProcessedMessageVO convertToVO(ProcessedMessage entity) {
        if (entity == null) {
            return null;
        }
        
        ProcessedMessageVO vo = new ProcessedMessageVO();
        vo.setId(entity.getId());
        vo.setChatId(entity.getChatId());
        vo.setMessageId(entity.getMessageId());
        vo.setMessageType(entity.getMessageType());
        vo.setIsRead(entity.getIsRead());
        vo.setIsMatched(entity.getIsMatched());
        vo.setProcessTime(entity.getProcessTime());
        vo.setReadTime(entity.getReadTime());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        
        // 转换matchedTags数组为List
        if (entity.getMatchedTags() != null) {
            vo.setMatchedTags(Arrays.asList(entity.getMatchedTags()));
        } else {
            vo.setMatchedTags(Collections.emptyList());
        }
        
        return vo;
    }
}
