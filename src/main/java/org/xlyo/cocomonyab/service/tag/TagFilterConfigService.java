package org.xlyo.cocomonyab.service.tag;

import org.xlyo.cocomonyab.domain.dto.tag.TagFilterConfigCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.TagFilterConfigUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.tag.TagFilterConfigVO;

/**
 * 标签过滤配置服务接口
 */
public interface TagFilterConfigService {
    /**
     * 创建或更新全局配置
     * 如果全局配置不存在则创建，存在则更新
     * 
     * @param dto 配置创建DTO
     * @return 配置VO
     */
    TagFilterConfigVO createOrUpdateGlobal(TagFilterConfigCreateDTO dto);
    
    /**
     * 获取全局配置
     * 
     * @return 配置VO，如果不存在则返回null
     */
    TagFilterConfigVO getGlobal();
    
    /**
     * 更新配置
     * 
     * @param id 配置ID
     * @param dto 配置更新DTO
     * @return 配置VO
     */
    TagFilterConfigVO update(String id, TagFilterConfigUpdateDTO dto);
    
    /**
     * 根据ID查询配置
     * 
     * @param id 配置ID
     * @return 配置VO
     */
    TagFilterConfigVO getById(String id);
}
