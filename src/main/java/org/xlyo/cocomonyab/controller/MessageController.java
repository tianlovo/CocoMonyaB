package org.xlyo.cocomonyab.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.MessageQueryDTO;
import org.xlyo.cocomonyab.domain.vo.MessageVO;
import org.xlyo.cocomonyab.service.MessageService;

import java.util.List;

/**
 * 消息查询REST控制器
 * 提供Telegram消息的查询、分页等功能
 */
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 根据MongoDB ID查询单条消息
     * GET /api/message/{id}
     *
     * @param id MongoDB文档ID
     * @return 消息视图对象
     */
    @GetMapping("/{id}")
    public ApiResponse<MessageVO> getById(@PathVariable String id) {
        MessageVO vo = messageService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 根据ChatId和MessageId查询消息
     * GET /api/message/by-tg-id
     *
     * @param chatId Telegram频道ID
     * @param messageId Telegram消息ID
     * @return 消息视图对象
     */
    @GetMapping("/by-tg-id")
    public ApiResponse<MessageVO> getByTgId(
            @RequestParam Long chatId,
            @RequestParam Long messageId) {
        MessageVO vo = messageService.getByTgId(chatId, messageId);
        return ApiResponse.success(vo);
    }

    /**
     * 分页查询消息列表
     * GET /api/message/page
     *
     * @param current 当前页码（默认1）
     * @param size 每页大小（默认10）
     * @param query 查询过滤条件
     * @return 分页响应包含消息视图对象列表和分页元数据
     */
    @GetMapping("/page")
    public PageResponse<MessageVO> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @Valid MessageQueryDTO query) {
        Page<MessageVO> messagePage = messageService.page(current, size, query);
        return PageResponse.success(
            messagePage.getContent(),
            current,
            size,
            messagePage.getTotalElements()
        );
    }

    /**
     * 查询媒体组消息
     * GET /api/message/media-album
     *
     * @param chatId Telegram频道ID
     * @param mediaAlbumId 媒体组ID
     * @return 媒体组消息列表
     */
    @GetMapping("/media-album")
    public ApiResponse<List<MessageVO>> getMediaAlbum(
            @RequestParam Long chatId,
            @RequestParam Long mediaAlbumId) {
        List<MessageVO> messages = messageService.getMediaAlbum(chatId, mediaAlbumId);
        return ApiResponse.success(messages);
    }
}
