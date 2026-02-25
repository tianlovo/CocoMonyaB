package org.xlyo.cocomonyab.plugin.tagforward.component;

import net.jqwik.api.*;
import net.jqwik.api.constraints.Size;
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
 * Property-based tests for TagMatcher
 * 
 * **Validates: Requirements 1.1, 1.7, 2.3, 2.4, 2.5, 2.6, 3.5, 3.6**
 */
@SpringBootTest
@Testcontainers
class TagMatcherPropertyTest {
    
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
    
    /**
     * 属性 1: 标签加载完整性
     * 
     * 对于任何启用的标签配置，加载后的展开标签列表应该包含所有标签实体的名称和别名（添加了前缀）
     * 
     * **Validates: Requirements 1.1, 1.7**
     */
    @Property(tries = 100)
    @Label("Feature: tag-based-message-forwarding, Property 1: Tag loading completeness")
    void tagLoadingCompleteness(
            @ForAll("tagEntities") @Size(min = 1, max = 10) List<TagEntity> authors,
            @ForAll("tagEntities") @Size(min = 1, max = 10) List<TagEntity> characters,
            @ForAll("tagEntities") @Size(min = 1, max = 10) List<TagEntity> works,
            @ForAll("customTags") @Size(min = 1, max = 5) Map<String, String> customTags) {
        
        // Given - 清空集合
        mongoTemplate.dropCollection("tag_filter_configs_v2");
        mongoTemplate.dropCollection("tag_authors");
        mongoTemplate.dropCollection("tag_characters");
        mongoTemplate.dropCollection("tag_works");
        
        // 插入标签实体
        List<String> authorIds = new ArrayList<>();
        for (TagEntity author : authors) {
            mongoTemplate.insert(author, "tag_authors");
            authorIds.add(author.getId());
        }
        
        List<String> characterIds = new ArrayList<>();
        for (TagEntity character : characters) {
            mongoTemplate.insert(character, "tag_characters");
            characterIds.add(character.getId());
        }
        
        List<String> workIds = new ArrayList<>();
        for (TagEntity work : works) {
            mongoTemplate.insert(work, "tag_works");
            workIds.add(work.getId());
        }
        
        // 创建启用的标签配置
        TagFilterConfig config = TagFilterConfig.builder()
                .id(UUID.randomUUID().toString())
                .enabled(true)
                .authorIds(authorIds)
                .characterIds(characterIds)
                .workIds(workIds)
                .customTags(customTags)
                .build();
        mongoTemplate.insert(config, "tag_filter_configs_v2");
        
        // 收集所有预期的标签（带前缀）
        Set<String> expectedTags = new HashSet<>();
        String prefix = properties.getTagPrefix();
        
        for (TagEntity entity : authors) {
            if (entity.getName() != null && !entity.getName().trim().isEmpty()) {
                expectedTags.add(prefix + entity.getName().trim());
            }
            if (entity.getAliases() != null) {
                for (String alias : entity.getAliases()) {
                    if (alias != null && !alias.trim().isEmpty()) {
                        expectedTags.add(prefix + alias.trim());
                    }
                }
            }
        }
        
        for (TagEntity entity : characters) {
            if (entity.getName() != null && !entity.getName().trim().isEmpty()) {
                expectedTags.add(prefix + entity.getName().trim());
            }
            if (entity.getAliases() != null) {
                for (String alias : entity.getAliases()) {
                    if (alias != null && !alias.trim().isEmpty()) {
                        expectedTags.add(prefix + alias.trim());
                    }
                }
            }
        }
        
        for (TagEntity entity : works) {
            if (entity.getName() != null && !entity.getName().trim().isEmpty()) {
                expectedTags.add(prefix + entity.getName().trim());
            }
            if (entity.getAliases() != null) {
                for (String alias : entity.getAliases()) {
                    if (alias != null && !alias.trim().isEmpty()) {
                        expectedTags.add(prefix + alias.trim());
                    }
                }
            }
        }
        
        for (String customTag : customTags.values()) {
            if (customTag != null && !customTag.trim().isEmpty()) {
                expectedTags.add(prefix + customTag.trim());
            }
        }
        
        // When - 加载标签配置
        TagMatcher tagMatcher = new TagMatcher(mongoTemplate, properties);
        tagMatcher.loadTagConfiguration();
        
        // Then - 展开的标签列表应该包含所有预期的标签
        Set<String> actualTags = tagMatcher.getExpandedTagList();
        assertThat(actualTags)
                .as("Expanded tag list should contain all tag names and aliases with prefix")
                .containsExactlyInAnyOrderElementsOf(expectedTags);
    }
    
