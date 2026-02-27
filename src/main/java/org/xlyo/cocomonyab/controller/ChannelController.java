package org.xlyo.cocomonyab.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.ChannelCreateDTO;
import org.xlyo.cocomonyab.domain.dto.ChannelQueryDTO;
import org.xlyo.cocomonyab.domain.dto.ChannelUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.ChannelVO;
import org.xlyo.cocomonyab.domain.vo.TgChannelVO;
import org.xlyo.cocomonyab.service.ChannelService;
import org.xlyo.cocomonyab.service.TgChannelService;

import java.util.List;

/**
 * Channel管理REST控制器
 * 提供Telegram频道监控配置的CRUD操作
 */
@RestController
@RequestMapping("/api/channel")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
    private final TgChannelService tgChannelService;

    /**
     * 创建新channel
     * POST /api/channel
     *
     * @param dto channel创建数据传输对象
     * @return 创建的channel视图对象
     */
    @PostMapping
    public ApiResponse<ChannelVO> createChannel(@Valid @RequestBody ChannelCreateDTO dto) {
        ChannelVO vo = channelService.create(dto);
        return ApiResponse.success(vo);
    }

    /**
     * 更新现有channel
     * PUT /api/channel/{id}
     *
     * @param id channel的MongoDB文档ID
     * @param dto channel更新数据传输对象
     * @return 更新后的channel视图对象
     */
    @PutMapping("/{id}")
    public ApiResponse<ChannelVO> updateChannel(
            @PathVariable String id,
            @Valid @RequestBody ChannelUpdateDTO dto) {
        ChannelVO vo = channelService.update(id, dto);
        return ApiResponse.success(vo);
    }

    /**
     * 删除channel
     * DELETE /api/channel/{id}
     *
     * @param id channel的MongoDB文档ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteChannel(@PathVariable String id) {
        channelService.deleteById(id);
        return ApiResponse.success();
    }

    /**
     * 根据ID获取channel
     * GET /api/channel/{id}
     *
     * @param id channel的MongoDB文档ID
     * @return channel视图对象
     */
    @GetMapping("/{id}")
    public ApiResponse<ChannelVO> getChannel(@PathVariable String id) {
        ChannelVO vo = channelService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 获取所有channels列表
     * GET /api/channel/list
     *
     * @return channel视图对象列表
     */
    @GetMapping("/list")
    public ApiResponse<List<ChannelVO>> listChannels() {
        List<ChannelVO> list = channelService.list();
        return ApiResponse.success(list);
    }

    /**
     * 分页查询channels
     * GET /api/channel/page
     *
     * @param current 当前页码（默认1）
     * @param size 每页大小（默认10）
     * @param query 查询过滤条件
     * @return 分页响应包含channel视图对象列表和分页元数据
     */
    @GetMapping("/page")
    public PageResponse<ChannelVO> pageChannels(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @Valid ChannelQueryDTO query) {
        List<ChannelVO> records = channelService.page(current, size, query);
        Long total = channelService.count(query);
        return PageResponse.success(records, current, size, total);
    }

    /**
     * 分页查询已登录TG账号的频道列表
     * GET /api/channel/tg/logged-in
     *
     * @param current 当前页码（默认1）
     * @param size 每页大小（默认10）
     * @param forceRefresh 是否强制从TDLib刷新数据（默认false）
     * @return 分页响应包含TG频道视图对象列表和分页元数据
     */
    @GetMapping("/tg/logged-in")
    public PageResponse<TgChannelVO> getLoggedInTgChannels(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(defaultValue = "false") Boolean forceRefresh) {
        List<TgChannelVO> records = tgChannelService.getLoggedInChannels(current, size, forceRefresh);
        Long total = tgChannelService.countLoggedInChannels();
        return PageResponse.success(records, current, size, total);
    }
}
