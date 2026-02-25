package org.xlyo.cocomonyab.domain.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 角色创建DTO
 */
@Data
public class CharacterCreateDTO {
    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100, message = "角色名称长度不能超过100")
    private String name;
    
    /**
     * 别名列表
     */
    @NotNull(message = "别名列表不能为null")
    private List<@NotBlank @Size(max = 100) String> aliases;
    
    /**
     * 所属原作ID（可为null）
     */
    private String workId;
    
    /**
     * 种族
     */
    @NotBlank(message = "种族不能为空")
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
