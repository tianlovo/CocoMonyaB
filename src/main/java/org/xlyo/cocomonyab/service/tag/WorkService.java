package org.xlyo.cocomonyab.service.tag;

import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.tag.WorkCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.WorkQueryDTO;
import org.xlyo.cocomonyab.domain.dto.tag.WorkUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.tag.ImportResultVO;
import org.xlyo.cocomonyab.domain.vo.tag.WorkVO;

/**
 * 原作服务接口
 */
public interface WorkService {
    /**
     * 创建原作
     * 
     * @param dto 原作创建DTO
     * @return 原作VO
     */
    WorkVO create(WorkCreateDTO dto);
    
    /**
     * 更新原作
     * 
     * @param id 原作ID（MongoDB文档ID）
     * @param dto 原作更新DTO
     * @return 原作VO
     */
    WorkVO update(String id, WorkUpdateDTO dto);
    
    /**
     * 删除原作
     * 
     * @param id 原作ID（MongoDB文档ID）
     * @param force 是否强制删除（清理所有引用）
     */
    void delete(String id, boolean force);
    
    /**
     * 根据ID查询原作
     * 
     * @param id 原作ID（MongoDB文档ID）
     * @return 原作VO
     */
    WorkVO getById(String id);
    
    /**
     * 根据名称查询原作
     * 
     * @param name 原作名称
     * @return 原作VO
     */
    WorkVO getByName(String name);
    
    /**
     * 根据别名查询原作
     * 
     * @param alias 别名
     * @return 原作VO
     */
    WorkVO getByAlias(String alias);
    
    /**
     * 分页查询原作
     * 
     * @param current 当前页码
     * @param size 每页大小
     * @param query 查询条件
     * @return 分页响应
     */
    PageResponse<WorkVO> page(Long current, Long size, WorkQueryDTO query);
    
    /**
     * 从JSON导入原作数据
     * 
     * @param json JSON字符串
     * @return 导入结果
     */
    ImportResultVO importFromJson(String json);
    
    /**
     * 导出原作数据为JSON
     * 
     * @return JSON字符串
     */
    String exportToJson();
}
