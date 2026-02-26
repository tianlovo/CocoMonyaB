package org.xlyo.cocomonyab.service.tag.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
import org.xlyo.cocomonyab.domain.dto.tag.WorkCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.WorkQueryDTO;
import org.xlyo.cocomonyab.domain.dto.tag.WorkUpdateDTO;
import org.xlyo.cocomonyab.domain.entity.tag.Character;
import org.xlyo.cocomonyab.domain.entity.tag.TagFilterConfig;
import org.xlyo.cocomonyab.domain.entity.tag.Work;
import org.xlyo.cocomonyab.domain.enums.EntityType;
import org.xlyo.cocomonyab.domain.vo.tag.WorkVO;
import org.xlyo.cocomonyab.event.TagConfigurationEvent;
import org.xlyo.cocomonyab.repository.tag.CharacterRepository;
import org.xlyo.cocomonyab.repository.tag.TagFilterConfigRepository;
import org.xlyo.cocomonyab.repository.tag.WorkRepository;
import org.xlyo.cocomonyab.service.tag.UniquenessValidationService;
import org.xlyo.cocomonyab.service.tag.WorkService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 原作服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkServiceImpl implements WorkService {
    
    private final WorkRepository workRepository;
    private final CharacterRepository characterRepository;
    private final TagFilterConfigRepository tagFilterConfigRepository;
    private final UniquenessValidationService uniquenessValidationService;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    @Transactional
    public WorkVO create(WorkCreateDTO dto) {
        // 验证名称唯一性
        uniquenessValidationService.validateNameUniqueness(
            dto.getName(), 
            null, 
            EntityType.WORK
        );
        
        // 验证别名唯一性
        if (dto.getAliases() != null && !dto.getAliases().isEmpty()) {
            uniquenessValidationService.validateAliasUniqueness(
                dto.getAliases(), 
                null, 
                EntityType.WORK
            );
        }
        
        // 创建实体
        Work work = new Work();
        work.setName(dto.getName());
        work.setAliases(dto.getAliases() != null ? dto.getAliases() : new ArrayList<>());
        work.setUrls(dto.getUrls() != null ? dto.getUrls() : new ArrayList<>());
        work.setAvatarBase64(dto.getAvatarBase64());
        work.setRemark(dto.getRemark());
        work.setCreateTime(LocalDateTime.now());
        work.setUpdateTime(LocalDateTime.now());
        
        // 保存
        work = workRepository.save(work);
        
        // 发布作品标签变更事件
        eventPublisher.publishEvent(
            TagConfigurationEvent.workChanged(this, work.getId())
        );
        
        log.info("创建原作成功: id={}, name={}", work.getId(), work.getName());
        
        return toVO(work);
    }
    
    @Override
    @Transactional
    public WorkVO update(String id, WorkUpdateDTO dto) {
        // 查询原作
        Work work = workRepository.findById(id)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "原作不存在: " + id
            ));
        
        // 更新名称
        if (dto.getName() != null && !dto.getName().equals(work.getName())) {
            uniquenessValidationService.validateNameUniqueness(
                dto.getName(), 
                id, 
                EntityType.WORK
            );
            work.setName(dto.getName());
        }
        
        // 更新别名
        if (dto.getAliases() != null) {
            uniquenessValidationService.validateAliasUniqueness(
                dto.getAliases(), 
                id, 
                EntityType.WORK
            );
            work.setAliases(dto.getAliases());
        }
        
        // 更新其他字段
        if (dto.getUrls() != null) {
            work.setUrls(dto.getUrls());
        }
        if (dto.getAvatarBase64() != null) {
            work.setAvatarBase64(dto.getAvatarBase64());
        }
        if (dto.getRemark() != null) {
            work.setRemark(dto.getRemark());
        }
        
        work.setUpdateTime(LocalDateTime.now());
        
        // 保存
        work = workRepository.save(work);
        
        // 发布作品标签变更事件
        eventPublisher.publishEvent(
            TagConfigurationEvent.workChanged(this, work.getId())
        );
        
        log.info("更新原作成功: id={}, name={}", work.getId(), work.getName());
        
        return toVO(work);
    }
    
    @Override
    @Transactional
    public void delete(String id, boolean force) {
        // 查询原作
        Work work = workRepository.findById(id)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "原作不存在: " + id
            ));
        
        // 检查引用
        List<Character> referencedCharacters = characterRepository.findByWorkId(id);
        List<TagFilterConfig> referencedConfigs = tagFilterConfigRepository.findByWorkIdsContaining(id);
        
        boolean hasReferences = !referencedCharacters.isEmpty() || !referencedConfigs.isEmpty();
        
        if (hasReferences && !force) {
            // 构建详细的引用信息
            Map<String, List<String>> references = new HashMap<>();
            
            if (!referencedCharacters.isEmpty()) {
                references.put("角色", referencedCharacters.stream()
                        .map(c -> c.getId() + "(" + c.getName() + ")")
                        .collect(Collectors.toList()));
            }
            
            if (!referencedConfigs.isEmpty()) {
                references.put("过滤配置", referencedConfigs.stream()
                        .map(TagFilterConfig::getId)
                        .collect(Collectors.toList()));
            }
            
            throw new ReferenceIntegrityException("无法删除：该原作被引用", references);
        }
        
        // 强制删除：清理引用
        if (force && hasReferences) {
            log.warn("强制删除原作，清理引用: id={}, name={}", id, work.getName());
            
            // 清理角色引用（删除所有引用该原作的角色）
            for (Character character : referencedCharacters) {
                characterRepository.deleteById(character.getId());
                log.info("删除角色: id={}, name={}", character.getId(), character.getName());
            }
            
            // 清理配置引用
            for (TagFilterConfig config : referencedConfigs) {
                config.getWorkIds().remove(id);
                tagFilterConfigRepository.save(config);
            }
        }
        
        // 删除原作
        workRepository.deleteById(id);
        
        // 发布作品标签变更事件
        eventPublisher.publishEvent(
            TagConfigurationEvent.workChanged(this, id)
        );
        
        log.info("删除原作成功: id={}, name={}", id, work.getName());
    }
    
    @Override
    public WorkVO getById(String id) {
        Work work = workRepository.findById(id)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "原作不存在: " + id
            ));
        return toVO(work);
    }
    
    @Override
    public WorkVO getByName(String name) {
        Work work = workRepository.findByName(name)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "原作不存在: " + name
            ));
        return toVO(work);
    }
    
    @Override
    public WorkVO getByAlias(String alias) {
        Work work = workRepository.findByAliasesContaining(alias)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "原作不存在（别名）: " + alias
            ));
        return toVO(work);
    }
    
    @Override
    public PageResponse<WorkVO> page(Long current, Long size, WorkQueryDTO query) {
        // 构建查询条件
        Query mongoQuery = new Query();
        
        String keyword = null;
        if (query != null && query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            keyword = query.getKeyword();
            Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
            
            Criteria criteria = new Criteria().orOperator(
                Criteria.where("name").regex(pattern),
                Criteria.where("aliases").regex(pattern)
            );
            mongoQuery.addCriteria(criteria);
        }
        
        // 分页和排序
        Pageable pageable = PageRequest.of(
            current.intValue() - 1, 
            size.intValue(), 
            Sort.by(Sort.Direction.DESC, "createTime")
        );
        
        mongoQuery.with(pageable);
        
        // 查询
        List<Work> works = mongoTemplate.find(mongoQuery, Work.class);
        long total = mongoTemplate.count(Query.of(mongoQuery).limit(-1).skip(-1), Work.class);
        
        // 转换为VO，并标记匹配字段
        final String searchKeyword = keyword;
        List<WorkVO> records = works.stream()
            .map(work -> toVOWithMatchedField(work, searchKeyword))
            .collect(Collectors.toList());
        
        return PageResponse.success(records, current, size, total);
    }
    
    @Override
    @Transactional
    public void importFromJson(String json) {
        try {
            List<Work> works = objectMapper.readValue(json, new TypeReference<List<Work>>() {});
            
            for (Work work : works) {
                // 验证唯一性
                try {
                    uniquenessValidationService.validateNameUniqueness(
                        work.getName(), 
                        null, 
                        EntityType.WORK
                    );
                    
                    if (work.getAliases() != null && !work.getAliases().isEmpty()) {
                        uniquenessValidationService.validateAliasUniqueness(
                            work.getAliases(), 
                            null, 
                            EntityType.WORK
                        );
                    }
                    
                    // 设置时间
                    work.setId(null); // 清除ID，让MongoDB生成新ID
                    work.setCreateTime(LocalDateTime.now());
                    work.setUpdateTime(LocalDateTime.now());
                    
                    // 保存
                    workRepository.save(work);
                    
                    log.info("导入原作成功: name={}", work.getName());
                } catch (BusinessException e) {
                    log.warn("跳过冲突的原作: name={}, reason={}", work.getName(), e.getMessage());
                }
            }
            
            // 批量导入完成后，发布重新加载事件
            eventPublisher.publishEvent(
                TagConfigurationEvent.reloadAll(this)
            );
            
            log.info("批量导入原作完成，已发布标签配置重新加载事件");
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
            List<Work> works = workRepository.findAll();
            return objectMapper.writeValueAsString(works);
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
    private WorkVO toVO(Work work) {
        WorkVO vo = new WorkVO();
        vo.setId(work.getId());
        vo.setName(work.getName());
        vo.setAliases(work.getAliases());
        vo.setUrls(work.getUrls());
        vo.setAvatarBase64(work.getAvatarBase64());
        vo.setRemark(work.getRemark());
        vo.setCreateTime(work.getCreateTime());
        vo.setUpdateTime(work.getUpdateTime());
        return vo;
    }
    
    /**
     * 实体转VO，并标记匹配字段
     */
    private WorkVO toVOWithMatchedField(Work work, String keyword) {
        WorkVO vo = toVO(work);
        
        if (keyword != null && !keyword.isEmpty()) {
            // 检查名称是否匹配
            if (work.getName() != null && 
                work.getName().toLowerCase().contains(keyword.toLowerCase())) {
                vo.setMatchedField("name");
            } 
            // 检查别名是否匹配
            else if (work.getAliases() != null) {
                for (String alias : work.getAliases()) {
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
