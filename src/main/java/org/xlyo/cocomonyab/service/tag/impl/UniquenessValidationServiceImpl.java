package org.xlyo.cocomonyab.service.tag.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.common.exception.TagUniquenessException;
import org.xlyo.cocomonyab.domain.entity.tag.Author;
import org.xlyo.cocomonyab.domain.entity.tag.Character;
import org.xlyo.cocomonyab.domain.entity.tag.Work;
import org.xlyo.cocomonyab.domain.enums.EntityType;
import org.xlyo.cocomonyab.domain.vo.ConflictInfo;
import org.xlyo.cocomonyab.repository.tag.AuthorRepository;
import org.xlyo.cocomonyab.repository.tag.CharacterRepository;
import org.xlyo.cocomonyab.repository.tag.WorkRepository;
import org.xlyo.cocomonyab.service.tag.UniquenessValidationService;

import java.util.List;
import java.util.Optional;

/**
 * 唯一性验证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UniquenessValidationServiceImpl implements UniquenessValidationService {
    
    private final AuthorRepository authorRepository;
    private final WorkRepository workRepository;
    private final CharacterRepository characterRepository;
    
    @Override
    public void validateNameUniqueness(String name, String excludeId, EntityType entityType) {
        log.debug("验证名称唯一性: name={}, excludeId={}, entityType={}", name, excludeId, entityType);
        
        // 检查名称是否与其他实体的名称或别名冲突
        ConflictInfo conflict = checkNameOrAliasConflict(name, excludeId);
        
        if (conflict.isHasConflict()) {
            log.warn("名称冲突: name={}, conflictType={}, conflictId={}", 
                    name, conflict.getConflictType(), conflict.getConflictId());
            throw new TagUniquenessException(
                    conflict.getConflictType().name(),
                    conflict.getConflictId(),
                    name
            );
        }
    }
    
    @Override
    public void validateAliasUniqueness(List<String> aliases, String excludeId, EntityType entityType) {
        if (aliases == null || aliases.isEmpty()) {
            return;
        }
        
        log.debug("验证别名唯一性: aliases={}, excludeId={}, entityType={}", aliases, excludeId, entityType);
        
        // 检查每个别名是否与其他实体的名称或别名冲突
        for (String alias : aliases) {
            ConflictInfo conflict = checkNameOrAliasConflict(alias, excludeId);
            
            if (conflict.isHasConflict()) {
                log.warn("别名冲突: alias={}, conflictType={}, conflictId={}", 
                        alias, conflict.getConflictType(), conflict.getConflictId());
                throw new TagUniquenessException(
                        conflict.getConflictType().name(),
                        conflict.getConflictId(),
                        alias
                );
            }
        }
    }
    
    @Override
    public ConflictInfo checkNameOrAliasConflict(String nameOrAlias, String excludeId) {
        log.debug("检查名称或别名冲突: nameOrAlias={}, excludeId={}", nameOrAlias, excludeId);
        
        // 检查作者库
        ConflictInfo authorConflict = checkInAuthorDatabase(nameOrAlias, excludeId);
        if (authorConflict.isHasConflict()) {
            return authorConflict;
        }
        
        // 检查原作库
        ConflictInfo workConflict = checkInWorkDatabase(nameOrAlias, excludeId);
        if (workConflict.isHasConflict()) {
            return workConflict;
        }
        
        // 检查角色库
        ConflictInfo characterConflict = checkInCharacterDatabase(nameOrAlias, excludeId);
        if (characterConflict.isHasConflict()) {
            return characterConflict;
        }
        
        return ConflictInfo.noConflict();
    }
    
    /**
     * 在作者库中检查冲突
     */
    private ConflictInfo checkInAuthorDatabase(String nameOrAlias, String excludeId) {
        // 检查名称
        Optional<Author> byName = authorRepository.findByName(nameOrAlias);
        if (byName.isPresent() && !byName.get().getId().equals(excludeId)) {
            return ConflictInfo.conflict(
                    EntityType.AUTHOR,
                    byName.get().getId(),
                    byName.get().getName()
            );
        }
        
        // 检查别名
        Optional<Author> byAlias = authorRepository.findByAliasesContaining(nameOrAlias);
        if (byAlias.isPresent() && !byAlias.get().getId().equals(excludeId)) {
            return ConflictInfo.conflict(
                    EntityType.AUTHOR,
                    byAlias.get().getId(),
                    byAlias.get().getName()
            );
        }
        
        return ConflictInfo.noConflict();
    }
    
    /**
     * 在原作库中检查冲突
     */
    private ConflictInfo checkInWorkDatabase(String nameOrAlias, String excludeId) {
        // 检查名称
        Optional<Work> byName = workRepository.findByName(nameOrAlias);
        if (byName.isPresent() && !byName.get().getId().equals(excludeId)) {
            return ConflictInfo.conflict(
                    EntityType.WORK,
                    byName.get().getId(),
                    byName.get().getName()
            );
        }
        
        // 检查别名
        Optional<Work> byAlias = workRepository.findByAliasesContaining(nameOrAlias);
        if (byAlias.isPresent() && !byAlias.get().getId().equals(excludeId)) {
            return ConflictInfo.conflict(
                    EntityType.WORK,
                    byAlias.get().getId(),
                    byAlias.get().getName()
            );
        }
        
        return ConflictInfo.noConflict();
    }
    
    /**
     * 在角色库中检查冲突
     */
    private ConflictInfo checkInCharacterDatabase(String nameOrAlias, String excludeId) {
        // 检查名称
        Optional<Character> byName = characterRepository.findByName(nameOrAlias);
        if (byName.isPresent() && !byName.get().getId().equals(excludeId)) {
            return ConflictInfo.conflict(
                    EntityType.CHARACTER,
                    byName.get().getId(),
                    byName.get().getName()
            );
        }
        
        // 检查别名
        Optional<Character> byAlias = characterRepository.findByAliasesContaining(nameOrAlias);
        if (byAlias.isPresent() && !byAlias.get().getId().equals(excludeId)) {
            return ConflictInfo.conflict(
                    EntityType.CHARACTER,
                    byAlias.get().getId(),
                    byAlias.get().getName()
            );
        }
        
        return ConflictInfo.noConflict();
    }
}
