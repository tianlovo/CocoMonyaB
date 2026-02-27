package org.xlyo.cocomonyab.controller.readonly;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.UnreadMessageBufferQueryDTO;
import org.xlyo.cocomonyab.domain.vo.UnreadMessageBufferStatsVO;
import org.xlyo.cocomonyab.domain.vo.UnreadMessageBufferVO;
import org.xlyo.cocomonyab.service.UnreadMessageBufferService;

/**
 * 未读消息缓冲区查询REST控制器
 * 提供未读消息缓冲区的查询、统计等功能
 */
@RestController
@RequestMapping("/api/unread-buffer")
@RequiredArgsConstructor
@Validated
public class UnreadMessageBufferController {

    private final UnreadMessageBufferService unreadMessageBufferService;

    /**
     * 根据ChatId和MessageId查询缓冲记录
     * GET /api/unread-buffer/by-tg-id
     *
     * @param chatId Telegram频道ID
     * @param messageId Telegram消息ID
     * @return 未读消息缓冲区视图对象
     */
    @GetMapping("/by-tg-id")
    public ApiResponse<UnreadMessageBufferVO> getByTgId(
            @RequestParam @NotNull(message = "频道ID不能为空") Long chatId,
            @RequestParam @NotNull(message = "消息ID不能为空") Long messageId) {
        UnreadMessageBufferVO vo = unreadMessageBufferService.getByTgId(chatId, messageId);
        return ApiResponse.success(vo);
    }

    /**
     * 分页查询缓冲记录
     * GET /api/unread-buffer/page
     *
     * @param current 当前页码（默认1）
     * @param size 每页大小（默认10）
     * @param query 查询过滤条件
     * @return 分页响应包含未读消息缓冲区视图对象列表和分页元数据
     */
    @GetMapping("/page")
    public PageResponse<UnreadMessageBufferVO> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Long current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于等于1") Long size,
            @Valid UnreadMessageBufferQueryDTO query) {
        Page<@NonNull UnreadMessageBufferVO> bufferPage = unreadMessageBufferService.page(current, size, query);
        return PageResponse.success(
            bufferPage.getContent(),
            current,
            size,
            bufferPage.getTotalElements()
        );
    }

    /**
     * 查询待处理消息数量
     * GET /api/unread-buffer/pending-count
     *
     * @param chatId 频道ID（可选）
     * @return 待处理消息数量
     */
    @GetMapping("/pending-count")
    public ApiResponse<Long> getPendingCount(@RequestParam(required = false) Long chatId) {
        Long count = unreadMessageBufferService.getPendingCount(chatId);
        return ApiResponse.success(count);
    }

    /**
     * 查询各状态消息统计
     * GET /api/unread-buffer/stats
     *
     * @return 未读消息缓冲区统计信息
     */
    @GetMapping("/stats")
    public ApiResponse<UnreadMessageBufferStatsVO> getStats() {
        UnreadMessageBufferStatsVO stats = unreadMessageBufferService.getStats();
        return ApiResponse.success(stats);
    }
}
