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
import org.xlyo.cocomonyab.domain.dto.ProcessedMessageQueryDTO;
import org.xlyo.cocomonyab.domain.vo.ProcessedMessageVO;
import org.xlyo.cocomonyab.service.ProcessedMessageService;

/**
 * 已处理消息查询REST控制器
 * 提供已处理消息的查询、分页等功能
 */
@RestController
@RequestMapping("/api/processed-message")
@RequiredArgsConstructor
@Validated
public class ProcessedMessageController {

    private final ProcessedMessageService processedMessageService;

    /**
     * 根据ChatId和MessageId查询处理记录
     * GET /api/processed-message/by-tg-id
     *
     * @param chatId Telegram频道ID
     * @param messageId Telegram消息ID
     * @return 已处理消息视图对象
     */
    @GetMapping("/by-tg-id")
    public ApiResponse<ProcessedMessageVO> getByTgId(
            @RequestParam @NotNull(message = "频道ID不能为空") Long chatId,
            @RequestParam @NotNull(message = "消息ID不能为空") Long messageId) {
        ProcessedMessageVO vo = processedMessageService.getByTgId(chatId, messageId);
        return ApiResponse.success(vo);
    }

    /**
     * 分页查询处理记录
     * GET /api/processed-message/page
     *
     * @param current 当前页码（默认1）
     * @param size 每页大小（默认10）
     * @param query 查询过滤条件
     * @return 分页响应包含已处理消息视图对象列表和分页元数据
     */
    @GetMapping("/page")
    public PageResponse<ProcessedMessageVO> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Long current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于等于1") Long size,
            @Valid ProcessedMessageQueryDTO query) {
        Page<@NonNull ProcessedMessageVO> messagePage = processedMessageService.page(current, size, query);
        return PageResponse.success(
            messagePage.getContent(),
            current,
            size,
            messagePage.getTotalElements()
        );
    }

    /**
     * 查询未读消息列表
     * GET /api/processed-message/unread
     *
     * @param current 当前页码（默认1）
     * @param size 每页大小（默认10）
     * @param chatId 频道ID（可选）
     * @return 分页响应包含未读消息列表和分页元数据
     */
    @GetMapping("/unread")
    public PageResponse<ProcessedMessageVO> getUnreadMessages(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Long current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于等于1") Long size,
            @RequestParam(required = false) Long chatId) {
        Page<@NonNull ProcessedMessageVO> messagePage = processedMessageService.getUnreadMessages(current, size, chatId);
        return PageResponse.success(
            messagePage.getContent(),
            current,
            size,
            messagePage.getTotalElements()
        );
    }

    /**
     * 查询匹配标签的消息列表
     * GET /api/processed-message/matched
     *
     * @param current 当前页码（默认1）
     * @param size 每页大小（默认10）
     * @param chatId 频道ID（可选）
     * @return 分页响应包含匹配标签的消息列表和分页元数据
     */
    @GetMapping("/matched")
    public PageResponse<ProcessedMessageVO> getMatchedMessages(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Long current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于等于1") Long size,
            @RequestParam(required = false) Long chatId) {
        Page<@NonNull ProcessedMessageVO> messagePage = processedMessageService.getMatchedMessages(current, size, chatId);
        return PageResponse.success(
            messagePage.getContent(),
            current,
            size,
            messagePage.getTotalElements()
        );
    }
}
