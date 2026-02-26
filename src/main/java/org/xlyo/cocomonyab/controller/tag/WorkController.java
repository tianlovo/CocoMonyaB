package org.xlyo.cocomonyab.controller.tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.tag.WorkCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.WorkQueryDTO;
import org.xlyo.cocomonyab.domain.dto.tag.WorkUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.tag.ImportResultVO;
import org.xlyo.cocomonyab.domain.vo.tag.WorkVO;
import org.xlyo.cocomonyab.service.tag.WorkService;

/**
 * 原作管理REST控制器
 * 提供原作库的CRUD操作和导入导出功能
 */
@RestController
@RequestMapping("/api/config/tag/work")
@RequiredArgsConstructor
public class WorkController {

    private final WorkService workService;

    /**
     * 创建新原作
     * POST /api/config/tag/work
     *
     * @param dto 原作创建数据传输对象
     * @return 创建的原作视图对象
     */
    @PostMapping
    public ApiResponse<WorkVO> createWork(@Valid @RequestBody WorkCreateDTO dto) {
        WorkVO vo = workService.create(dto);
        return ApiResponse.success(vo);
    }

    /**
     * 更新现有原作
     * PUT /api/config/tag/work/{id}
     *
     * @param id 原作的MongoDB文档ID
     * @param dto 原作更新数据传输对象
     * @return 更新后的原作视图对象
     */
    @PutMapping("/{id}")
    public ApiResponse<WorkVO> updateWork(
            @PathVariable String id,
            @Valid @RequestBody WorkUpdateDTO dto) {
        WorkVO vo = workService.update(id, dto);
        return ApiResponse.success(vo);
    }

    /**
     * 删除原作
     * DELETE /api/config/tag/work/{id}
     *
     * @param id 原作的MongoDB文档ID
     * @param force 是否强制删除（清理所有引用），默认false
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWork(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean force) {
        workService.delete(id, force);
        return ApiResponse.success();
    }

    /**
     * 根据ID获取原作
     * GET /api/config/tag/work/{id}
     *
     * @param id 原作的MongoDB文档ID
     * @return 原作视图对象
     */
    @GetMapping("/{id}")
    public ApiResponse<WorkVO> getWork(@PathVariable String id) {
        WorkVO vo = workService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 根据名称获取原作
     * GET /api/config/tag/work/name/{name}
     *
     * @param name 原作名称
     * @return 原作视图对象
     */
    @GetMapping("/name/{name}")
    public ApiResponse<WorkVO> getWorkByName(@PathVariable String name) {
        WorkVO vo = workService.getByName(name);
        return ApiResponse.success(vo);
    }

    /**
     * 分页查询原作
     * GET /api/config/tag/work/page
     *
     * @param current 当前页码（默认1）
     * @param size 每页大小（默认10）
     * @param query 查询过滤条件
     * @return 分页响应包含原作视图对象列表和分页元数据
     */
    @GetMapping("/page")
    public PageResponse<WorkVO> pageWorks(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @Valid WorkQueryDTO query) {
        return workService.page(current, size, query);
    }

    /**
     * 导入原作数据
     * POST /api/config/tag/work/import
     *
     * @param json JSON格式的原作数据
     * @return 导入结果
     */
    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ImportResultVO> importWorks(@RequestBody String json) {
        ImportResultVO result = workService.importFromJson(json);
        return ApiResponse.success(result);
    }

    /**
     * 导出原作数据
     * GET /api/config/tag/work/export
     *
     * @return JSON格式的原作数据
     */
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<String> exportWorks() {
        String json = workService.exportToJson();
        return ApiResponse.success(json);
    }
}
