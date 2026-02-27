package org.xlyo.cocomonyab.controller.tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.domain.dto.tag.TagFilterConfigCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.TagFilterConfigUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.tag.TagFilterConfigVO;
import org.xlyo.cocomonyab.service.tag.TagExpansionService;
import org.xlyo.cocomonyab.service.tag.TagFilterConfigService;

import java.util.List;

/**
 * 标签过滤配置REST控制器
 * 提供全局标签过滤配置的管理和标签展开功能
 */
@RestController
@RequestMapping("/api/config/tag/filter")
@RequiredArgsConstructor
public class TagFilterConfigController {

    private final TagFilterConfigService tagFilterConfigService;
    private final TagExpansionService tagExpansionService;

    /**
     * 创建或更新全局配置
     * POST /api/config/tag/filter
     * 如果全局配置不存在则创建，存在则更新
     *
     * @param dto 配置创建数据传输对象
     * @return 配置视图对象
     */
    @PostMapping
    public ApiResponse<TagFilterConfigVO> createOrUpdateGlobal(@Valid @RequestBody TagFilterConfigCreateDTO dto) {
        TagFilterConfigVO vo = tagFilterConfigService.createOrUpdateGlobal(dto);
        return ApiResponse.success(vo);
    }

    /**
     * 获取全局配置
     * GET /api/config/tag/filter
     *
     * @return 配置视图对象
     */
    @GetMapping
    public ApiResponse<TagFilterConfigVO> getGlobal() {
        TagFilterConfigVO vo = tagFilterConfigService.getGlobal();
        return ApiResponse.success(vo);
    }

    /**
     * 更新配置
     * PUT /api/config/tag/filter/{id}
     *
     * @param id 配置的MongoDB文档ID
     * @param dto 配置更新数据传输对象
     * @return 更新后的配置视图对象
     */
    @PutMapping("/{id}")
    public ApiResponse<TagFilterConfigVO> updateConfig(
            @PathVariable String id,
            @Valid @RequestBody TagFilterConfigUpdateDTO dto) {
        TagFilterConfigVO vo = tagFilterConfigService.update(id, dto);
        return ApiResponse.success(vo);
    }

    /**
     * 根据ID获取配置
     * GET /api/config/tag/filter/{id}
     *
     * @param id 配置的MongoDB文档ID
     * @return 配置视图对象
     */
    @GetMapping("/{id}")
    public ApiResponse<TagFilterConfigVO> getConfig(@PathVariable String id) {
        TagFilterConfigVO vo = tagFilterConfigService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 展开标签（用于测试）
     * POST /api/config/tag/filter/expand
     * 将配置中的所有标签ID展开为实际的标签字符串列表
     *
     * @param dto 配置数据传输对象
     * @return 展开后的标签字符串列表
     */
    @PostMapping("/expand")
    public ApiResponse<List<String>> expandTags(@Valid @RequestBody TagFilterConfigCreateDTO dto) {
        // 将DTO转换为VO以便展开
        TagFilterConfigVO vo = new TagFilterConfigVO();
        vo.setAuthorIds(dto.getAuthorIds());
        vo.setCharacterIds(dto.getCharacterIds());
        vo.setWorkIds(dto.getWorkIds());
        vo.setCustomTags(dto.getCustomTags());
        
        List<String> expandedTags = tagExpansionService.expandAll(vo);
        return ApiResponse.success(expandedTags);
    }
}
