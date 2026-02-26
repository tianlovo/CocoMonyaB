package org.xlyo.cocomonyab.controller.tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.xlyo.cocomonyab.common.response.ApiResponse;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterQueryDTO;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.tag.CharacterVO;
import org.xlyo.cocomonyab.domain.vo.tag.ImportResultVO;
import org.xlyo.cocomonyab.service.tag.CharacterService;

import java.util.List;

/**
 * 角色管理REST控制器
 * 提供角色库的CRUD操作和导入导出功能
 */
@RestController
@RequestMapping("/api/config/tag/character")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    /**
     * 创建新角色
     * POST /api/config/tag/character
     *
     * @param dto 角色创建数据传输对象
     * @return 创建的角色视图对象
     */
    @PostMapping
    public ApiResponse<CharacterVO> createCharacter(@Valid @RequestBody CharacterCreateDTO dto) {
        CharacterVO vo = characterService.create(dto);
        return ApiResponse.success(vo);
    }

    /**
     * 更新现有角色
     * PUT /api/config/tag/character/{id}
     *
     * @param id 角色的MongoDB文档ID
     * @param dto 角色更新数据传输对象
     * @return 更新后的角色视图对象
     */
    @PutMapping("/{id}")
    public ApiResponse<CharacterVO> updateCharacter(
            @PathVariable String id,
            @Valid @RequestBody CharacterUpdateDTO dto) {
        CharacterVO vo = characterService.update(id, dto);
        return ApiResponse.success(vo);
    }

    /**
     * 删除角色
     * DELETE /api/config/tag/character/{id}
     *
     * @param id 角色的MongoDB文档ID
     * @param force 是否强制删除（清理所有引用），默认false
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCharacter(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean force) {
        characterService.delete(id, force);
        return ApiResponse.success();
    }

    /**
     * 根据ID获取角色
     * GET /api/config/tag/character/{id}
     *
     * @param id 角色的MongoDB文档ID
     * @return 角色视图对象
     */
    @GetMapping("/{id}")
    public ApiResponse<CharacterVO> getCharacter(@PathVariable String id) {
        CharacterVO vo = characterService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 根据名称获取角色
     * GET /api/config/tag/character/name/{name}
     *
     * @param name 角色名称
     * @return 角色视图对象
     */
    @GetMapping("/name/{name}")
    public ApiResponse<CharacterVO> getCharacterByName(@PathVariable String name) {
        CharacterVO vo = characterService.getByName(name);
        return ApiResponse.success(vo);
    }

    /**
     * 根据原作ID获取角色列表
     * GET /api/config/tag/character/work/{workId}
     *
     * @param workId 原作的MongoDB文档ID
     * @return 角色视图对象列表
     */
    @GetMapping("/work/{workId}")
    public ApiResponse<List<CharacterVO>> getCharactersByWork(@PathVariable String workId) {
        List<CharacterVO> list = characterService.getByWorkId(workId);
        return ApiResponse.success(list);
    }

    /**
     * 分页查询角色
     * GET /api/config/tag/character/page
     *
     * @param current 当前页码（默认1）
     * @param size 每页大小（默认10）
     * @param query 查询过滤条件（支持种族过滤）
     * @return 分页响应包含角色视图对象列表和分页元数据
     */
    @GetMapping("/page")
    public PageResponse<CharacterVO> pageCharacters(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @Valid CharacterQueryDTO query) {
        return characterService.page(current, size, query);
    }

    /**
     * 导入角色数据
     * POST /api/config/tag/character/import
     *
     * @param json JSON格式的角色数据
     * @return 导入结果
     */
    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ImportResultVO> importCharacters(@RequestBody String json) {
        ImportResultVO result = characterService.importFromJson(json);
        return ApiResponse.success(result);
    }

    /**
     * 导出角色数据
     * GET /api/config/tag/character/export
     *
     * @return JSON格式的角色数据
     */
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<String> exportCharacters() {
        String json = characterService.exportToJson();
        return ApiResponse.success(json);
    }
}
