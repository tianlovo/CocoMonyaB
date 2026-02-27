package org.xlyo.cocomonyab.domain.vo.tag;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作者视图对象
 */
@Data
public class AuthorVO {
    /**
     * 作者ID（MongoDB文档ID）
     */
    private String id;
    
    /**
     * 作者名称
     */
    private String name;
    
    /**
     * 别名列表
     */
    private List<String> aliases;
    
    /**
     * 个性签名
     */
    private String signature;
    
    /**
     * 网址列表
     */
    private List<String> urls;
    
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
