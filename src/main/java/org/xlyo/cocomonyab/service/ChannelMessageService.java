package org.xlyo.cocomonyab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.domain.dto.ChannelMessageQueryDTO;
import org.xlyo.cocomonyab.domain.entity.ChannelMessage;
import org.xlyo.cocomonyab.domain.vo.ChannelMessageVO;
import org.xlyo.cocomonyab.repository.ChannelMessageRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 频道消息服务类
 * 提供频道消息的查询、转换和业务逻辑处理
 * 
 * @author CocoMonya Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelMessageService {
    
    private final ChannelMessageRepository channelMessageRepository;
    
    /**
     * 根据MongoDB ID查询单条频道消息
     *
     * @param id MongoDB文档ID
     * @return 频道消息VO
     * @throws BusinessException 当消息不存在时抛出
     */
    public ChannelMessageVO getById(String id) {
        log.debug("查询频道消息: id={}", id);
        
        ChannelMessage message = channelMessageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ResponseCode.DATA_NOT_FOUND,
                        "频道消息不存在: " + id
                ));
        
        return convertToVO(message);
    }
    
    /**
     * 根据chatId和messageId查询频道消息
     *
     * @param chatId 频道ID
     * @param messageId 消息ID
     * @return 频道消息VO
     * @throws BusinessException 当消息不存在时抛出
     */
    public ChannelMessageVO getByTgId(Long chatId, Long messageId) {
        log.debug("查询频道消息: chatId={}, messageId={}", chatId, messageId);
        
        ChannelMessage message = channelMessageRepository.findByChatIdAndMessageId(chatId, messageId)
                .orElseThrow(() -> new BusinessException(
                        ResponseCode.DATA_NOT_FOUND,
                        String.format("频道消息不存在: chatId=%d, messageId=%d", chatId, messageId)
                ));
        
        return convertToVO(message);
    }
    
    /**
     * 分页查询频道消息列表，支持过滤条件
     *
     * @param current 当前页码（从1开始）
     * @param size 每页大小
     * @param query 查询条件
     * @return 频道消息分页结果
     */
    public Page<ChannelMessageVO> page(Long current, Long size, ChannelMessageQueryDTO query) {
        log.debug("分页查询频道消息: current={}, size={}, query={}", current, size, query);
        
        // 创建分页参数（Spring Data页码从0开始）
        Pageable pageable = PageRequest.of(current.intValue() - 1, size.intValue());
        
        Page<ChannelMessage> messagePage;
        
        // 根据查询条件选择合适的查询方法
        if (query.getChatId() != null && query.getStatus() != null) {
            // 按频道ID和状态查询
            ChannelMessage.MessageStatus status = ChannelMessage.MessageStatus.valueOf(query.getStatus());
            messagePage = channelMessageRepository.findByChatIdAndStatusOrderByDateDesc(
                    query.getChatId(), status, pageable);
        } else if (query.getChatId() != null && query.getStartDate() != null && query.getEndDate() != null) {
            // 按频道ID和日期范围查询
            messagePage = channelMessageRepository.findByChatIdAndDateBetweenOrderByDateDesc(
                    query.getChatId(), query.getStartDate(), query.getEndDate(), pageable);
        } else if (query.getChatId() != null) {
            // 仅按频道ID查询
            messagePage = channelMessageRepository.findByChatIdOrderByDateDesc(query.getChatId(), pageable);
        } else if (query.getStatus() != null) {
            // 仅按状态查询
            ChannelMessage.MessageStatus status = ChannelMessage.MessageStatus.valueOf(query.getStatus());
            messagePage = channelMessageRepository.findByStatusOrderByCreateTimeDesc(status, pageable);
        } else {
            // 查询所有消息
            messagePage = channelMessageRepository.findAll(pageable);
        }
        
        // 转换为VO
        return messagePage.map(this::convertToVO);
    }
    
    /**
     * 查询媒体组消息
     *
     * @param chatId 频道ID
     * @param mediaAlbumId 媒体组ID
     * @return 媒体组消息列表
     * @throws BusinessException 当媒体组不存在时抛出
     */
    public List<ChannelMessageVO> getMediaAlbum(Long chatId, Long mediaAlbumId) {
        log.debug("查询媒体组消息: chatId={}, mediaAlbumId={}", chatId, mediaAlbumId);
        
        List<ChannelMessage> messages = channelMessageRepository.findAllByChatIdAndMediaAlbumId(
                chatId, mediaAlbumId);
        
        if (messages.isEmpty()) {
            throw new BusinessException(
                    ResponseCode.DATA_NOT_FOUND,
                    String.format("媒体组不存在: chatId=%d, mediaAlbumId=%d", chatId, mediaAlbumId)
            );
        }
        
        return messages.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    /**
     * 将Entity转换为VO
     *
     * @param entity 频道消息实体
     * @return 频道消息VO
     */
    public ChannelMessageVO convertToVO(ChannelMessage entity) {
        if (entity == null) {
            return null;
        }
        
        ChannelMessageVO vo = new ChannelMessageVO();
        vo.setId(entity.getId());
        vo.setMessageId(entity.getMessageId());
        vo.setChatId(entity.getChatId());
        vo.setChannelUsername(entity.getChannelUsername());
        vo.setChannelTitle(entity.getChannelTitle());
        vo.setDate(entity.getDate());
        vo.setEditDate(entity.getEditDate());
        vo.setContentType(entity.getContentType());
        vo.setTextContent(entity.getTextContent());
        vo.setMediaAlbumId(entity.getMediaAlbumId());
        vo.setIsMediaGroup(entity.getIsMediaGroup());
        vo.setMediaGroupItemCount(entity.getMediaGroupItemCount());
        vo.setMediaGroupMessageIds(entity.getMediaGroupMessageIds());
        vo.setViews(entity.getViews());
        vo.setForwards(entity.getForwards());
        vo.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        
        // 转换媒体文件列表
        if (entity.getMediaFiles() != null) {
            List<ChannelMessageVO.MediaFileVO> mediaFileVOs = entity.getMediaFiles().stream()
                    .map(this::convertMediaFileToVO)
                    .collect(Collectors.toList());
            vo.setMediaFiles(mediaFileVOs);
        }
        
        // 转换WebPage信息
        if (entity.getWebPage() != null) {
            vo.setWebPage(convertWebPageToVO(entity.getWebPage()));
        }
        
        return vo;
    }
    
    /**
     * 将MediaFile Entity转换为VO
     */
    private ChannelMessageVO.MediaFileVO convertMediaFileToVO(ChannelMessage.MediaFile entity) {
        if (entity == null) {
            return null;
        }
        
        ChannelMessageVO.MediaFileVO vo = new ChannelMessageVO.MediaFileVO();
        vo.setFileId(entity.getFileId());
        vo.setFileType(entity.getFileType());
        vo.setFileSize(entity.getFileSize());
        vo.setMimeType(entity.getMimeType());
        vo.setLocalPath(entity.getLocalPath());
        vo.setDownloaded(entity.getDownloaded());
        return vo;
    }
    
    /**
     * 将WebPageInfo Entity转换为VO
     */
    private ChannelMessageVO.WebPageInfoVO convertWebPageToVO(ChannelMessage.WebPageInfo entity) {
        if (entity == null) {
            return null;
        }
        
        ChannelMessageVO.WebPageInfoVO vo = new ChannelMessageVO.WebPageInfoVO();
        vo.setUrl(entity.getUrl());
        vo.setDisplayUrl(entity.getDisplayUrl());
        vo.setType(entity.getType());
        vo.setSiteName(entity.getSiteName());
        vo.setTitle(entity.getTitle());
        vo.setDescription(entity.getDescription());
        vo.setAuthor(entity.getAuthor());
        vo.setDuration(entity.getDuration());
        vo.setHasInstantView(entity.getHasInstantView());
        vo.setInstantViewVersion(entity.getInstantViewVersion());
        return vo;
    }
}
