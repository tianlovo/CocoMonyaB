package org.xlyo.cocomonyab.service.tag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.domain.dto.tag.TagFilterConfigCreateDTO;
import org.xlyo.cocomonyab.domain.dto.tag.TagFilterConfigUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.tag.TagFilterConfigVO;
import org.xlyo.cocomonyab.repository.tag.TagFilterConfigRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for TagFilterConfigService.
 * Tests Property 23 from the design document.
 * 
 * Note: Using JUnit @Test with manual property generation instead of jqwik
 * because jqwik doesn't support Spring's dependency injection well.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.mode=embedded",
    "spring.data.mongodb.embedded.storage.directory=data/db/mongo-test",
    "spring.data.mongodb.database=cocomonya"
})
class TagFilterConfigServicePropertyTest {
    
    @Autowired
    private TagFilterConfigService tagFilterConfigService;
    
    @Autowired
    private TagFilterConfigRepository configRepository;
    
    private final Random random = new Random();
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        configRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after tests
        configRepository.deleteAll();
    }
    
    /**
     * Feature: tag-database-and-filter-refactor, Property 23: 配置数据持久化完整性
     * 
     * 对于任何标签过滤配置，创建或更新后立即查询应该返回包含所有字段
     * （作者ID列表、角色ID列表、原作ID列表、自定义标签映射、匹配模式、启用状态）的完整数据。
     * 
     * **Validates: Requirements 6.2, 6.3, 6.4, 6.5**
     */
    @Test
    @Tag("tag-database-and-filter-refactor")
    @Tag("property-23")
    void configDataPersistenceIntegrity() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Generate random configuration data
            List<String> authorIds = generateRandomIdList(random.nextInt(5) + 1);
            List<String> characterIds = generateRandomIdList(random.nextInt(5) + 1);
            List<String> workIds = generateRandomIdList(random.nextInt(5) + 1);
            Map<String, String> customTags = generateRandomCustomTags(random.nextInt(5) + 1);
            String matchMode = random.nextBoolean() ? "whitelist" : "blacklist";
            Boolean enabled = random.nextBoolean();
            
            // Test 1: Create configuration and verify all fields
            TagFilterConfigCreateDTO createDTO = new TagFilterConfigCreateDTO();
            createDTO.setAuthorIds(authorIds);
            createDTO.setCharacterIds(characterIds);
            createDTO.setWorkIds(workIds);
            createDTO.setCustomTags(customTags);
            createDTO.setMatchMode(matchMode);
            createDTO.setEnabled(enabled);
            
            TagFilterConfigVO createdConfig = tagFilterConfigService.createOrUpdateGlobal(createDTO);
            
            // Verify created config has all fields
            assertNotNull(createdConfig, "Created config should not be null (iteration " + i + ")");
            assertNotNull(createdConfig.getId(), "Config ID should not be null (iteration " + i + ")");
            
            // Immediately query the config
            TagFilterConfigVO queriedConfig = tagFilterConfigService.getById(createdConfig.getId());
            
            // Verify all fields match
            assertNotNull(queriedConfig, "Queried config should not be null (iteration " + i + ")");
            assertEquals(createdConfig.getId(), queriedConfig.getId(),
                "Config ID should match (iteration " + i + ")");
            
            // Verify authorIds
            assertNotNull(queriedConfig.getAuthorIds(), "Author IDs should not be null (iteration " + i + ")");
            assertEquals(authorIds.size(), queriedConfig.getAuthorIds().size(),
                "Author IDs size should match (iteration " + i + ")");
            for (int j = 0; j < authorIds.size(); j++) {
                assertEquals(authorIds.get(j), queriedConfig.getAuthorIds().get(j),
                    "Author ID at index " + j + " should match (iteration " + i + ")");
            }
            
            // Verify characterIds
            assertNotNull(queriedConfig.getCharacterIds(), "Character IDs should not be null (iteration " + i + ")");
            assertEquals(characterIds.size(), queriedConfig.getCharacterIds().size(),
                "Character IDs size should match (iteration " + i + ")");
            for (int j = 0; j < characterIds.size(); j++) {
                assertEquals(characterIds.get(j), queriedConfig.getCharacterIds().get(j),
                    "Character ID at index " + j + " should match (iteration " + i + ")");
            }
            
            // Verify workIds
            assertNotNull(queriedConfig.getWorkIds(), "Work IDs should not be null (iteration " + i + ")");
            assertEquals(workIds.size(), queriedConfig.getWorkIds().size(),
                "Work IDs size should match (iteration " + i + ")");
            for (int j = 0; j < workIds.size(); j++) {
                assertEquals(workIds.get(j), queriedConfig.getWorkIds().get(j),
                    "Work ID at index " + j + " should match (iteration " + i + ")");
            }
            
            // Verify customTags
            assertNotNull(queriedConfig.getCustomTags(), "Custom tags should not be null (iteration " + i + ")");
            assertEquals(customTags.size(), queriedConfig.getCustomTags().size(),
                "Custom tags size should match (iteration " + i + ")");
            for (Map.Entry<String, String> entry : customTags.entrySet()) {
                assertTrue(queriedConfig.getCustomTags().containsKey(entry.getKey()),
                    "Custom tag key " + entry.getKey() + " should exist (iteration " + i + ")");
                assertEquals(entry.getValue(), queriedConfig.getCustomTags().get(entry.getKey()),
                    "Custom tag value for key " + entry.getKey() + " should match (iteration " + i + ")");
            }
            
            // Verify matchMode
            assertNotNull(queriedConfig.getMatchMode(), "Match mode should not be null (iteration " + i + ")");
            assertEquals(matchMode, queriedConfig.getMatchMode(),
                "Match mode should match (iteration " + i + ")");
            
            // Verify enabled
            assertNotNull(queriedConfig.getEnabled(), "Enabled status should not be null (iteration " + i + ")");
            assertEquals(enabled, queriedConfig.getEnabled(),
                "Enabled status should match (iteration " + i + ")");
            
            // Verify timestamps
            assertNotNull(queriedConfig.getCreateTime(), "Create time should not be null (iteration " + i + ")");
            assertNotNull(queriedConfig.getUpdateTime(), "Update time should not be null (iteration " + i + ")");
            
            // Test 2: Update configuration and verify all fields
            List<String> updatedAuthorIds = generateRandomIdList(random.nextInt(5) + 1);
            List<String> updatedCharacterIds = generateRandomIdList(random.nextInt(5) + 1);
            List<String> updatedWorkIds = generateRandomIdList(random.nextInt(5) + 1);
            Map<String, String> updatedCustomTags = generateRandomCustomTags(random.nextInt(5) + 1);
            String updatedMatchMode = random.nextBoolean() ? "whitelist" : "blacklist";
            Boolean updatedEnabled = random.nextBoolean();
            
            TagFilterConfigUpdateDTO updateDTO = new TagFilterConfigUpdateDTO();
            updateDTO.setAuthorIds(updatedAuthorIds);
            updateDTO.setCharacterIds(updatedCharacterIds);
            updateDTO.setWorkIds(updatedWorkIds);
            updateDTO.setCustomTags(updatedCustomTags);
            updateDTO.setMatchMode(updatedMatchMode);
            updateDTO.setEnabled(updatedEnabled);
            
            TagFilterConfigVO updatedConfig = tagFilterConfigService.update(createdConfig.getId(), updateDTO);
            
            // Immediately query the updated config
            TagFilterConfigVO queriedUpdatedConfig = tagFilterConfigService.getById(updatedConfig.getId());
            
            // Verify all updated fields match
            assertNotNull(queriedUpdatedConfig, "Queried updated config should not be null (iteration " + i + ")");
            
            // Verify updated authorIds
            assertEquals(updatedAuthorIds.size(), queriedUpdatedConfig.getAuthorIds().size(),
                "Updated author IDs size should match (iteration " + i + ")");
            for (int j = 0; j < updatedAuthorIds.size(); j++) {
                assertEquals(updatedAuthorIds.get(j), queriedUpdatedConfig.getAuthorIds().get(j),
                    "Updated author ID at index " + j + " should match (iteration " + i + ")");
            }
            
            // Verify updated characterIds
            assertEquals(updatedCharacterIds.size(), queriedUpdatedConfig.getCharacterIds().size(),
                "Updated character IDs size should match (iteration " + i + ")");
            for (int j = 0; j < updatedCharacterIds.size(); j++) {
                assertEquals(updatedCharacterIds.get(j), queriedUpdatedConfig.getCharacterIds().get(j),
                    "Updated character ID at index " + j + " should match (iteration " + i + ")");
            }
            
            // Verify updated workIds
            assertEquals(updatedWorkIds.size(), queriedUpdatedConfig.getWorkIds().size(),
                "Updated work IDs size should match (iteration " + i + ")");
            for (int j = 0; j < updatedWorkIds.size(); j++) {
                assertEquals(updatedWorkIds.get(j), queriedUpdatedConfig.getWorkIds().get(j),
                    "Updated work ID at index " + j + " should match (iteration " + i + ")");
            }
            
            // Verify updated customTags
            assertEquals(updatedCustomTags.size(), queriedUpdatedConfig.getCustomTags().size(),
                "Updated custom tags size should match (iteration " + i + ")");
            for (Map.Entry<String, String> entry : updatedCustomTags.entrySet()) {
                assertTrue(queriedUpdatedConfig.getCustomTags().containsKey(entry.getKey()),
                    "Updated custom tag key " + entry.getKey() + " should exist (iteration " + i + ")");
                assertEquals(entry.getValue(), queriedUpdatedConfig.getCustomTags().get(entry.getKey()),
                    "Updated custom tag value for key " + entry.getKey() + " should match (iteration " + i + ")");
            }
            
            // Verify updated matchMode
            assertEquals(updatedMatchMode, queriedUpdatedConfig.getMatchMode(),
                "Updated match mode should match (iteration " + i + ")");
            
            // Verify updated enabled
            assertEquals(updatedEnabled, queriedUpdatedConfig.getEnabled(),
                "Updated enabled status should match (iteration " + i + ")");
            
            // Clean up for next iteration
            configRepository.deleteAll();
        }
    }
    
    // Helper methods
    
    /**
     * Generate a list of random IDs
     */
    private List<String> generateRandomIdList(int count) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add("id_" + random.nextInt(1000000));
        }
        return ids;
    }
    
    /**
     * Generate a map of random custom tags
     */
    private Map<String, String> generateRandomCustomTags(int count) {
        Map<String, String> tags = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String key = "customTag_" + random.nextInt(1000000);
            String value = "tagValue_" + random.nextInt(1000000);
            tags.put(key, value);
        }
        return tags;
    }
}