    /**
     * 属性 2: 标签前缀处理
     * 
     * 对于任何标签字符串，处理后的标签应该：
     * (1) 以配置的前缀开头
     * (2) 保留原始大小写
     * (3) 去除首尾空白
     * (4) 如果原始字符串为空或仅包含空白则被过滤掉
     * 
     * **Validates: Requirements 2.3, 2.4, 2.5, 2.6**
     */
    @Property(tries = 100)
    @Label("Feature: tag-based-message-forwarding, Property 2: Tag prefix processing")
    void tagPrefixProcessing(
            @ForAll("tagNameWithWhitespace") String tagName) {
        
        // Given - 清空集合
        mongoTemplate.dropCollection("tag_filter_configs_v2");
        mongoTemplate.dropCollection("tag_authors");
        
        // 创建标签实体
        TagEntity author = TagEntity.builder()
                .id(UUID.randomUUID().toString())
                .name(tagName)
                .aliases(null)
                .build();
        mongoTemplate.insert(author, "tag_authors");
        
        // 创建标签配置
        TagFilterConfig config = TagFilterConfig.builder()
                .id(UUID.randomUUID().toString())
                .enabled(true)
                .authorIds(Arrays.asList(author.getId()))
                .build();
        mongoTemplate.insert(config, "tag_filter_configs_v2");
        
        // When - 加载标签配置
        TagMatcher tagMatcher = new TagMatcher(mongoTemplate, properties);
        tagMatcher.loadTagConfiguration();
        
        // Then - 验证标签前缀处理
        Set<String> expandedTags = tagMatcher.getExpandedTagList();
        String prefix = properties.getTagPrefix();
        String trimmedTag = tagName.trim();
        
        if (trimmedTag.isEmpty()) {
            // (4) 空白标签应该被过滤掉
            assertThat(expandedTags)
                    .as("Empty or whitespace-only tags should be filtered out")
                    .isEmpty();
        } else {
            // (1) 应该以配置的前缀开头
            assertThat(expandedTags).hasSize(1);
            String processedTag = expandedTags.iterator().next();
            assertThat(processedTag)
                    .as("Tag should start with configured prefix")
                    .startsWith(prefix);
            
            // (2) 保留原始大小写
            assertThat(processedTag)
                    .as("Tag should preserve original case")
                    .isEqualTo(prefix + trimmedTag);
            
            // (3) 去除首尾空白
            assertThat(processedTag)
                    .as("Tag should have whitespace trimmed")
                    .doesNotStartWith(prefix + " ")
                    .doesNotEndWith(" ");
        }
    }
    
    /**
     * 属性 3: 大小写不敏感的标签匹配
     * 
     * 对于任何消息文本和标签列表，如果消息文本包含至少一个标签（大小写不敏感匹配），
     * 则消息应该被标记为感兴趣的消息
     * 
     * **Validates: Requirements 3.5, 3.6**
     */
    @Property(tries = 100)
    @Label("Feature: tag-based-message-forwarding, Property 3: Case-insensitive tag matching")
    void caseInsensitiveTagMatching(
            @ForAll("validTagName") String tagName,
            @ForAll("caseVariation") CaseVariation caseVariation) {
        
        // Given - 清空集合并创建标签
        mongoTemplate.dropCollection("tag_filter_configs_v2");
        mongoTemplate.dropCollection("tag_authors");
        
        TagEntity author = TagEntity.builder()
                .id(UUID.randomUUID().toString())
                .name(tagName)
                .aliases(null)
                .build();
        mongoTemplate.insert(author, "tag_authors");
        
        TagFilterConfig config = TagFilterConfig.builder()
                .id(UUID.randomUUID().toString())
                .enabled(true)
                .authorIds(Arrays.asList(author.getId()))
                .build();
        mongoTemplate.insert(config, "tag_filter_configs_v2");
        
        TagMatcher tagMatcher = new TagMatcher(mongoTemplate, properties);
        tagMatcher.loadTagConfiguration();
        
        // 构造消息文本，包含不同大小写变体的标签
        String prefix = properties.getTagPrefix();
        String tagInMessage = applyCaseVariation(prefix + tagName, caseVariation);
        String messageText = "This is a test message with " + tagInMessage + " in it";
        
        // When - 匹配标签
        List<String> matchedTags = tagMatcher.matchTags(messageText);
        
        // Then - 应该匹配到标签（大小写不敏感）
        assertThat(matchedTags)
                .as("Should match tag regardless of case variation")
                .isNotEmpty()
                .hasSize(1);
        
        // 匹配到的标签应该是原始标签（保留原始大小写）
        assertThat(matchedTags.get(0))
                .as("Matched tag should preserve original case from configuration")
                .isEqualTo(prefix + tagName);
    }
    
