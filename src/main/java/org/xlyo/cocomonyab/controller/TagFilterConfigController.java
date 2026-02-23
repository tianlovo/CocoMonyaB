package org.xlyo.cocomonyab.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigCreateDTO;
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigQueryDTO;
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.TagFilterConfigVO;
import org.xlyo.cocomonyab.service.TagFilterConfigService;

import java.util.List;

/**
 * 标签过滤配置REST控制器
 * 提供标签过滤配置的CRUD操作
 */
@RestController
@RequestMapping("/api/tag-filter-config")
@RequiredArgsConstructor
public class TagFilterConfigController {

    private final TagFilterConfigService service;

    /**
     * 创建或更新全局配置
     * POST /api/tag-filter-config/global
     *
     * @param dto 配置创建数据传输对象
     * @return 创建或更新的配置视图对象
     */
    @PostMapping("/global")
    public ApiResponse<TagFilterConfigVO> createOrUpdateGlobalConfig(
            @Valid @RequestBody TagFilterConfigCreateDTO dto) {
        TagFilterConfigVO vo = service.createOrUpdateGlobalConfig(dto);
        return ApiResponse.success(vo);
    }

    /**
     * 获取全局配置
     * GET /api/tag-filter-config/global
     *
     * @return 全局配置视图对象
     */
    @GetMapping("/global")
    public ApiResponse<TagFilterConfigVO> getGlobalConfig() {
        TagFilterConfigVO vo = service.getGlobalConfig();
        return ApiResponse.success(vo);
    }

    /**
     * 创建频道配置
     * POST /api/tag-filter-config/channel
     *
     * @param dto 配置创建数据传输对象
     * @return 创建的频道配置视图对象
     */
    @PostMapping("/channel")
    public ApiResponse<TagFilterConfigVO> createChannelConfig(
            @Valid @RequestBody TagFilterConfigCreateDTO dto) {
        TagFilterConfigVO vo = service.createChannelConfig(dto);
        return ApiResponse.success(vo);
    }

    /**
     * 更新配置（通过MongoDB ID）
     * PUT /api/tag-filter-config/{id}
     *
     * @param id 配置的MongoDB文档ID
     * @param dto 配置更新数据传输对象
     * @return 更新后的配置视图对象
     */
    @PutMapping("/{id}")
    public ApiResponse<TagFilterConfigVO> updateConfig(
            @PathVariable String id,
            @Valid @RequestBody TagFilterConfigUpdateDTO dto) {
        TagFilterConfigVO vo = service.updateConfig(id, dto);
        return ApiResponse.success(vo);
    }

    /**
     * 删除配置（通过MongoDB ID）
     * DELETE /api/tag-filter-config/{id}
     *
     * @param id 配置的MongoDB文档ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable String id) {
        service.deleteConfig(id);
        return ApiResponse.success();
    }

    /**
     * 通过MongoDB ID获取配置
     * GET /api/tag-filter-config/{id}
     *
     * @param id 配置的MongoDB文档ID
     * @return 配置视图对象
     */
    @GetMapping("/{id}")
    public ApiResponse<TagFilterConfigVO> getConfigById(@PathVariable String id) {
        TagFilterConfigVO vo = service.getConfigById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 通过channelId获取配置
     * GET /api/tag-filter-config/channel/{channelId}
     *
     * @param channelId Telegram频道ID
     * @return 配置视图对象
     */
    @GetMapping("/channel/{channelId}")
    public ApiResponse<TagFilterConfigVO> getConfigByChannelId(@PathVariable Long channelId) {
        TagFilterConfigVO vo = service.getConfigByChannelId(channelId);
        return ApiResponse.success(vo);
    }

    /**
     * 获取有效配置（频道配置优先，否则返回全局配置）
     * GET /api/tag-filter-config/effective/{channelId}
     *
     * @param channelId Telegram频道ID
     * @return 有效配置视图对象
     */
    @GetMapping("/effective/{channelId}")
    public ApiResponse<TagFilterConfigVO> getEffectiveConfig(@PathVariable Long channelId) {
        TagFilterConfigVO vo = service.getEffectiveConfig(channelId);
        return ApiResponse.success(vo);
    }

    /**
     * 分页查询频道配置
     * GET /api/tag-filter-config/page
     *
     * @param current 当前页码（默认1）
     * @param size 每页大小（默认10）
     * @param query 查询过滤条件
     * @return 分页响应包含配置视图对象列表和分页元数据
     */
    @GetMapping("/page")
    public PageResponse<TagFilterConfigVO> pageChannelConfigs(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @Valid TagFilterConfigQueryDTO query) {
        List<TagFilterConfigVO> records = service.pageChannelConfigs(current, size, query);
        Long total = service.countChannelConfigs(query);
        return PageResponse.success(records, current, size, total);
    }

    /**
     * 触发重新加载所有配置
     * POST /api/tag-filter-config/reload
     *
     * @return 成功响应
     */
    @PostMapping("/reload")
    public ApiResponse<Void> reloadAll() {
        service.publishReloadAllEvent();
        return ApiResponse.success();
    }
}
