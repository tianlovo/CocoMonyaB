package org.xlyo.cocomonyab.service.tag;

import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.tag.AuthorCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.AuthorQueryDTO;
import org.xlyo.cocomonyab.domain.dto.tag.AuthorUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.tag.AuthorVO;
import org.xlyo.cocomonyab.domain.vo.tag.ImportResultVO;

/**
 * 作者服务接口
 */
public interface AuthorService {
    /**
     * 创建作者
     * 
     * @param dto 作者创建DTO
     * @return 作者VO
     */
    AuthorVO create(AuthorCreateDTO dto);
    
    /**
     * 更新作者
     * 
     * @param id 作者ID（MongoDB文档ID）
     * @param dto 作者更新DTO
     * @return 作者VO
     */
    AuthorVO update(String id, AuthorUpdateDTO dto);
    
    /**
     * 删除作者
     * 
     * @param id 作者ID（MongoDB文档ID）
     * @param force 是否强制删除（清理所有引用）
     */
    void delete(String id, boolean force);
    
    /**
     * 根据ID查询作者
     * 
     * @param id 作者ID（MongoDB文档ID）
     * @return 作者VO
     */
    AuthorVO getById(String id);
    
    /**
     * 根据名称查询作者
     * 
     * @param name 作者名称
     * @return 作者VO
     */
    AuthorVO getByName(String name);
    
    /**
     * 根据别名查询作者
     * 
     * @param alias 别名
     * @return 作者VO
     */
    AuthorVO getByAlias(String alias);
    
    /**
     * 分页查询作者
     * 
     * @param current 当前页码
     * @param size 每页大小
     * @param query 查询条件
     * @return 分页响应
     */
    PageResponse<AuthorVO> page(Long current, Long size, AuthorQueryDTO query);
    
    /**
     * 从JSON导入作者数据
     * 
     * @param json JSON字符串
     * @return 导入结果
     */
    ImportResultVO importFromJson(String json);
    
    /**
     * 导出作者数据为JSON
     * 
     * @return JSON字符串
     */
    String exportToJson();
}
