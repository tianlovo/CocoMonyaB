package org.xlyo.cocomonyab.domain.dto.tag;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 作者更新DTO
 */
@Data
public class AuthorUpdateDTO {
    /**
     * 作者名称
     */
    @Size(max = 100, message = "作者名称长度不能超过100")
    private String name;
    
    /**
     * 别名列表
     */
    private List<@Size(max = 100) String> aliases;
    
    /**
     * 个性签名
     */
    @Size(max = 500, message = "个性签名长度不能超过500")
    private String signature;
    
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
