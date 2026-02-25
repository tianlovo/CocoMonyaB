package org.xlyo.cocomonyab.plugin.tagforward.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.plugin.tagforward.config.TagBasedForwardingProperties;
import org.xlyo.cocomonyab.plugin.tagforward.model.TagEntity;
import org.xlyo.cocomonyab.plugin.tagforward.model.TagFilterConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 标签匹配器组件
 * 
 * <p>负责从MongoDB标签系统加载标签配置，维护展开的标签列表，
 * 并对消息文本执行大小写不敏感的标签匹配
 */
@Component
@Slf4j
public class TagMatcher {
    
    private final MongoTemplate mongoTemplate;
    private final TagBasedForwardingProperties properties;
    
    /**
     * 展开的标签列表（包含前缀）
     * 使用volatile确保多线程可见性
     */
    private volatile Set<String> expandedTagList = new HashSet<>();
    
    public TagMatcher(MongoTemplate mongoTemplate, TagBasedForwardingProperties properties) {
        this.mongoTemplate = mongoTemplate;
        this.properties = properties;
    }
    
    /**
     * 加载标签配置
     * 
     * <p>从tag_filter_configs_v2集合查询所有启用的配置，
     * 加载相关的标签实体（作者、角色、原作），
     * 收集所有标签名称和别名，添加前缀，并过滤空白标签
     */
    public void loadTagConfiguration() {
        try {
            log.info("Loading tag configuration...");
            
            // 查询启用的标签配置
            Query query = Query.query(Criteria.where("enabled").is(true));
            List<TagFilterConfig> configs = mongoTemplate.find(query, TagFilterConfig.class, "tag_filter_configs_v2");
            
            log.debug("Found {} enabled tag filter configs", configs.size());
            
            Set<String> tags = new HashSet<>();
            
            for (TagFilterConfig config : configs) {
                // 加载作者标签
                if (config.getAuthorIds() != null && !config.getAuthorIds().isEmpty()) {
                    Set<String> authorTags = loadTagEntities("tag_authors", config.getAuthorIds());
                    tags.addAll(authorTags);
                    log.debug("Loaded {} author tags from config {}", authorTags.size(), config.getId());
                }
                
                // 加载角色标签
                if (config.getCharacterIds() != null && !config.getCharacterIds().isEmpty()) {
                    Set<String> characterTags = loadTagEntities("tag_characters", config.getCharacterIds());
                    tags.addAll(characterTags);
                    log.debug("Loaded {} character tags from config {}", characterTags.size(), config.getId());
                }
                
                // 加载原作标签
                if (config.getWorkIds() != null && !config.getWorkIds().isEmpty()) {
                    Set<String> workTags = loadTagEntities("tag_works", config.getWorkIds());
                    tags.addAll(workTags);
                    log.debug("Loaded {} work tags from config {}", workTags.size(), config.getId());
                }
                
                // 加载自定义标签
                if (config.getCustomTags() != null && !config.getCustomTags().isEmpty()) {
                    Set<String> customTags = new HashSet<>(config.getCustomTags().values());
                    tags.addAll(customTags);
                    log.debug("Loaded {} custom tags from config {}", customTags.size(), config.getId());
                }
            }
            
            // 添加前缀并过滤空标签
            String prefix = properties.getTagPrefix();
            this.expandedTagList = tags.stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(tag -> prefix + tag)
                .collect(Collectors.toSet());
            
            log.info("Loaded {} tags with prefix '{}'", expandedTagList.size(), prefix);
            
        } catch (Exception e) {
            log.error("Failed to load tag configuration", e);
            // 使用空标签列表继续运行
            this.expandedTagList = new HashSet<>();
        }
    }
    
    /**
     * 从指定集合加载标签实体
     * 
     * @param collection 集合名称（tag_authors、tag_characters或tag_works）
     * @param ids 要查询的实体ID列表
     * @return 包含所有标签名称和别名的集合
     */
    private Set<String> loadTagEntities(String collection, List<String> ids) {
        try {
            Query query = Query.query(Criteria.where("_id").in(ids));
            List<TagEntity> entities = mongoTemplate.find(query, TagEntity.class, collection);
            
            Set<String> tags = new HashSet<>();
            for (TagEntity entity : entities) {
                // 添加标签名称
                if (entity.getName() != null && !entity.getName().trim().isEmpty()) {
                    tags.add(entity.getName());
                }
                
                // 添加标签别名
                if (entity.getAliases() != null) {
                    for (String alias : entity.getAliases()) {
                        if (alias != null && !alias.trim().isEmpty()) {
                            tags.add(alias);
                        }
                    }
                }
            }
            
            return tags;
            
        } catch (Exception e) {
            log.error("Failed to load tag entities from collection {}", collection, e);
            return new HashSet<>();
        }
    }
    
    /**
     * 匹配消息文本中的标签
     * 
     * <p>执行大小写不敏感的字符串包含匹配，
     * 返回所有在消息文本中找到的标签列表
     * 
     * @param textContent 消息文本内容
     * @return 匹配到的标签列表（如果没有匹配则返回空列表）
     */
    public List<String> matchTags(String textContent) {
        // 处理null和空字符串的边缘情况
        if (textContent == null || textContent.isEmpty()) {
            log.debug("Text content is null or empty, no tags matched");
            return Collections.emptyList();
        }
        
        // 转换为小写进行大小写不敏感匹配
        String lowerTextContent = textContent.toLowerCase();
        
        // 过滤出所有匹配的标签
        List<String> matchedTags = expandedTagList.stream()
            .filter(tag -> lowerTextContent.contains(tag.toLowerCase()))
            .collect(Collectors.toList());
        
        if (!matchedTags.isEmpty()) {
            log.debug("Matched {} tags in text content", matchedTags.size());
        }
        
        return matchedTags;
    }
    
    /**
     * 获取当前展开的标签列表（用于测试）
     * 
     * @return 展开的标签列表的不可变副本
     */
    public Set<String> getExpandedTagList() {
        return Collections.unmodifiableSet(expandedTagList);
    }
}
