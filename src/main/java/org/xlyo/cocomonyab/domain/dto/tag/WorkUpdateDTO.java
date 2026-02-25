package org.xlyo.cocomonyab.domain.dto.tag;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 原作更新DTO
 */
@Data
public class WorkUpdateDTO {
    /**
     * 原作名称
     */
    @Size(max = 100, message = "原作名称长度不能超过100")
    private String name;
    
    /**
     * 别名列表
     */
    private List<@Size(max = 100) String> aliases;
    
    /**
     * 网址列表
     */
    private List<@Size(max = 500) String> urls;
    
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
