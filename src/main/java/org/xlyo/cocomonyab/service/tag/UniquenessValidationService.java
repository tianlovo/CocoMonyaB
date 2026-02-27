package org.xlyo.cocomonyab.service.tag;

import org.xlyo.cocomonyab.domain.enums.EntityType;
import org.xlyo.cocomonyab.domain.vo.ConflictInfo;

import java.util.List;

/**
 * 唯一性验证服务接口
 * 负责跨库的名称和别名唯一性验证
 */
public interface UniquenessValidationService {
    
    /**
     * 验证名称唯一性
     * 
     * @param name 要验证的名称
     * @param excludeId 要排除的实体ID（用于更新操作）
     * @param entityType 实体类型
     * @throws org.xlyo.cocomonyab.common.exception.TagUniquenessException 如果名称已存在
     */
    void validateNameUniqueness(String name, String excludeId, EntityType entityType);
    
    /**
     * 验证别名唯一性
     * 
     * @param aliases 要验证的别名列表
     * @param excludeId 要排除的实体ID（用于更新操作）
     * @param entityType 实体类型
     * @throws org.xlyo.cocomonyab.common.exception.TagUniquenessException 如果别名已存在
     */
    void validateAliasUniqueness(List<String> aliases, String excludeId, EntityType entityType);
    
    /**
     * 检查名称或别名是否已存在
     * 
     * @param nameOrAlias 要检查的名称或别名
     * @param excludeId 要排除的实体ID（用于更新操作）
     * @return 冲突信息
     */
    ConflictInfo checkNameOrAliasConflict(String nameOrAlias, String excludeId);
}
