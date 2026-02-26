package org.xlyo.cocomonyab.plugin.tagforward.component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.xlyo.cocomonyab.event.TagConfigurationEvent;
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
 * 
 * <p>动态更新支持：
 * <ul>
 *   <li>监听 TagConfigurationEvent 事件，自动更新缓存</li>
 *   <li>支持增量更新（标签过滤配置、作者、角色、作品变更）</li>
 *   <li>支持全量重载（重新加载所有标签配置）</li>
 *   <li>线程安全：使用 volatile 保证并发安全</li>
 * </ul>
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

    @Getter
    private volatile boolean configurationLoaded = false;
    
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
            log.info("正在加载标签配置...");
            
            // 查询启用的标签配置
            Query query = Query.query(Criteria.where("enabled").is(true));
            List<TagFilterConfig> configs = mongoTemplate.find(query, TagFilterConfig.class, "tag_filter_configs_v2");
            
            log.debug("找到 {} 个已启用的标签过滤配置", configs.size());
            
            Set<String> tags = new HashSet<>();
            
            for (TagFilterConfig config : configs) {
                // 加载作者标签
                if (config.getAuthorIds() != null && !config.getAuthorIds().isEmpty()) {
                    Set<String> authorTags = loadTagEntities("tag_authors", config.getAuthorIds());
                    tags.addAll(authorTags);
                    log.debug("从配置 {} 加载了 {} 个作者标签", config.getId(), authorTags.size());
                }
                
                // 加载角色标签
                if (config.getCharacterIds() != null && !config.getCharacterIds().isEmpty()) {
                    Set<String> characterTags = loadTagEntities("tag_characters", config.getCharacterIds());
                    tags.addAll(characterTags);
                    log.debug("从配置 {} 加载了 {} 个角色标签", config.getId(), characterTags.size());
                }
                
                // 加载原作标签
                if (config.getWorkIds() != null && !config.getWorkIds().isEmpty()) {
                    Set<String> workTags = loadTagEntities("tag_works", config.getWorkIds());
                    tags.addAll(workTags);
                    log.debug("从配置 {} 加载了 {} 个原作标签", config.getId(), workTags.size());
                }
                
                // 加载自定义标签
                if (config.getCustomTags() != null && !config.getCustomTags().isEmpty()) {
                    Set<String> customTags = new HashSet<>(config.getCustomTags().values());
                    tags.addAll(customTags);
                    log.debug("从配置 {} 加载了 {} 个自定义标签", config.getId(), customTags.size());
                }
            }
            
            // 添加前缀并过滤空标签
            String prefix = properties.getTagPrefix();
            this.expandedTagList = tags.stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(tag -> prefix + tag)
                .collect(Collectors.toSet());
            
            this.configurationLoaded = true;
            
            log.info("已加载 {} 个标签，前缀为 '{}'", expandedTagList.size(), prefix);
            
        } catch (Exception e) {
            log.error("加载标签配置失败", e);
            // 使用空标签列表继续运行
            this.expandedTagList = new HashSet<>();
            this.configurationLoaded = false;
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
            log.error("从集合 {} 加载标签实体失败", collection, e);
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
        // 如果配置尚未加载，返回空列表
        if (!configurationLoaded) {
            log.debug("标签配置尚未加载，跳过匹配");
            return Collections.emptyList();
        }
        
        // 处理null和空字符串的边缘情况
        if (textContent == null || textContent.isEmpty()) {
            log.debug("文本内容为 null 或空，未匹配到标签");
            return Collections.emptyList();
        }
        
        // 转换为小写进行大小写不敏感匹配
        String lowerTextContent = textContent.toLowerCase();
        
        // 过滤出所有匹配的标签
        List<String> matchedTags = expandedTagList.stream()
            .filter(tag -> lowerTextContent.contains(tag.toLowerCase()))
            .collect(Collectors.toList());
        
        if (!matchedTags.isEmpty()) {
            log.debug("在文本内容中匹配到 {} 个标签", matchedTags.size());
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
    
    /**
     * 监听标签配置事件，动态更新缓存
     * 
     * @param event 标签配置事件
     */
    @EventListener
    public void handleTagConfigurationEvent(TagConfigurationEvent event) {
        log.info("收到标签配置事件: {}", event);
        
        switch (event.getEventType()) {
            case TAG_FILTER_ADDED:
            case TAG_FILTER_REMOVED:
            case TAG_FILTER_UPDATED:
                // 标签过滤配置变更，重新加载所有标签
                handleReloadAll("标签过滤配置变更");
                break;
                
            case AUTHOR_CHANGED:
                // 作者标签变更，重新加载所有标签
                handleReloadAll("作者标签变更: " + event.getEntityId());
                break;
                
            case CHARACTER_CHANGED:
                // 角色标签变更，重新加载所有标签
                handleReloadAll("角色标签变更: " + event.getEntityId());
                break;
                
            case WORK_CHANGED:
                // 作品标签变更，重新加载所有标签
                handleReloadAll("作品标签变更: " + event.getEntityId());
                break;
                
            case RELOAD_ALL:
                // 全量重载
                handleReloadAll("手动触发重新加载");
                break;
                
            default:
                log.warn("未知的事件类型: {}", event.getEventType());
        }
    }
    
    /**
     * 处理重新加载所有标签配置事件
     * 
     * @param reason 重新加载的原因
     */
    private void handleReloadAll(String reason) {
        log.info("开始重新加载标签配置，原因: {}", reason);
        
        int oldSize = expandedTagList.size();
        loadTagConfiguration();
        int newSize = expandedTagList.size();
        
        log.info("✓ 标签配置已重新加载，标签数量: {} -> {}", oldSize, newSize);
    }
}
