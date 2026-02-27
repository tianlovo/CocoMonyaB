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
import org.xlyo.cocomonyab.domain.dto.ForwardQueueQueryDTO;
import org.xlyo.cocomonyab.domain.vo.ForwardQueueStatsVO;
import org.xlyo.cocomonyab.domain.vo.ForwardQueueVO;
import org.xlyo.cocomonyab.service.ForwardQueueService;

/**
 * 转发队列查询REST控制器
 * 提供转发队列的查询、统计等功能
 */
@RestController
@RequestMapping("/api/forward-queue")
@RequiredArgsConstructor
@Validated
public class ForwardQueueController {

    private final ForwardQueueService forwardQueueService;

    /**
     * 根据MongoDB ID查询单条队列记录
     * GET /api/forward-queue/{id}
     *
     * @param id MongoDB文档ID
     * @return 转发队列视图对象
     */
    @GetMapping("/{id}")
    public ApiResponse<ForwardQueueVO> getById(@PathVariable String id) {
        ForwardQueueVO vo = forwardQueueService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 根据源频道ID和源消息ID查询队列记录
     * GET /api/forward-queue/by-source
     *
     * @param sourceChatId 源频道ID
     * @param sourceMessageId 源消息ID
     * @return 转发队列视图对象
     */
    @GetMapping("/by-source")
    public ApiResponse<ForwardQueueVO> getBySource(
            @RequestParam @NotNull(message = "源频道ID不能为空") Long sourceChatId,
            @RequestParam @NotNull(message = "源消息ID不能为空") Long sourceMessageId) {
        ForwardQueueVO vo = forwardQueueService.getBySource(sourceChatId, sourceMessageId);
        return ApiResponse.success(vo);
    }

    /**
     * 分页查询队列记录
     * GET /api/forward-queue/page
     *
     * @param current 当前页码（默认1）
     * @param size 每页大小（默认10）
     * @param query 查询过滤条件
     * @return 分页响应包含转发队列视图对象列表和分页元数据
     */
    @GetMapping("/page")
    public PageResponse<ForwardQueueVO> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Long current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于等于1") Long size,
            @Valid ForwardQueueQueryDTO query) {
        Page<@NonNull ForwardQueueVO> queuePage = forwardQueueService.page(current, size, query);
        return PageResponse.success(
            queuePage.getContent(),
            current,
            size,
            queuePage.getTotalElements()
        );
    }

    /**
     * 查询转发统计信息
     * GET /api/forward-queue/stats
     *
     * @return 转发统计信息
     */
    @GetMapping("/stats")
    public ApiResponse<ForwardQueueStatsVO> getStats() {
        ForwardQueueStatsVO stats = forwardQueueService.getStats();
        return ApiResponse.success(stats);
    }
}
