package org.xlyo.cocomonyab.service.tag.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.common.exception.ReferenceIntegrityException;
import org.xlyo.cocomonyab.common.response.PageResponse;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterQueryDTO;
import org.xlyo.cocomonyab.domain.dto.tag.CharacterUpdateDTO;
import org.xlyo.cocomonyab.domain.entity.tag.Character;
import org.xlyo.cocomonyab.domain.entity.tag.TagFilterConfig;
import org.xlyo.cocomonyab.domain.enums.EntityType;
import org.xlyo.cocomonyab.domain.vo.tag.CharacterVO;
import org.xlyo.cocomonyab.repository.tag.CharacterRepository;
import org.xlyo.cocomonyab.repository.tag.TagFilterConfigRepository;
import org.xlyo.cocomonyab.repository.tag.WorkRepository;
import org.xlyo.cocomonyab.service.tag.CharacterService;
import org.xlyo.cocomonyab.service.tag.UniquenessValidationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterServiceImpl implements CharacterService {
    
    private final CharacterRepository characterRepository;
    private final WorkRepository workRepository;
    private final TagFilterConfigRepository tagFilterConfigRepository;
    private final UniquenessValidationService uniquenessValidationService;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public CharacterVO create(CharacterCreateDTO dto) {
        // 验证名称唯一性
        uniquenessValidationService.validateNameUniqueness(
            dto.getName(), 
            null, 
            EntityType.CHARACTER
        );
        
        // 验证别名唯一性
        if (dto.getAliases() != null && !dto.getAliases().isEmpty()) {
            uniquenessValidationService.validateAliasUniqueness(
                dto.getAliases(), 
                null, 
                EntityType.CHARACTER
            );
        }
        
        // 验证原作引用（如果提供了workId）
        if (dto.getWorkId() != null && !dto.getWorkId().isEmpty()) {
            if (!workRepository.existsById(dto.getWorkId())) {
                throw new BusinessException(
                    ResponseCode.DATA_NOT_FOUND,
                    "所属原作不存在: " + dto.getWorkId()
                );
            }
        }
        
        // 创建实体
        Character character = new Character();
        character.setName(dto.getName());
        character.setAliases(dto.getAliases() != null ? dto.getAliases() : new ArrayList<>());
        character.setWorkId(dto.getWorkId());
        character.setSpecies(dto.getSpecies());
        character.setAvatarBase64(dto.getAvatarBase64());
        character.setRemark(dto.getRemark());
        character.setCreateTime(LocalDateTime.now());
        character.setUpdateTime(LocalDateTime.now());
        
        // 保存
        character = characterRepository.save(character);
        
        log.info("创建角色成功: id={}, name={}", character.getId(), character.getName());
        
        return toVO(character);
    }
    
    @Override
    @Transactional
    public CharacterVO update(String id, CharacterUpdateDTO dto) {
        // 查询角色
        Character character = characterRepository.findById(id)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "角色不存在: " + id
            ));
        
        // 更新名称
        if (dto.getName() != null && !dto.getName().equals(character.getName())) {
            uniquenessValidationService.validateNameUniqueness(
                dto.getName(), 
                id, 
                EntityType.CHARACTER
            );
            character.setName(dto.getName());
        }
        
        // 更新别名
        if (dto.getAliases() != null) {
            uniquenessValidationService.validateAliasUniqueness(
                dto.getAliases(), 
                id, 
                EntityType.CHARACTER
            );
            character.setAliases(dto.getAliases());
        }
        
        // 更新原作引用（如果提供了workId）
        if (dto.getWorkId() != null && !dto.getWorkId().equals(character.getWorkId())) {
            if (!dto.getWorkId().isEmpty() && !workRepository.existsById(dto.getWorkId())) {
                throw new BusinessException(
                    ResponseCode.DATA_NOT_FOUND,
                    "所属原作不存在: " + dto.getWorkId()
                );
            }
            character.setWorkId(dto.getWorkId());
        }
        
        // 更新其他字段
        if (dto.getSpecies() != null) {
            character.setSpecies(dto.getSpecies());
        }
        if (dto.getAvatarBase64() != null) {
            character.setAvatarBase64(dto.getAvatarBase64());
        }
        if (dto.getRemark() != null) {
            character.setRemark(dto.getRemark());
        }
        
        character.setUpdateTime(LocalDateTime.now());
        
        // 保存
        character = characterRepository.save(character);
        
        log.info("更新角色成功: id={}, name={}", character.getId(), character.getName());
        
        return toVO(character);
    }
    
    @Override
    @Transactional
    public void delete(String id, boolean force) {
        // 查询角色
        Character character = characterRepository.findById(id)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "角色不存在: " + id
            ));
        
        // 检查过滤配置引用
        List<TagFilterConfig> referencedConfigs = tagFilterConfigRepository.findByCharacterIdsContaining(id);
        
        boolean hasReferences = !referencedConfigs.isEmpty();
        
        if (hasReferences && !force) {
            // 构建详细的引用信息
            Map<String, List<String>> references = new HashMap<>();
            
            references.put("过滤配置", referencedConfigs.stream()
                    .map(TagFilterConfig::getId)
                    .collect(Collectors.toList()));
            
            throw new ReferenceIntegrityException("无法删除：该角色被引用", references);
        }
        
        // 强制删除：清理引用
        if (force && hasReferences) {
            log.warn("强制删除角色，清理引用: id={}, name={}", id, character.getName());
            
            // 清理配置引用
            for (TagFilterConfig config : referencedConfigs) {
                config.getCharacterIds().remove(id);
                tagFilterConfigRepository.save(config);
            }
        }
        
        // 删除角色
        characterRepository.deleteById(id);
        
        log.info("删除角色成功: id={}, name={}", id, character.getName());
    }
    
    @Override
    public CharacterVO getById(String id) {
        Character character = characterRepository.findById(id)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "角色不存在: " + id
            ));
        return toVO(character);
    }
    
    @Override
    public CharacterVO getByName(String name) {
        Character character = characterRepository.findByName(name)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "角色不存在: " + name
            ));
        return toVO(character);
    }
    
    @Override
    public CharacterVO getByAlias(String alias) {
        Character character = characterRepository.findByAliasesContaining(alias)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "角色不存在（别名）: " + alias
            ));
        return toVO(character);
    }
    
    @Override
    public List<CharacterVO> getByWorkId(String workId) {
        List<Character> characters = characterRepository.findByWorkId(workId);
        return characters.stream()
            .map(this::toVO)
            .collect(Collectors.toList());
    }
    
    @Override
    public PageResponse<CharacterVO> page(Long current, Long size, CharacterQueryDTO query) {
        // 构建查询条件
        Query mongoQuery = new Query();
        
        String keyword = null;
        if (query != null) {
            // 关键词搜索（名称或别名）
            if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
                keyword = query.getKeyword();
                Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
                
                Criteria criteria = new Criteria().orOperator(
                    Criteria.where("name").regex(pattern),
                    Criteria.where("aliases").regex(pattern)
                );
                mongoQuery.addCriteria(criteria);
            }
            
            // 按原作过滤
            if (query.getWorkId() != null && !query.getWorkId().isEmpty()) {
                mongoQuery.addCriteria(Criteria.where("workId").is(query.getWorkId()));
            }
            
            // 按种族过滤
            if (query.getSpecies() != null && !query.getSpecies().isEmpty()) {
                mongoQuery.addCriteria(Criteria.where("species").is(query.getSpecies()));
            }
        }
        
        // 分页和排序
        Pageable pageable = PageRequest.of(
            current.intValue() - 1, 
            size.intValue(), 
            Sort.by(Sort.Direction.DESC, "createTime")
        );
        
        mongoQuery.with(pageable);
        
        // 查询
        List<Character> characters = mongoTemplate.find(mongoQuery, Character.class);
        long total = mongoTemplate.count(Query.of(mongoQuery).limit(-1).skip(-1), Character.class);
        
        // 转换为VO，并标记匹配字段
        final String searchKeyword = keyword;
        List<CharacterVO> records = characters.stream()
            .map(character -> toVOWithMatchedField(character, searchKeyword))
            .collect(Collectors.toList());
        
        return PageResponse.success(records, current, size, total);
    }
    
    @Override
    @Transactional
    public void importFromJson(String json) {
        try {
            List<Character> characters = objectMapper.readValue(json, new TypeReference<List<Character>>() {});
            
            for (Character character : characters) {
                // 验证唯一性
                try {
                    uniquenessValidationService.validateNameUniqueness(
                        character.getName(), 
                        null, 
                        EntityType.CHARACTER
                    );
                    
                    if (character.getAliases() != null && !character.getAliases().isEmpty()) {
                        uniquenessValidationService.validateAliasUniqueness(
                            character.getAliases(), 
                            null, 
                            EntityType.CHARACTER
                        );
                    }
                    
                    // 验证原作引用
                    if (!workRepository.existsById(character.getWorkId())) {
                        log.warn("跳过角色（原作不存在）: name={}, workId={}", 
                            character.getName(), character.getWorkId());
                        continue;
                    }
                    
                    // 设置时间
                    character.setId(null); // 清除ID，让MongoDB生成新ID
                    character.setCreateTime(LocalDateTime.now());
                    character.setUpdateTime(LocalDateTime.now());
                    
                    // 保存
                    characterRepository.save(character);
                    
                    log.info("导入角色成功: name={}", character.getName());
                } catch (BusinessException e) {
                    log.warn("跳过冲突的角色: name={}, reason={}", character.getName(), e.getMessage());
                }
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                ResponseCode.VALIDATION_ERROR, 
                "JSON格式错误: " + e.getMessage()
            );
        }
    }
    
    @Override
    public String exportToJson() {
        try {
            List<Character> characters = characterRepository.findAll();
            return objectMapper.writeValueAsString(characters);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                ResponseCode.INTERNAL_ERROR, 
                "导出失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 实体转VO
     */
    private CharacterVO toVO(Character character) {
        CharacterVO vo = new CharacterVO();
        vo.setId(character.getId());
        vo.setName(character.getName());
        vo.setAliases(character.getAliases());
        vo.setWorkId(character.getWorkId());
        
        // 查询原作名称（冗余字段）
        if (character.getWorkId() != null) {
            workRepository.findById(character.getWorkId())
                .ifPresent(work -> vo.setWorkName(work.getName()));
        }
        
        vo.setSpecies(character.getSpecies());
        vo.setAvatarBase64(character.getAvatarBase64());
        vo.setRemark(character.getRemark());
        vo.setCreateTime(character.getCreateTime());
        vo.setUpdateTime(character.getUpdateTime());
        return vo;
    }
    
    /**
     * 实体转VO，并标记匹配字段
     */
    private CharacterVO toVOWithMatchedField(Character character, String keyword) {
        CharacterVO vo = toVO(character);
        
        if (keyword != null && !keyword.isEmpty()) {
            // 检查名称是否匹配
            if (character.getName() != null && 
                character.getName().toLowerCase().contains(keyword.toLowerCase())) {
                vo.setMatchedField("name");
            } 
            // 检查别名是否匹配
            else if (character.getAliases() != null) {
                for (String alias : character.getAliases()) {
                    if (alias != null && alias.toLowerCase().contains(keyword.toLowerCase())) {
                        vo.setMatchedField("alias");
                        vo.setMatchedAlias(alias);
                        break;
                    }
                }
            }
        }
        
        return vo;
    }
}
