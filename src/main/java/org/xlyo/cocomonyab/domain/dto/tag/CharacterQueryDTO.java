package org.xlyo.cocomonyab.domain.dto.tag;

import lombok.Data;

/**
 * 角色查询DTO
 */
@Data
public class CharacterQueryDTO {
    /**
     * 搜索关键词（名称或别名）
     */
    private String keyword;
    
    /**
     * 按原作过滤
     */
    private String workId;
    
    /**
     * 按种族过滤
     */
    private String species;
}
