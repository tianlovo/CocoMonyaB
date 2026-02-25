package org.xlyo.cocomonyab.domain.dto.tag;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 角色更新DTO
 */
@Data
public class CharacterUpdateDTO {
    /**
     * 角色名称
     */
    @Size(max = 100, message = "角色名称长度不能超过100")
    private String name;
    
    /**
     * 别名列表
     */
    private List<@Size(max = 100) String> aliases;
    
    /**
     * 所属原作ID
     */
    private String workId;
    
    /**
     * 种族
     */
    @Size(max = 100, message = "种族长度不能超过100")
    private String species;
    
    /**
     * BASE64编码的头像
     */
    private String avatarBase64;
    
    /**
     * 备注信息
     */
    @Size(max = 1000, message = "备注长度不能超过1000")
    private String remark;
}
