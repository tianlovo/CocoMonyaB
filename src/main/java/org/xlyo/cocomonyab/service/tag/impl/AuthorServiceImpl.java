package org.xlyo.cocomonyab.service.tag.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.xlyo.cocomonyab.domain.dto.tag.AuthorCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.AuthorQueryDTO;
import org.xlyo.cocomonyab.domain.dto.tag.AuthorUpdateDTO;
import org.xlyo.cocomonyab.domain.entity.tag.Author;
import org.xlyo.cocomonyab.domain.entity.tag.Character;
import org.xlyo.cocomonyab.domain.entity.tag.TagFilterConfig;
import org.xlyo.cocomonyab.domain.enums.EntityType;
import org.xlyo.cocomonyab.domain.vo.tag.AuthorVO;
import org.xlyo.cocomonyab.domain.vo.tag.ImportResultVO;
import org.xlyo.cocomonyab.event.TagConfigurationEvent;
import org.xlyo.cocomonyab.repository.tag.AuthorRepository;
import org.xlyo.cocomonyab.repository.tag.CharacterRepository;
import org.xlyo.cocomonyab.repository.tag.TagFilterConfigRepository;
import org.xlyo.cocomonyab.service.tag.AuthorService;
import org.xlyo.cocomonyab.service.tag.UniquenessValidationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 作者服务实现类
 */
@Slf4j
@Service
public class AuthorServiceImpl implements AuthorService {
    
    private final AuthorRepository authorRepository;
    private final CharacterRepository characterRepository;
    private final TagFilterConfigRepository tagFilterConfigRepository;
    private final UniquenessValidationService uniquenessValidationService;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    
    public AuthorServiceImpl(
            AuthorRepository authorRepository,
            CharacterRepository characterRepository,
            TagFilterConfigRepository tagFilterConfigRepository,
            UniquenessValidationService uniquenessValidationService,
            MongoTemplate mongoTemplate,
            @Qualifier("exportObjectMapper") ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher) {
        this.authorRepository = authorRepository;
        this.characterRepository = characterRepository;
        this.tagFilterConfigRepository = tagFilterConfigRepository;
        this.uniquenessValidationService = uniquenessValidationService;
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    @Transactional
    public AuthorVO create(AuthorCreateDTO dto) {
        // 验证名称唯一性
        uniquenessValidationService.validateNameUniqueness(
            dto.getName(), 
            null, 
            EntityType.AUTHOR
        );
        
        // 验证别名唯一性
        if (dto.getAliases() != null && !dto.getAliases().isEmpty()) {
            uniquenessValidationService.validateAliasUniqueness(
                dto.getAliases(), 
                null, 
                EntityType.AUTHOR
            );
        }
        
        // 创建实体
        Author author = new Author();
        author.setName(dto.getName());
        author.setAliases(dto.getAliases() != null ? dto.getAliases() : new ArrayList<>());
        author.setSignature(dto.getSignature());
        author.setUrls(dto.getUrls() != null ? dto.getUrls() : new ArrayList<>());
        author.setAvatarBase64(dto.getAvatarBase64());
        author.setRemark(dto.getRemark());
        author.setCreateTime(LocalDateTime.now());
        author.setUpdateTime(LocalDateTime.now());
        
        // 保存
        author = authorRepository.save(author);
        
        // 发布作者标签变更事件
        eventPublisher.publishEvent(
            TagConfigurationEvent.authorChanged(this, author.getId())
        );
        
        log.info("创建作者成功: id={}, name={}", author.getId(), author.getName());
        
        return toVO(author);
    }
    
    @Override
    @Transactional
    public AuthorVO update(String id, AuthorUpdateDTO dto) {
        // 查询作者
        Author author = authorRepository.findById(id)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "作者不存在: " + id
            ));
        
        // 更新名称
        if (dto.getName() != null && !dto.getName().equals(author.getName())) {
            uniquenessValidationService.validateNameUniqueness(
                dto.getName(), 
                id, 
                EntityType.AUTHOR
            );
            author.setName(dto.getName());
        }
        
        // 更新别名
        if (dto.getAliases() != null) {
            uniquenessValidationService.validateAliasUniqueness(
                dto.getAliases(), 
                id, 
                EntityType.AUTHOR
            );
            author.setAliases(dto.getAliases());
        }
        
        // 更新其他字段
        if (dto.getSignature() != null) {
            author.setSignature(dto.getSignature());
        }
        if (dto.getUrls() != null) {
            author.setUrls(dto.getUrls());
        }
        if (dto.getAvatarBase64() != null) {
            author.setAvatarBase64(dto.getAvatarBase64());
        }
        if (dto.getRemark() != null) {
            author.setRemark(dto.getRemark());
        }
        
        author.setUpdateTime(LocalDateTime.now());
        
        // 保存
        author = authorRepository.save(author);
        
        // 发布作者标签变更事件
        eventPublisher.publishEvent(
            TagConfigurationEvent.authorChanged(this, author.getId())
        );
        
        log.info("更新作者成功: id={}, name={}", author.getId(), author.getName());
        
