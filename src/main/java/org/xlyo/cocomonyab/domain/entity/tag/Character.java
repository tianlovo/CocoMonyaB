package org.xlyo.cocomonyab.domain.entity.tag;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色实体
 */
@Data
@Document(collection = "tag_characters")
public class Character {
    /**
     * 角色ID
     */
    @Id
    private String id;
    
    /**
     * 角色名称（唯一）
     */
    @Indexed(unique = true)
    private String name;
    
    /**
     * 别名列表
     */
    @Indexed
    private List<String> aliases;
    
    /**
     * 所属原作ID
     */
    @Indexed
    private String workId;
    
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
}
