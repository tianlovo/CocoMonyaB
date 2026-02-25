package org.xlyo.cocomonyab.repository.tag;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.xlyo.cocomonyab.domain.entity.tag.Character;

import java.util.List;
import java.util.Optional;

/**
 * 角色数据访问接口
 */
@Repository
public interface CharacterRepository extends MongoRepository<Character, String> {
    /**
     * 根据名称查询角色
     */
    Optional<Character> findByName(String name);
    
    /**
     * 根据别名查询角色
     */
    Optional<Character> findByAliasesContaining(String alias);
    
    /**
     * 根据原作ID查询角色列表
     */
    List<Character> findByWorkId(String workId);
    
    /**
     * 检查名称是否存在（排除指定ID）
     */
    boolean existsByNameAndIdNot(String name, String id);
    
    /**
     * 检查名称是否存在
     */
    boolean existsByName(String name);
    
    /**
     * 检查是否存在引用指定原作的角色
     */
    boolean existsByWorkId(String workId);
    
    /**
     * 根据种族查询角色列表
     */
    List<Character> findBySpecies(String species);
}
