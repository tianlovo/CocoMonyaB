package org.xlyo.cocomonyab.plugin.tagforward.component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.xlyo.cocomonyab.plugin.tagforward.config.TagBasedForwardingProperties;
import org.xlyo.cocomonyab.plugin.tagforward.model.TagEntity;
import org.xlyo.cocomonyab.plugin.tagforward.model.TagFilterConfig;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TagMatcher单元测试
 * 
 * 使用Testcontainers启动MongoDB进行集成测试
 */
@SpringBootTest
@Testcontainers
@DisplayName("TagMatcher Unit Tests")
class TagMatcherTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("plugin.tag-based-forwarding.target-channel-id", () -> "-1001234567890");
        registry.add("plugin.tag-based-forwarding.tag-prefix", () -> "#");
    }
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private TagBasedForwardingProperties properties;
    
    private TagMatcher tagMatcher;
    
    @BeforeEach
    void setUp() {
        // 清空集合
        mongoTemplate.dropCollection("tag_filter_configs_v2");
        mongoTemplate.dropCollection("tag_authors");
        mongoTemplate.dropCollection("tag_characters");
        mongoTemplate.dropCollection("tag_works");
        
        // 创建TagMatcher实例
        tagMatcher = new TagMatcher(mongoTemplate, properties);
    }
    
    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection("tag_filter_configs_v2");
        mongoTemplate.dropCollection("tag_authors");
        mongoTemplate.dropCollection("tag_characters");
        mongoTemplate.dropCollection("tag_works");
    }
    
    @Test
    @DisplayName("Should handle empty tag configuration")
    void testLoadTagConfiguration_shouldHandleEmptyConfiguration() {
        // Given - 没有任何配置
        
        // When
        tagMatcher.loadTagConfiguration();
        
        // Then
        Set<String> expandedTags = tagMatcher.getExpandedTagList();
        assertThat(expandedTags).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle empty message text")
    void testMatchTags_shouldHandleEmptyMessageText() {
        // Given
        tagMatcher.loadTagConfiguration();
        
        // When & Then
        assertThat(tagMatcher.matchTags(null)).isEmpty();
        assertThat(tagMatcher.matchTags("")).isEmpty();
    }
    
    @Test
    @DisplayName("Should add tag prefix correctly")
    void testLoadTagConfiguration_shouldAddTagPrefix() {
        // Given - 创建标签实体
        TagEntity author = TagEntity.builder()
                .id("author1")
                .name("AuthorName")
                .aliases(Arrays.asList("AuthorAlias"))
                .build();
        mongoTemplate.insert(author, "tag_authors");
        
        // 创建标签配置
        TagFilterConfig config = TagFilterConfig.builder()
                .id("config1")
                .enabled(true)
                .authorIds(Arrays.asList("author1"))
                .build();
        mongoTemplate.insert(config, "tag_filter_configs_v2");
        
        // When
        tagMatcher.loadTagConfiguration();
        
        // Then
        Set<String> expandedTags = tagMatcher.getExpandedTagList();
        assertThat(expandedTags).containsExactlyInAnyOrder("#AuthorName", "#AuthorAlias");
    }
    
    @Test
    @DisplayName("Should perform case-insensitive tag matching")
    void testMatchTags_shouldBeCaseInsensitive() {
        // Given - 创建标签实体
        TagEntity author = TagEntity.builder()
                .id("author1")
                .name("Test")
                .build();
        mongoTemplate.insert(author, "tag_authors");
        
        TagFilterConfig config = TagFilterConfig.builder()
                .id("config1")
                .enabled(true)
                .authorIds(Arrays.asList("author1"))
                .build();
        mongoTemplate.insert(config, "tag_filter_configs_v2");
        
        tagMatcher.loadTagConfiguration();
        
        // When & Then - 测试不同大小写组合
        assertThat(tagMatcher.matchTags("This message contains #test tag")).isNotEmpty();
        assertThat(tagMatcher.matchTags("This message contains #TEST tag")).isNotEmpty();
        assertThat(tagMatcher.matchTags("This message contains #Test tag")).isNotEmpty();
        assertThat(tagMatcher.matchTags("This message contains #TeSt tag")).isNotEmpty();
    }
    
    @Test
    @DisplayName("Should handle whitespace in tags correctly")
    void testLoadTagConfiguration_shouldHandleWhitespace() {
        // Given - 创建带空白字符的标签
        TagEntity author1 = TagEntity.builder()
                .id("author1")
                .name("  ValidTag  ")  // 前后有空格
                .build();
        
        TagEntity author2 = TagEntity.builder()
                .id("author2")
                .name("   ")  // 仅空白字符
                .build();
        
        TagEntity author3 = TagEntity.builder()
                .id("author3")
                .name("")  // 空字符串
                .build();
        
        mongoTemplate.insert(author1, "tag_authors");
        mongoTemplate.insert(author2, "tag_authors");
        mongoTemplate.insert(author3, "tag_authors");
        
        TagFilterConfig config = TagFilterConfig.builder()
                .id("config1")
                .enabled(true)
                .authorIds(Arrays.asList("author1", "author2", "author3"))
                .build();
        mongoTemplate.insert(config, "tag_filter_configs_v2");
        
        // When
        tagMatcher.loadTagConfiguration();
        
        // Then - 应该只包含有效标签（去除空白后非空）
        Set<String> expandedTags = tagMatcher.getExpandedTagList();
        assertThat(expandedTags).containsExactly("#ValidTag");
    }
    
    @Test
    @DisplayName("Should load tags from all entity types")
    void testLoadTagConfiguration_shouldLoadAllEntityTypes() {
        // Given - 创建不同类型的标签实体
        TagEntity author = TagEntity.builder()
                .id("author1")
                .name("Author1")
                .aliases(Arrays.asList("AuthorAlias1"))
                .build();
        mongoTemplate.insert(author, "tag_authors");
        
        TagEntity character = TagEntity.builder()
                .id("char1")
                .name("Character1")
                .aliases(Arrays.asList("CharAlias1"))
                .build();
        mongoTemplate.insert(character, "tag_characters");
        
        TagEntity work = TagEntity.builder()
                .id("work1")
                .name("Work1")
                .aliases(Arrays.asList("WorkAlias1"))
                .build();
        mongoTemplate.insert(work, "tag_works");
        
        // 创建包含所有类型的配置
        Map<String, String> customTags = new HashMap<>();
        customTags.put("key1", "CustomTag1");
        
        TagFilterConfig config = TagFilterConfig.builder()
                .id("config1")
                .enabled(true)
                .authorIds(Arrays.asList("author1"))
                .characterIds(Arrays.asList("char1"))
                .workIds(Arrays.asList("work1"))
                .customTags(customTags)
                .build();
        mongoTemplate.insert(config, "tag_filter_configs_v2");
        
        // When
        tagMatcher.loadTagConfiguration();
        
        // Then
        Set<String> expandedTags = tagMatcher.getExpandedTagList();
        assertThat(expandedTags).containsExactlyInAnyOrder(
                "#Author1", "#AuthorAlias1",
                "#Character1", "#CharAlias1",
                "#Work1", "#WorkAlias1",
                "#CustomTag1"
        );
    }
    
    @Test
    @DisplayName("Should only load enabled configurations")
    void testLoadTagConfiguration_shouldOnlyLoadEnabledConfigs() {
        // Given - 创建启用和禁用的配置
        TagEntity author1 = TagEntity.builder()
                .id("author1")
                .name("EnabledTag")
                .build();
        mongoTemplate.insert(author1, "tag_authors");
        
        TagEntity author2 = TagEntity.builder()
                .id("author2")
                .name("DisabledTag")
                .build();
        mongoTemplate.insert(author2, "tag_authors");
        
        TagFilterConfig enabledConfig = TagFilterConfig.builder()
                .id("config1")
                .enabled(true)
                .authorIds(Arrays.asList("author1"))
                .build();
        mongoTemplate.insert(enabledConfig, "tag_filter_configs_v2");
        
        TagFilterConfig disabledConfig = TagFilterConfig.builder()
                .id("config2")
                .enabled(false)
                .authorIds(Arrays.asList("author2"))
                .build();
        mongoTemplate.insert(disabledConfig, "tag_filter_configs_v2");
        
        // When
        tagMatcher.loadTagConfiguration();
        
        // Then - 应该只包含启用配置的标签
        Set<String> expandedTags = tagMatcher.getExpandedTagList();
        assertThat(expandedTags).containsExactly("#EnabledTag");
    }
    
    @Test
    @DisplayName("Should match multiple tags in message")
    void testMatchTags_shouldMatchMultipleTags() {
        // Given
        TagEntity author1 = TagEntity.builder()
                .id("author1")
                .name("Tag1")
                .build();
        TagEntity author2 = TagEntity.builder()
                .id("author2")
                .name("Tag2")
                .build();
        mongoTemplate.insert(author1, "tag_authors");
        mongoTemplate.insert(author2, "tag_authors");
        
        TagFilterConfig config = TagFilterConfig.builder()
                .id("config1")
                .enabled(true)
                .authorIds(Arrays.asList("author1", "author2"))
                .build();
        mongoTemplate.insert(config, "tag_filter_configs_v2");
        
        tagMatcher.loadTagConfiguration();
        
        // When
        List<String> matchedTags = tagMatcher.matchTags("This message has #Tag1 and #Tag2");
        
        // Then
        assertThat(matchedTags).hasSize(2);
        assertThat(matchedTags).containsExactlyInAnyOrder("#Tag1", "#Tag2");
    }
    
    @Test
    @DisplayName("Should return empty list when no tags match")
    void testMatchTags_shouldReturnEmptyWhenNoMatch() {
        // Given
        TagEntity author = TagEntity.builder()
                .id("author1")
                .name("SpecificTag")
                .build();
        mongoTemplate.insert(author, "tag_authors");
        
        TagFilterConfig config = TagFilterConfig.builder()
                .id("config1")
                .enabled(true)
                .authorIds(Arrays.asList("author1"))
                .build();
        mongoTemplate.insert(config, "tag_filter_configs_v2");
        
        tagMatcher.loadTagConfiguration();
        
        // When
        List<String> matchedTags = tagMatcher.matchTags("This message has no matching tags");
        
        // Then
        assertThat(matchedTags).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle tags with null aliases")
    void testLoadTagConfiguration_shouldHandleNullAliases() {
        // Given
        TagEntity author = TagEntity.builder()
                .id("author1")
                .name("TagName")
                .aliases(null)  // null aliases
                .build();
        mongoTemplate.insert(author, "tag_authors");
        
        TagFilterConfig config = TagFilterConfig.builder()
                .id("config1")
                .enabled(true)
                .authorIds(Arrays.asList("author1"))
                .build();
        mongoTemplate.insert(config, "tag_filter_configs_v2");
        
        // When
        tagMatcher.loadTagConfiguration();
        
        // Then - 应该只包含名称，不会因为null aliases而失败
        Set<String> expandedTags = tagMatcher.getExpandedTagList();
        assertThat(expandedTags).containsExactly("#TagName");
    }
}
