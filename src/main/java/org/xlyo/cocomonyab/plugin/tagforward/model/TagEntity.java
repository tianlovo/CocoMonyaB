package org.xlyo.cocomonyab.plugin.tagforward.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.util.List;

/**
 * 标签实体数据模型
 * <p>
 * 用于从tag_authors、tag_characters、tag_works集合读取标签信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagEntity {
    
    /**
     * 标签ID
     */
    @Id
    private String id;
    
    /**
     * 标签名称
     */
    private String name;
    
    /**
     * 标签别名列表
     */
    private List<String> aliases;
}
