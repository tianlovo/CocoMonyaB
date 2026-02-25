package org.xlyo.cocomonyab.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.xlyo.cocomonyab.domain.enums.EntityType;

/**
 * 冲突信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictInfo {
    /**
     * 是否存在冲突
     */
    private boolean hasConflict;
    
    /**
     * 冲突的实体类型
     */
    private EntityType conflictType;
    
    /**
     * 冲突实体的ID
     */
    private String conflictId;
    
    /**
     * 冲突的名称
     */
    private String conflictName;
    
    /**
     * 创建无冲突的信息
     */
    public static ConflictInfo noConflict() {
        return ConflictInfo.builder()
                .hasConflict(false)
                .build();
    }
    
    /**
     * 创建有冲突的信息
     */
    public static ConflictInfo conflict(EntityType type, String id, String name) {
        return ConflictInfo.builder()
                .hasConflict(true)
                .conflictType(type)
                .conflictId(id)
                .conflictName(name)
                .build();
    }
}
