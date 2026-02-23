package org.xlyo.cocomonyab.domain.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.repository.TagFilterConfigRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for TagFilterConfig entity data integrity.
 * Tests Property 1 from the design document.
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
class TagFilterConfigPropertyTest {
    
    @Autowired
    private TagFilterConfigRepository repository;
    
    private final Random random = new Random();
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        repository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after tests
        repository.deleteAll();
    }
    
    /**
     * Property 1: 配置数据完整性
     * 
     * For any successfully created TagFilterConfig, the configuration must contain
     * all required fields (id, tags, matchMode, enabled, createTime, updateTime),
     * and the values of these fields must conform to their type constraints.
     * 
     * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 1: 配置数据完整性")
    void configDataIntegrity() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Generate random valid configuration data
            Long channelId = generateChannelId();
            List<String> tags = generateTags();
            String matchMode = generateMatchMode();
            Boolean enabled = random.nextBoolean();
            
            // Create and save configuration
            TagFilterConfig config = new TagFilterConfig();
            config.setChannelId(channelId);
            config.setTags(tags);
            config.setMatchMode(matchMode);
            config.setEnabled(enabled);
            
            TagFilterConfig savedConfig = repository.save(config);
            
            // Verify all required fields are present and valid
            
            // 1. MongoDB ID must be generated (Requirement 1.1)
            assertNotNull(savedConfig.getId(), 
                "id should be generated after saving (iteration " + i + ")");
            assertFalse(savedConfig.getId().isEmpty(), 
                "id should not be empty (iteration " + i + ")");
            
            // 2. Tags list must be present (Requirement 1.2)
            assertNotNull(savedConfig.getTags(), 
                "tags should not be null (iteration " + i + ")");
            assertEquals(tags, savedConfig.getTags(), 
                "tags should match the input (iteration " + i + ")");
            
            // 3. Match mode must be present and valid (Requirement 1.3)
            assertNotNull(savedConfig.getMatchMode(), 
                "matchMode should not be null (iteration " + i + ")");
            assertTrue(savedConfig.getMatchMode().equals("whitelist") || 
                      savedConfig.getMatchMode().equals("blacklist"),
                "matchMode should be either whitelist or blacklist (iteration " + i + ")");
            assertEquals(matchMode, savedConfig.getMatchMode(), 
                "matchMode should match the input (iteration " + i + ")");
            
            // 4. Enabled status must be present (Requirement 1.4)
            assertNotNull(savedConfig.getEnabled(), 
                "enabled should not be null (iteration " + i + ")");
            assertEquals(enabled, savedConfig.getEnabled(), 
                "enabled should match the input (iteration " + i + ")");
            
            // 5. ChannelId must match input (Requirement 1.5)
            assertEquals(channelId, savedConfig.getChannelId(), 
                "channelId should match the input (iteration " + i + ")");
            
            // 6. Timestamps must be automatically set (Requirement 1.6)
            assertNotNull(savedConfig.getCreateTime(), 
                "createTime should be set after saving (iteration " + i + ")");
            assertNotNull(savedConfig.getUpdateTime(), 
                "updateTime should be set after saving (iteration " + i + ")");
            
            // Cleanup for this iteration
            repository.deleteById(savedConfig.getId());
        }
    }
    
    /**
     * Generates a channel ID (can be null for global config or negative Long for channel config)
     */
    private Long generateChannelId() {
        // 50% chance of null (global config), 50% chance of valid channel ID
        if (random.nextBoolean()) {
            return null;
        } else {
            // Telegram channel IDs are negative Long values
            return -Math.abs(random.nextLong(1000000000000L, 9999999999999L));
        }
    }
    
    /**
     * Generates a list of tags (can be empty but not null)
     */
    private List<String> generateTags() {
        int tagCount = random.nextInt(6); // 0 to 5 tags
        List<String> tags = new ArrayList<>();
        
        String[] possibleTags = {"tech", "news", "ai", "urgent", "important", 
                                 "ml", "data", "science", "business", "finance"};
        
        for (int i = 0; i < tagCount; i++) {
            tags.add(possibleTags[random.nextInt(possibleTags.length)]);
        }
        
        return tags;
    }
    
    /**
     * Generates a valid match mode (whitelist or blacklist)
     */
    private String generateMatchMode() {
        return random.nextBoolean() ? "whitelist" : "blacklist";
    }
}
