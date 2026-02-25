package org.xlyo.cocomonyab.service.tag.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xlyo.cocomonyab.domain.vo.tag.TagFilterConfigVO;
import org.xlyo.cocomonyab.repository.tag.AuthorRepository;
import org.xlyo.cocomonyab.repository.tag.CharacterRepository;
import org.xlyo.cocomonyab.repository.tag.WorkRepository;
import org.xlyo.cocomonyab.service.tag.TagExpansionService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 标签展开服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagExpansionServiceImpl implements TagExpansionService {
    
    private final AuthorRepository authorRepository;
    private final WorkRepository workRepository;
    private final CharacterRepository characterRepository;
    
    @Override
    public List<String> expandAuthor(String authorId) {
        if (authorId == null || authorId.isEmpty()) {
            return new ArrayList<>();
        }
        
        return authorRepository.findById(authorId)
                .map(author -> {
                    List<String> tags = new ArrayList<>();
                    tags.add(author.getName());
                    if (author.getAliases() != null) {
                        tags.addAll(author.getAliases());
                    }
                    return tags;
                })
                .orElseGet(() -> {
                    log.warn("标签ID不存在，已跳过: authorId={}", authorId);
                    return new ArrayList<>();
                });
    }
    
    @Override
    public List<String> expandWork(String workId) {
        if (workId == null || workId.isEmpty()) {
            return new ArrayList<>();
        }
        
        return workRepository.findById(workId)
                .map(work -> {
                    List<String> tags = new ArrayList<>();
                    tags.add(work.getName());
                    if (work.getAliases() != null) {
                        tags.addAll(work.getAliases());
                    }
                    return tags;
                })
                .orElseGet(() -> {
                    log.warn("标签ID不存在，已跳过: workId={}", workId);
                    return new ArrayList<>();
                });
    }
    
    @Override
    public List<String> expandCharacter(String characterId) {
        if (characterId == null || characterId.isEmpty()) {
            return new ArrayList<>();
        }
        
        return characterRepository.findById(characterId)
                .map(character -> {
                    List<String> tags = new ArrayList<>();
                    tags.add(character.getName());
                    if (character.getAliases() != null) {
                        tags.addAll(character.getAliases());
                    }
                    return tags;
                })
                .orElseGet(() -> {
                    log.warn("标签ID不存在，已跳过: characterId={}", characterId);
                    return new ArrayList<>();
                });
    }
    
    @Override
    public String expandCustomTag(String customTagId, TagFilterConfigVO config) {
        if (customTagId == null || customTagId.isEmpty() || config == null) {
            return null;
        }
        
        if (config.getCustomTags() == null) {
            return null;
        }
        
        String tag = config.getCustomTags().get(customTagId);
        if (tag == null) {
            log.warn("标签ID不存在，已跳过: customTagId={}", customTagId);
        }
        return tag;
    }
    
    @Override
    public List<String> expandAll(TagFilterConfigVO config) {
        if (config == null) {
            return new ArrayList<>();
        }
        
        // 使用LinkedHashSet保持插入顺序并自动去重
        Set<String> allTags = new LinkedHashSet<>();
        
        // 展开作者标签
        if (config.getAuthorIds() != null) {
            for (String authorId : config.getAuthorIds()) {
                allTags.addAll(expandAuthor(authorId));
            }
        }
        
        // 展开原作标签
        if (config.getWorkIds() != null) {
            for (String workId : config.getWorkIds()) {
                allTags.addAll(expandWork(workId));
            }
        }
        
        // 展开角色标签
        if (config.getCharacterIds() != null) {
            for (String characterId : config.getCharacterIds()) {
                allTags.addAll(expandCharacter(characterId));
            }
        }
        
        // 添加自定义标签
        if (config.getCustomTags() != null) {
            for (String customTagId : config.getCustomTags().keySet()) {
                String tag = expandCustomTag(customTagId, config);
                if (tag != null) {
                    allTags.add(tag);
                }
            }
        }
        
        return new ArrayList<>(allTags);
    }
}
