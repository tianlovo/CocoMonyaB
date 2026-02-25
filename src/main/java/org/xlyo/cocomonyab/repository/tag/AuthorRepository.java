package org.xlyo.cocomonyab.repository.tag;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.xlyo.cocomonyab.domain.entity.tag.Author;

import java.util.Optional;

/**
 * 作者数据访问接口
 */
@Repository
public interface AuthorRepository extends MongoRepository<Author, String> {
    /**
     * 根据名称查询作者
     */
    Optional<Author> findByName(String name);
    
    /**
     * 根据别名查询作者
     */
    Optional<Author> findByAliasesContaining(String alias);
    
    /**
     * 检查名称是否存在（排除指定ID）
     */
    boolean existsByNameAndIdNot(String name, String id);
    
    /**
     * 检查名称是否存在
     */
    boolean existsByName(String name);
}
