package org.xlyo.cocomonyab.service.tag;

import org.xlyo.cocomonyab.domain.vo.tag.TagFilterConfigVO;

import java.util.List;

/**
 * 标签展开服务接口
 * 负责将标签ID展开为实际的标签字符串列表
 */
public interface TagExpansionService {
    /**
     * 展开单个作者ID为作者名称和所有别名
     * 
     * @param authorId 作者ID
     * @return 作者名称和别名列表，如果作者不存在则返回空列表
     */
    List<String> expandAuthor(String authorId);
    
    /**
     * 展开单个原作ID为原作名称和所有别名
     * 
     * @param workId 原作ID
     * @return 原作名称和别名列表，如果原作不存在则返回空列表
     */
    List<String> expandWork(String workId);
    
    /**
     * 展开单个角色ID为角色名称和所有别名
     * 
     * @param characterId 角色ID
     * @return 角色名称和别名列表，如果角色不存在则返回空列表
     */
    List<String> expandCharacter(String characterId);
    
    /**
     * 展开自定义标签ID为标签字符串
     * 
     * @param customTagId 自定义标签ID
     * @param config 标签过滤配置
     * @return 标签字符串，如果自定义标签不存在则返回null
     */
    String expandCustomTag(String customTagId, TagFilterConfigVO config);
    
    /**
     * 展开所有标签ID为完整的标签字符串列表
     * 包含所有作者、原作、角色和自定义标签，并去重
     * 
     * @param config 标签过滤配置
     * @return 去重后的标签字符串列表
     */
    List<String> expandAll(TagFilterConfigVO config);
}
