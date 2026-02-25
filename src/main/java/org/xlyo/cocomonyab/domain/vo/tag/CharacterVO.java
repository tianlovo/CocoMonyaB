package org.xlyo.cocomonyab.domain.vo.tag;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色视图对象
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CharacterVO {
    /**
     * 角色ID
     */
    private String id;
    
    /**
     * 角色名称
     */
    private String name;
    
    /**
     * 别名列表
     */
    private List<String> aliases;
    
    /**
     * 所属原作ID
     */
    private String workId;
    
    /**
     * 所属原作名称（冗余字段，方便展示）
     */
    private String workName;
    
    /**
     * 种族
     */
    private String species;
    
    /**
     * BASE64编码的头像
     */
    private String avatarBase64;
    
    /**
     * 备注信息
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 匹配字段（用于搜索结果标记）
     * 可能的值: "name", "alias"
     */
    private String matchedField;
    
    /**
     * 匹配的别名（当matchedField为"alias"时）
     */
    private String matchedAlias;
}
