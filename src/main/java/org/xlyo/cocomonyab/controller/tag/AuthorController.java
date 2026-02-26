package org.xlyo.cocomonyab.controller.tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.tag.AuthorCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.AuthorQueryDTO;
import org.xlyo.cocomonyab.domain.dto.tag.AuthorUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.tag.AuthorVO;
import org.xlyo.cocomonyab.domain.vo.tag.ImportResultVO;
import org.xlyo.cocomonyab.service.tag.AuthorService;

/**
 * 作者管理REST控制器
 * 提供作者库的CRUD操作和导入导出功能
 */
@RestController
@RequestMapping("/api/config/tag/author")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    /**
     * 创建新作者
     * POST /api/config/tag/author
     *
     * @param dto 作者创建数据传输对象
     * @return 创建的作者视图对象
     */
    @PostMapping
    public ApiResponse<AuthorVO> createAuthor(@Valid @RequestBody AuthorCreateDTO dto) {
        AuthorVO vo = authorService.create(dto);
        return ApiResponse.success(vo);
    }

    /**
     * 更新现有作者
     * PUT /api/config/tag/author/{id}
     *
     * @param id 作者的MongoDB文档ID
     * @param dto 作者更新数据传输对象
     * @return 更新后的作者视图对象
     */
    @PutMapping("/{id}")
    public ApiResponse<AuthorVO> updateAuthor(
            @PathVariable String id,
            @Valid @RequestBody AuthorUpdateDTO dto) {
        AuthorVO vo = authorService.update(id, dto);
        return ApiResponse.success(vo);
    }

    /**
     * 删除作者
     * DELETE /api/config/tag/author/{id}
     *
     * @param id 作者的MongoDB文档ID
     * @param force 是否强制删除（清理所有引用），默认false
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAuthor(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean force) {
        authorService.delete(id, force);
        return ApiResponse.success();
    }

    /**
     * 根据ID获取作者
     * GET /api/config/tag/author/{id}
     *
     * @param id 作者的MongoDB文档ID
     * @return 作者视图对象
     */
    @GetMapping("/{id}")
    public ApiResponse<AuthorVO> getAuthor(@PathVariable String id) {
        AuthorVO vo = authorService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 根据名称获取作者
     * GET /api/config/tag/author/name/{name}
     *
     * @param name 作者名称
     * @return 作者视图对象
     */
    @GetMapping("/name/{name}")
    public ApiResponse<AuthorVO> getAuthorByName(@PathVariable String name) {
        AuthorVO vo = authorService.getByName(name);
        return ApiResponse.success(vo);
    }

    /**
     * 分页查询作者
     * GET /api/config/tag/author/page
     *
     * @param current 当前页码（默认1）
     * @param size 每页大小（默认10）
     * @param query 查询过滤条件
     * @return 分页响应包含作者视图对象列表和分页元数据
     */
    @GetMapping("/page")
    public PageResponse<AuthorVO> pageAuthors(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @Valid AuthorQueryDTO query) {
        return authorService.page(current, size, query);
    }

    /**
     * 导入作者数据
     * POST /api/config/tag/author/import
     *
     * @param json JSON格式的作者数据
     * @return 导入结果
     */
    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ImportResultVO> importAuthors(@RequestBody String json) {
        ImportResultVO result = authorService.importFromJson(json);
        return ApiResponse.success(result);
    }

    /**
     * 导出作者数据
     * GET /api/config/tag/author/export
     *
     * @return JSON格式的作者数据
     */
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<String> exportAuthors() {
        String json = authorService.exportToJson();
        return ApiResponse.success(json);
    }
}