        return toVO(author);
    }
    
    @Override
    @Transactional
    public void delete(String id, boolean force) {
        // 查询作者
        Author author = authorRepository.findById(id)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "作者不存在: " + id
            ));
        
        // 检查引用
        List<Character> referencedCharacters = characterRepository.findByWorkId(id);
        List<TagFilterConfig> referencedConfigs = tagFilterConfigRepository.findByAuthorIdsContaining(id);
        
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
            
            throw new ReferenceIntegrityException("无法删除：该作者被引用", references);
        }
        
        // 强制删除：清理引用
        if (force && hasReferences) {
            log.warn("强制删除作者，清理引用: id={}, name={}", id, author.getName());
            
            // 清理角色引用（将作者ID设为null或删除角色）
            // 注意：这里假设角色的作者字段可以为空，如果不能为空则需要删除角色
            // 当前实现：不做处理，因为角色的workId字段是必需的
            
            // 清理配置引用
            for (TagFilterConfig config : referencedConfigs) {
                config.getAuthorIds().remove(id);
                tagFilterConfigRepository.save(config);
            }
        }
        
        // 删除作者
        authorRepository.deleteById(id);
        
        // 发布作者标签变更事件
        eventPublisher.publishEvent(
            TagConfigurationEvent.authorChanged(this, id)
        );
        
        log.info("删除作者成功: id={}, name={}", id, author.getName());
    }
    
    @Override
    public AuthorVO getById(String id) {
        Author author = authorRepository.findById(id)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "作者不存在: " + id
            ));
        return toVO(author);
    }
    
    @Override
    public AuthorVO getByName(String name) {
        Author author = authorRepository.findByName(name)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "作者不存在: " + name
            ));
        return toVO(author);
    }
    
    @Override
    public AuthorVO getByAlias(String alias) {
        Author author = authorRepository.findByAliasesContaining(alias)
            .orElseThrow(() -> new BusinessException(
                ResponseCode.DATA_NOT_FOUND, 
                "作者不存在（别名）: " + alias
            ));
        return toVO(author);
    }
    
    @Override
    public PageResponse<AuthorVO> page(Long current, Long size, AuthorQueryDTO query) {
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
        List<Author> authors = mongoTemplate.find(mongoQuery, Author.class);
        long total = mongoTemplate.count(Query.of(mongoQuery).limit(-1).skip(-1), Author.class);
        
        // 转换为VO，并标记匹配字段
        final String searchKeyword = keyword;
        List<AuthorVO> records = authors.stream()
            .map(author -> toVOWithMatchedField(author, searchKeyword))
            .collect(Collectors.toList());
        
        return PageResponse.success(records, current, size, total);
    }
    
    @Override
    @Transactional
    public ImportResultVO importFromJson(String json) {
        int successCount = 0;
        int failureCount = 0;
        List<ImportResultVO.ImportError> errors = new ArrayList<>();
        
        try {
            List<Author> authors = objectMapper.readValue(json, new TypeReference<List<Author>>() {});
            
            for (int i = 0; i < authors.size(); i++) {
                Author author = authors.get(i);
                
                // 验证唯一性
                try {
                    uniquenessValidationService.validateNameUniqueness(
                        author.getName(), 
                        null, 
                        EntityType.AUTHOR
                    );
                    
                    if (author.getAliases() != null && !author.getAliases().isEmpty()) {
                        uniquenessValidationService.validateAliasUniqueness(
                            author.getAliases(), 
                            null, 
                            EntityType.AUTHOR
                        );
                    }
                    
                    // 设置时间
                    author.setId(null); // 清除ID，让MongoDB生成新ID
                    author.setCreateTime(LocalDateTime.now());
                    author.setUpdateTime(LocalDateTime.now());
                    
                    // 保存
                    authorRepository.save(author);
                    successCount++;
                    
                    log.info("导入作者成功: name={}", author.getName());
                } catch (BusinessException e) {
                    failureCount++;
                    errors.add(ImportResultVO.ImportError.builder()
                        .index(i)
                        .name(author.getName())
                        .error(e.getMessage())
                        .build());
                    log.warn("跳过冲突的作者: name={}, reason={}", author.getName(), e.getMessage());
                }
            }
            
            // 批量导入完成后，发布重新加载事件
            eventPublisher.publishEvent(
                TagConfigurationEvent.reloadAll(this)
            );
            
            log.info("批量导入作者完成，成功: {}, 失败: {}, 已发布标签配置重新加载事件", successCount, failureCount);
            
            return ImportResultVO.builder()
                .successCount(successCount)
                .failureCount(failureCount)
                .errors(errors)
                .build();
                
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
            List<Author> authors = authorRepository.findAll();
            return objectMapper.writeValueAsString(authors);
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
    private AuthorVO toVO(Author author) {
        AuthorVO vo = new AuthorVO();
        vo.setId(author.getId());
        vo.setName(author.getName());
        vo.setAliases(author.getAliases());
        vo.setSignature(author.getSignature());
        vo.setUrls(author.getUrls());
        vo.setAvatarBase64(author.getAvatarBase64());
        vo.setRemark(author.getRemark());
        vo.setCreateTime(author.getCreateTime());
        vo.setUpdateTime(author.getUpdateTime());
        return vo;
    }
    
    /**
     * 实体转VO，并标记匹配字段
     */
    private AuthorVO toVOWithMatchedField(Author author, String keyword) {
        AuthorVO vo = toVO(author);
        
        if (keyword != null && !keyword.isEmpty()) {
            // 检查名称是否匹配
            if (author.getName() != null && 
                author.getName().toLowerCase().contains(keyword.toLowerCase())) {
                vo.setMatchedField("name");
            } 
            // 检查别名是否匹配
            else if (author.getAliases() != null) {
                for (String alias : author.getAliases()) {
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
