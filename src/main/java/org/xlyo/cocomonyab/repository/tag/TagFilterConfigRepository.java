package org.xlyo.cocomonyab.repository.tag;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.xlyo.cocomonyab.domain.entity.tag.TagFilterConfig;

import java.util.List;

/**
 * 标签过滤配置数据访问接口
 */
@Repository
public interface TagFilterConfigRepository extends MongoRepository<TagFilterConfig, String> {
    /**
     * 检查作者ID是否被引用
     */
    boolean existsByAuthorIdsContaining(String authorId);
    
    /**
     * 查找引用了指定作者ID的配置
     */
    List<TagFilterConfig> findByAuthorIdsContaining(String authorId);
    
    /**
     * 检查原作ID是否被引用
     */
    boolean existsByWorkIdsContaining(String workId);
    
    /**
     * 查找引用了指定原作ID的配置
     */
    List<TagFilterConfig> findByWorkIdsContaining(String workId);
    
    /**
     * 检查角色ID是否被引用
     */
    boolean existsByCharacterIdsContaining(String characterId);
    
    /**
     * 查找引用了指定角色ID的配置
     */
    List<TagFilterConfig> findByCharacterIdsContaining(String characterId);
}
