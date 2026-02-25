package org.xlyo.cocomonyab.repository.tag;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.xlyo.cocomonyab.domain.entity.tag.Work;

import java.util.Optional;

/**
 * 原作数据访问接口
 */
@Repository
public interface WorkRepository extends MongoRepository<Work, String> {
    /**
     * 根据名称查询原作
     */
    Optional<Work> findByName(String name);
    
    /**
     * 根据别名查询原作
     */
    Optional<Work> findByAliasesContaining(String alias);
    
    /**
     * 检查名称是否存在（排除指定ID）
     */
    boolean existsByNameAndIdNot(String name, String id);
    
    /**
     * 检查名称是否存在
     */
    boolean existsByName(String name);
}