    /**
     * 属性: 空消息文本处理
     * 
     * 对于null或空字符串的消息文本，应该返回空列表
     */
    @Property(tries = 50)
    @Label("Feature: tag-based-message-forwarding, Property: Empty message text handling")
    void emptyMessageTextHandling(
            @ForAll("validTagName") String tagName) {
        
        // Given - 创建标签配置
        mongoTemplate.dropCollection("tag_filter_configs_v2");
        mongoTemplate.dropCollection("tag_authors");
        
        TagEntity author = TagEntity.builder()
                .id(UUID.randomUUID().toString())
                .name(tagName)
                .build();
        mongoTemplate.insert(author, "tag_authors");
        
        TagFilterConfig config = TagFilterConfig.builder()
                .id(UUID.randomUUID().toString())
                .enabled(true)
                .authorIds(Arrays.asList(author.getId()))
                .build();
        mongoTemplate.insert(config, "tag_filter_configs_v2");
        
        TagMatcher tagMatcher = new TagMatcher(mongoTemplate, properties);
        tagMatcher.loadTagConfiguration();
        
        // When & Then - null和空字符串应该返回空列表
        assertThat(tagMatcher.matchTags(null))
                .as("Null text should return empty list")
                .isEmpty();
        
        assertThat(tagMatcher.matchTags(""))
                .as("Empty text should return empty list")
                .isEmpty();
    }
    
    // Arbitraries for generating test data
    
    @Provide
    Arbitrary<TagEntity> tagEntities() {
        Arbitrary<List<String>> aliasesArbitrary = Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                        .list().ofMinSize(0).ofMaxSize(3)
        );
        
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
                aliasesArbitrary
        ).as((name, aliases) ->
                TagEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .name(name)
                        .aliases(aliases)
                        .build()
        );
    }
    
    @Provide
    Arbitrary<Map<String, String>> customTags() {
        return Arbitraries.maps(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
        );
    }
    
    @Provide
    Arbitrary<String> tagNameWithWhitespace() {
        return Arbitraries.oneOf(
                // 正常标签
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
                // 带前导空格
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                        .map(s -> "  " + s),
                // 带尾随空格
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                        .map(s -> s + "  "),
                // 带前后空格
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                        .map(s -> "  " + s + "  "),
                // 仅空格
                Arbitraries.just("   "),
                // 空字符串
                Arbitraries.just("")
        );
    }
    
    @Provide
    Arbitrary<String> validTagName() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
    }
    
    @Provide
    Arbitrary<CaseVariation> caseVariation() {
        return Arbitraries.of(CaseVariation.values());
    }
    
    // Helper enum for case variations
    enum CaseVariation {
        LOWER, UPPER, MIXED, ORIGINAL
    }
    
    // Helper method to apply case variation
    private String applyCaseVariation(String text, CaseVariation variation) {
        switch (variation) {
            case LOWER:
                return text.toLowerCase();
            case UPPER:
                return text.toUpperCase();
            case MIXED:
                // Alternate between upper and lower case
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < text.length(); i++) {
                    char c = text.charAt(i);
                    sb.append(i % 2 == 0 ? Character.toUpperCase(c) : Character.toLowerCase(c));
                }
                return sb.toString();
            case ORIGINAL:
            default:
                return text;
        }
    }
}
