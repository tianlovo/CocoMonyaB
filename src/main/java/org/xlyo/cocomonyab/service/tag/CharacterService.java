package org.xlyo.cocomonyab.service.tag;

import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterQueryDTO;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.tag.CharacterVO;
import org.xlyo.cocomonyab.domain.vo.tag.ImportResultVO;

import java.util.List;

/**
 * 角色服务接口
 */
public interface CharacterService {
    /**
     * 创建角色
     * 
     * @param dto 角色创建DTO
     * @return 角色VO
     */
    CharacterVO create(CharacterCreateDTO dto);
    
    /**
     * 更新角色
     * 
     * @param id 角色ID（MongoDB文档ID）
     * @param dto 角色更新DTO
     * @return 角色VO
     */
    CharacterVO update(String id, CharacterUpdateDTO dto);
    
    /**
     * 删除角色
     * 
     * @param id 角色ID（MongoDB文档ID）
     * @param force 是否强制删除（清理所有引用）
     */
    void delete(String id, boolean force);
    
    /**
     * 根据ID查询角色
     * 
     * @param id 角色ID（MongoDB文档ID）
     * @return 角色VO
     */
    CharacterVO getById(String id);
    
    /**
     * 根据名称查询角色
     * 
     * @param name 角色名称
     * @return 角色VO
     */
    CharacterVO getByName(String name);
    
    /**
     * 根据别名查询角色
     * 
     * @param alias 别名
     * @return 角色VO
     */
    CharacterVO getByAlias(String alias);
    
    /**
     * 根据原作ID查询角色列表
     * 
     * @param workId 原作ID
     * @return 角色VO列表
     */
    List<CharacterVO> getByWorkId(String workId);
    
    /**
     * 分页查询角色
     * 
     * @param current 当前页码
     * @param size 每页大小
     * @param query 查询条件
     * @return 分页响应
     */
    PageResponse<CharacterVO> page(Long current, Long size, CharacterQueryDTO query);
    
    /**
     * 从JSON导入角色数据
     * 
     * @param json JSON字符串
     * @return 导入结果
     */
    ImportResultVO importFromJson(String json);
    
    /**
     * 导出角色数据为JSON
     * 
     * @return JSON字符串
     */
    String exportToJson();
}
