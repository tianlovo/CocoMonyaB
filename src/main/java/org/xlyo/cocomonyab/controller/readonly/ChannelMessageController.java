package org.xlyo.cocomonyab.controller.readonly;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.ChannelMessageQueryDTO;
import org.xlyo.cocomonyab.domain.vo.ChannelMessageVO;
import org.xlyo.cocomonyab.service.ChannelMessageService;

import java.util.List;

/**
 * 频道消息查询REST控制器
 * 提供频道消息的查询、分页等功能
 */
@RestController
@RequestMapping("/api/channel-message")
@RequiredArgsConstructor
@Validated
public class ChannelMessageController {

    private final ChannelMessageService channelMessageService;

    /**
     * 根据MongoDB ID查询单条频道消息
     * GET /api/channel-message/{id}
     *
     * @param id MongoDB文档ID
     * @return 频道消息视图对象
     */
    @GetMapping("/{id}")
    public ApiResponse<ChannelMessageVO> getById(@PathVariable String id) {
        ChannelMessageVO vo = channelMessageService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 根据ChatId和MessageId查询频道消息
     * GET /api/channel-message/by-tg-id
     *
     * @param chatId Telegram频道ID
     * @param messageId Telegram消息ID
     * @return 频道消息视图对象
     */
    @GetMapping("/by-tg-id")
    public ApiResponse<ChannelMessageVO> getByTgId(
            @RequestParam @NotNull(message = "频道ID不能为空") Long chatId,
            @RequestParam @NotNull(message = "消息ID不能为空") Long messageId) {
        ChannelMessageVO vo = channelMessageService.getByTgId(chatId, messageId);
        return ApiResponse.success(vo);
    }

    /**
     * 分页查询频道消息列表
     * GET /api/channel-message/page
     *
     * @param current 当前页码（默认1）
     * @param size 每页大小（默认10）
     * @param query 查询过滤条件
     * @return 分页响应包含频道消息视图对象列表和分页元数据
     */
    @GetMapping("/page")
    public PageResponse<ChannelMessageVO> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Long current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于等于1") Long size,
            @Valid ChannelMessageQueryDTO query) {
        Page<ChannelMessageVO> messagePage = channelMessageService.page(current, size, query);
        return PageResponse.success(
            messagePage.getContent(),
            current,
            size,
            messagePage.getTotalElements()
        );
    }

    /**
     * 查询媒体组消息
     * GET /api/channel-message/media-album
     *
     * @param chatId Telegram频道ID
     * @param mediaAlbumId 媒体组ID
     * @return 媒体组消息列表
     */
    @GetMapping("/media-album")
    public ApiResponse<List<ChannelMessageVO>> getMediaAlbum(
            @RequestParam @NotNull(message = "频道ID不能为空") Long chatId,
            @RequestParam @NotNull(message = "媒体组ID不能为空") Long mediaAlbumId) {
        List<ChannelMessageVO> messages = channelMessageService.getMediaAlbum(chatId, mediaAlbumId);
        return ApiResponse.success(messages);
    }
}
