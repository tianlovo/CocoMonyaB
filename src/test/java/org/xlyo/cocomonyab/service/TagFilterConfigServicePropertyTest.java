package org.xlyo.cocomonyab.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;
import org.xlyo.cocomonyab.common.enums.ResponseCode;
import org.xlyo.cocomonyab.common.exception.BusinessException;
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigCreateDTO;
import org.xlyo.cocomonyab.domain.dto.TagFilterConfigUpdateDTO;
import org.xlyo.cocomonyab.domain.vo.TagFilterConfigVO;
import org.xlyo.cocomonyab.event.TagFilterConfigEvent;
import org.xlyo.cocomonyab.repository.TagFilterConfigRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for TagFilterConfigService business logic.
 * Tests Properties 2, 3, 5, 6, 7, 8, 9 from the design document.
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
    private TagFilterConfigService service;
    
    @Autowired
    private TagFilterConfigRepository repository;
    
    @Autowired
    private TestEventListener eventListener;
    
    private final Random random = new Random();
    
    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        repository.deleteAll();
        eventListener.clear();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after tests
        repository.deleteAll();
        eventListener.clear();
    }
    
    /**
     * Property 2: 全局配置唯一性
     * 
     * 对于任何系统状态，MongoDB中最多只能存在一个channelId为null的配置记录。
     * 
     * Validates: Requirements 2.4
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 2: 全局配置唯一性")
    void globalConfigUniqueness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            // Create first global config
            TagFilterConfigCreateDTO dto1 = createValidDTO(null);
            TagFilterConfigVO config1 = service.createOrUpdateGlobalConfig(dto1);
            assertNotNull(config1, "First global config should be created (iteration " + i + ")");
            assertNull(config1.getChannelId(), "Global config should have null channelId (iteration " + i + ")");
            
            // Create second global config with different values
            TagFilterConfigCreateDTO dto2 = createValidDTO(null);
            TagFilterConfigVO config2 = service.createOrUpdateGlobalConfig(dto2);
            assertNotNull(config2, "Second global config should be created (iteration " + i + ")");
            
            // Verify only one global config exists
            long globalConfigCount = repository.findByChannelIdIsNull().stream().count();
            assertEquals(1, globalConfigCount, 
                "Only one global config should exist in database (iteration " + i + ")");
            
            // Verify the second call updated the first config (same ID)
            assertEquals(config1.getId(), config2.getId(), 
                "Second createOrUpdateGlobalConfig should update the same config (iteration " + i + ")");
            
            // Cleanup
            repository.deleteAll();
        }
    }
    
    /**
     * Property 5: 全局配置事件发布
     * 
     * 对于任何全局配置的创建或更新操作，系统必须发布一个EventType为CONFIG_UPDATED且channelId为null的TagFilterConfigEvent。
     * 
     * Validates: Requirements 2.5, 5.3
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 5: 全局配置事件发布")
    void globalConfigEventPublishing() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            eventListener.clear();
            
            // Create global config
            TagFilterConfigCreateDTO createDTO = createValidDTO(null);
            TagFilterConfigVO created = service.createOrUpdateGlobalConfig(createDTO);
            
            // Verify event was published
            List<TagFilterConfigEvent> events = eventListener.getEvents();
            assertTrue(events.size() >= 1, 
                "At least one event should be published for global config creation (iteration " + i + ")");
            
            TagFilterConfigEvent createEvent = events.get(events.size() - 1);
            assertEquals(TagFilterConfigEvent.EventType.CONFIG_UPDATED, createEvent.getEventType(),
                "Event type should be CONFIG_UPDATED for global config creation (iteration " + i + ")");
            assertNull(createEvent.getChannelId(),
                "Event channelId should be null for global config (iteration " + i + ")");
            assertEquals(created.getId(), createEvent.getConfigId(),
                "Event configId should match created config ID (iteration " + i + ")");
            assertEquals(created.getEnabled(), createEvent.getEnabled(),
                "Event enabled status should match created config (iteration " + i + ")");
            
            // Update global config
            eventListener.clear();
            TagFilterConfigCreateDTO updateDTO = createValidDTO(null);
            TagFilterConfigVO updated = service.createOrUpdateGlobalConfig(updateDTO);
            
            // Verify update event was published
            events = eventListener.getEvents();
            assertTrue(events.size() >= 1, 
                "At least one event should be published for global config update (iteration " + i + ")");
            
            TagFilterConfigEvent updateEvent = events.get(events.size() - 1);
            assertEquals(TagFilterConfigEvent.EventType.CONFIG_UPDATED, updateEvent.getEventType(),
                "Event type should be CONFIG_UPDATED for global config update (iteration " + i + ")");
            assertNull(updateEvent.getChannelId(),
                "Event channelId should be null for global config update (iteration " + i + ")");
            assertEquals(updated.getId(), updateEvent.getConfigId(),
                "Event configId should match updated config ID (iteration " + i + ")");
            
            // Cleanup
            repository.deleteAll();
        }
    }
    
    /**
     * Property 3: 频道配置唯一性
     * 
     * 对于任何非null的channelId值，MongoDB中最多只能存在一个具有该channelId的配置记录。
     * 
     * Validates: Requirements 3.6, 8.6
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 3: 频道配置唯一性")
    void channelConfigUniqueness() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            Long channelId = generateValidChannelId();
            
            // Create first channel config
            TagFilterConfigCreateDTO dto1 = createValidDTO(channelId);
            TagFilterConfigVO config1 = service.createChannelConfig(dto1);
            assertNotNull(config1, "First channel config should be created (iteration " + i + ")");
            assertEquals(channelId, config1.getChannelId(), 
                "Channel config should have correct channelId (iteration " + i + ")");
            
            // Attempt to create second channel config with same channelId
            TagFilterConfigCreateDTO dto2 = createValidDTO(channelId);
            BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createChannelConfig(dto2),
                "Creating duplicate channel config should throw exception (iteration " + i + ")");
            
            assertEquals(ResponseCode.DATA_ALREADY_EXISTS.getCode(), exception.getCode(),
                "Exception should be DATA_ALREADY_EXISTS (iteration " + i + ")");
            assertTrue(exception.getMessage().contains(channelId.toString()),
                "Exception message should contain channelId (iteration " + i + ")");
            
            // Verify only one config exists for this channelId
            long count = repository.findByChannelId(channelId).stream().count();
            assertEquals(1, count, 
                "Only one config should exist for channelId (iteration " + i + ")");
            
            // Cleanup
            repository.deleteAll();
        }
    }
    
    /**
     * Property 6: 频道配置创建事件
     * 
     * 对于任何频道配置的创建操作，系统必须发布一个EventType为CONFIG_CREATED且包含正确channelId的TagFilterConfigEvent。
     * 
     * Validates: Requirements 3.5, 5.4
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 6: 频道配置创建事件")
    void channelConfigCreationEvent() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            eventListener.clear();
            Long channelId = generateValidChannelId();
            
            // Create channel config
            TagFilterConfigCreateDTO dto = createValidDTO(channelId);
            TagFilterConfigVO created = service.createChannelConfig(dto);
            
            // Verify event was published
            List<TagFilterConfigEvent> events = eventListener.getEvents();
            assertTrue(events.size() >= 1, 
                "At least one event should be published for channel config creation (iteration " + i + ")");
            
            TagFilterConfigEvent event = events.get(events.size() - 1);
            assertEquals(TagFilterConfigEvent.EventType.CONFIG_CREATED, event.getEventType(),
                "Event type should be CONFIG_CREATED (iteration " + i + ")");
            assertEquals(channelId, event.getChannelId(),
                "Event channelId should match created config (iteration " + i + ")");
            assertEquals(created.getId(), event.getConfigId(),
                "Event configId should match created config ID (iteration " + i + ")");
            assertEquals(created.getEnabled(), event.getEnabled(),
                "Event enabled status should match created config (iteration " + i + ")");
            
            // Cleanup
            repository.deleteAll();
        }
    }
    
    /**
     * Property 7: 频道配置更新事件
     * 
     * 对于任何频道配置的更新操作，系统必须发布一个EventType为CONFIG_UPDATED且包含正确channelId的TagFilterConfigEvent。
     * 
     * Validates: Requirements 3.5, 5.5
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 7: 频道配置更新事件")
    void channelConfigUpdateEvent() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            Long channelId = generateValidChannelId();
            
            // Create channel config
            TagFilterConfigCreateDTO createDTO = createValidDTO(channelId);
            TagFilterConfigVO created = service.createChannelConfig(createDTO);
            
            // Clear events and update config
            eventListener.clear();
            TagFilterConfigUpdateDTO updateDTO = new TagFilterConfigUpdateDTO();
            updateDTO.setEnabled(!created.getEnabled());
            TagFilterConfigVO updated = service.updateConfig(created.getId(), updateDTO);
            
            // Verify event was published
            List<TagFilterConfigEvent> events = eventListener.getEvents();
            assertTrue(events.size() >= 1, 
                "At least one event should be published for channel config update (iteration " + i + ")");
            
            TagFilterConfigEvent event = events.get(events.size() - 1);
            assertEquals(TagFilterConfigEvent.EventType.CONFIG_UPDATED, event.getEventType(),
                "Event type should be CONFIG_UPDATED (iteration " + i + ")");
            assertEquals(channelId, event.getChannelId(),
                "Event channelId should match updated config (iteration " + i + ")");
            assertEquals(updated.getId(), event.getConfigId(),
                "Event configId should match updated config ID (iteration " + i + ")");
            assertEquals(updated.getEnabled(), event.getEnabled(),
                "Event enabled status should match updated config (iteration " + i + ")");
            
            // Cleanup
            repository.deleteAll();
        }
    }
    
    /**
     * Property 8: 频道配置删除事件
     * 
     * 对于任何频道配置的删除操作，系统必须发布一个EventType为CONFIG_DELETED且包含正确channelId的TagFilterConfigEvent。
     * 
     * Validates: Requirements 3.5, 5.6
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 8: 频道配置删除事件")
    void channelConfigDeletionEvent() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            Long channelId = generateValidChannelId();
            
            // Create channel config
            TagFilterConfigCreateDTO createDTO = createValidDTO(channelId);
            TagFilterConfigVO created = service.createChannelConfig(createDTO);
            
            // Clear events and delete config
            eventListener.clear();
            service.deleteConfig(created.getId());
            
            // Verify event was published
            List<TagFilterConfigEvent> events = eventListener.getEvents();
            assertTrue(events.size() >= 1, 
                "At least one event should be published for channel config deletion (iteration " + i + ")");
            
            TagFilterConfigEvent event = events.get(events.size() - 1);
            assertEquals(TagFilterConfigEvent.EventType.CONFIG_DELETED, event.getEventType(),
                "Event type should be CONFIG_DELETED (iteration " + i + ")");
            assertEquals(channelId, event.getChannelId(),
                "Event channelId should match deleted config (iteration " + i + ")");
            assertEquals(created.getId(), event.getConfigId(),
                "Event configId should match deleted config ID (iteration " + i + ")");
            assertNull(event.getEnabled(),
                "Event enabled should be null for deletion (iteration " + i + ")");
            
            // Verify config was actually deleted
            assertFalse(repository.existsById(created.getId()),
                "Config should not exist after deletion (iteration " + i + ")");
        }
    }
    
    /**
     * Property 9: 配置优先级逻辑
     * 
     * 对于任何channelId，当查询有效配置时：如果存在该channelId的频道配置，则返回频道配置；
     * 否则，如果存在全局配置，则返回全局配置；否则抛出异常。
     * 
     * Validates: Requirements 2.6, 3.7, 4.4, 4.5
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 9: 配置优先级逻辑")
    void configPriorityLogic() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            Long channelId = generateValidChannelId();
            
            // Test Case 1: No configs exist - should throw exception
            BusinessException noConfigException = assertThrows(BusinessException.class,
                () -> service.getEffectiveConfig(channelId),
                "Should throw exception when no configs exist (iteration " + i + ")");
            assertEquals(ResponseCode.DATA_NOT_FOUND.getCode(), noConfigException.getCode(),
                "Exception should be DATA_NOT_FOUND (iteration " + i + ")");
            
            // Test Case 2: Only global config exists - should return global config
            TagFilterConfigCreateDTO globalDTO = createValidDTO(null);
            TagFilterConfigVO globalConfig = service.createOrUpdateGlobalConfig(globalDTO);
            
            TagFilterConfigVO effective1 = service.getEffectiveConfig(channelId);
            assertNotNull(effective1, "Effective config should not be null (iteration " + i + ")");
            assertEquals(globalConfig.getId(), effective1.getId(),
                "Should return global config when no channel config exists (iteration " + i + ")");
            assertNull(effective1.getChannelId(),
                "Effective config should be global config (iteration " + i + ")");
            
            // Test Case 3: Both global and channel configs exist - should return channel config
            TagFilterConfigCreateDTO channelDTO = createValidDTO(channelId);
            TagFilterConfigVO channelConfig = service.createChannelConfig(channelDTO);
            
            TagFilterConfigVO effective2 = service.getEffectiveConfig(channelId);
            assertNotNull(effective2, "Effective config should not be null (iteration " + i + ")");
            assertEquals(channelConfig.getId(), effective2.getId(),
                "Should return channel config when both exist (iteration " + i + ")");
            assertEquals(channelId, effective2.getChannelId(),
                "Effective config should be channel config (iteration " + i + ")");
            
            // Test Case 4: Delete channel config - should fall back to global config
            service.deleteConfig(channelConfig.getId());
            
            TagFilterConfigVO effective3 = service.getEffectiveConfig(channelId);
            assertNotNull(effective3, "Effective config should not be null (iteration " + i + ")");
            assertEquals(globalConfig.getId(), effective3.getId(),
                "Should return global config after channel config deleted (iteration " + i + ")");
            
            // Cleanup
            repository.deleteAll();
        }
    }
    
    /**
     * Property 18: 配置序列化往返一致性
     * 
     * 对于任何有效的TagFilterConfig对象，将其序列化为MongoDB文档后再反序列化，
     * 应得到与原对象等价的配置对象（所有字段值相同）。
     * 
     * Validates: Requirements 10.1, 10.2, 10.5
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 18: 配置序列化往返一致性")
    void configSerializationRoundTrip() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            Long channelId = random.nextBoolean() ? null : generateValidChannelId();
            
            // Create and save config
            TagFilterConfigCreateDTO createDTO = createValidDTO(channelId);
            TagFilterConfigVO created = channelId == null 
                ? service.createOrUpdateGlobalConfig(createDTO)
                : service.createChannelConfig(createDTO);
            
            // Retrieve from database
            TagFilterConfigVO retrieved = service.getConfigById(created.getId());
            
            // Verify all fields match
            assertEquals(created.getId(), retrieved.getId(),
                "ID should match after round-trip (iteration " + i + ")");
            assertEquals(created.getChannelId(), retrieved.getChannelId(),
                "ChannelId should match after round-trip (iteration " + i + ")");
            assertEquals(created.getTags(), retrieved.getTags(),
                "Tags should match after round-trip (iteration " + i + ")");
            assertEquals(created.getMatchMode(), retrieved.getMatchMode(),
                "MatchMode should match after round-trip (iteration " + i + ")");
            assertEquals(created.getEnabled(), retrieved.getEnabled(),
                "Enabled should match after round-trip (iteration " + i + ")");
            assertNotNull(retrieved.getCreateTime(),
                "CreateTime should not be null (iteration " + i + ")");
            assertNotNull(retrieved.getUpdateTime(),
                "UpdateTime should not be null (iteration " + i + ")");
            
            // Cleanup
            repository.deleteAll();
        }
    }
    
    /**
     * Property 19: DTO到Entity转换保真性
     * 
     * 对于任何有效的TagFilterConfigCreateDTO，将其转换为TagFilterConfig实体后，
     * 实体中的tags、matchMode和enabled字段必须与DTO中的对应字段值相同。
     * 
     * Validates: Requirements 10.4, 10.5
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 19: DTO到Entity转换保真性")
    void dtoToEntityConversionFidelity() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            Long channelId = random.nextBoolean() ? null : generateValidChannelId();
            TagFilterConfigCreateDTO dto = createValidDTO(channelId);
            
            // Create config (which internally converts DTO to Entity)
            TagFilterConfigVO created = channelId == null 
                ? service.createOrUpdateGlobalConfig(dto)
                : service.createChannelConfig(dto);
            
            // Verify DTO fields match created entity fields
            assertEquals(dto.getChannelId(), created.getChannelId(),
                "ChannelId should match DTO (iteration " + i + ")");
            assertEquals(dto.getTags(), created.getTags(),
                "Tags should match DTO (iteration " + i + ")");
            assertEquals(dto.getMatchMode(), created.getMatchMode(),
                "MatchMode should match DTO (iteration " + i + ")");
            assertEquals(dto.getEnabled(), created.getEnabled(),
                "Enabled should match DTO (iteration " + i + ")");
            
            // Cleanup
            repository.deleteAll();
        }
    }
    
    /**
     * Property 20: Entity到VO转换保真性
     * 
     * 对于任何TagFilterConfig实体，将其转换为TagFilterConfigVO后，
     * VO中的所有字段值必须与实体中的对应字段值相同。
     * 
     * Validates: Requirements 10.3, 10.5
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 20: Entity到VO转换保真性")
    void entityToVOConversionFidelity() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            Long channelId = random.nextBoolean() ? null : generateValidChannelId();
            
            // Create config
            TagFilterConfigCreateDTO createDTO = createValidDTO(channelId);
            TagFilterConfigVO created = channelId == null 
                ? service.createOrUpdateGlobalConfig(createDTO)
                : service.createChannelConfig(createDTO);
            
            // Retrieve config (which internally converts Entity to VO)
            TagFilterConfigVO retrieved = service.getConfigById(created.getId());
            
            // Verify all fields match
            assertEquals(created.getId(), retrieved.getId(),
                "ID should match (iteration " + i + ")");
            assertEquals(created.getChannelId(), retrieved.getChannelId(),
                "ChannelId should match (iteration " + i + ")");
            assertEquals(created.getTags(), retrieved.getTags(),
                "Tags should match (iteration " + i + ")");
            assertEquals(created.getMatchMode(), retrieved.getMatchMode(),
                "MatchMode should match (iteration " + i + ")");
            assertEquals(created.getEnabled(), retrieved.getEnabled(),
                "Enabled should match (iteration " + i + ")");
            assertEquals(created.getCreateTime(), retrieved.getCreateTime(),
                "CreateTime should match (iteration " + i + ")");
            assertEquals(created.getUpdateTime(), retrieved.getUpdateTime(),
                "UpdateTime should match (iteration " + i + ")");
            
            // Cleanup
            repository.deleteAll();
        }
    }
    
    /**
     * Property 10: 标签列表非空验证
     * 
     * 对于任何创建或更新配置的请求，如果tags字段为null，
     * 系统必须拒绝该请求并返回错误码-40006。
     * 
     * Validates: Requirements 8.1, 8.5
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 10: 标签列表非空验证")
    void tagsNotNullValidation() {
        // Run 100 iterations
        for (int i = 0; i < 100; i++) {
            // Test global config creation with null tags
            TagFilterConfigCreateDTO globalDTO = new TagFilterConfigCreateDTO();
            globalDTO.setChannelId(null);
            globalDTO.setTags(null); // Invalid: null tags
            globalDTO.setMatchMode(random.nextBoolean() ? "whitelist" : "blacklist");
            globalDTO.setEnabled(random.nextBoolean());
            
            BusinessException globalException = assertThrows(BusinessException.class,
                () -> service.createOrUpdateGlobalConfig(globalDTO),
                "Creating global config with null tags should throw exception (iteration " + i + ")");
            assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), globalException.getCode(),
                "Exception code should be VALIDATION_ERROR (-40006) (iteration " + i + ")");
            assertTrue(globalException.getMessage().contains("标签列表"),
                "Exception message should mention tags (iteration " + i + ")");
            
            // Test channel config creation with null tags
            TagFilterConfigCreateDTO channelDTO = new TagFilterConfigCreateDTO();
            channelDTO.setChannelId(generateValidChannelId());
            channelDTO.setTags(null); // Invalid: null tags
            channelDTO.setMatchMode(random.nextBoolean() ? "whitelist" : "blacklist");
            channelDTO.setEnabled(random.nextBoolean());
            
            BusinessException channelException = assertThrows(BusinessException.class,
                () -> service.createChannelConfig(channelDTO),
                "Creating channel config with null tags should throw exception (iteration " + i + ")");
            assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), channelException.getCode(),
                "Exception code should be VALIDATION_ERROR (-40006) (iteration " + i + ")");
            assertTrue(channelException.getMessage().contains("标签列表"),
                "Exception message should mention tags (iteration " + i + ")");
        }
    }
    
    /**
     * Property 11: 匹配模式验证
     * 
     * 对于任何创建或更新配置的请求，如果matchMode字段不是"whitelist"或"blacklist"，
     * 系统必须拒绝该请求并返回错误码-40006。
     * 
     * Validates: Requirements 8.2, 8.5
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 11: 匹配模式验证")
    void matchModeValidation() {
        String[] invalidModes = {"invalid", "white", "black", "WHITELIST", "BLACKLIST", "", "both", "none"};
        
        // Run 100 iterations with different invalid modes
        for (int i = 0; i < 100; i++) {
            String invalidMode = invalidModes[random.nextInt(invalidModes.length)];
            
            // Test global config creation with invalid matchMode
            TagFilterConfigCreateDTO globalDTO = new TagFilterConfigCreateDTO();
            globalDTO.setChannelId(null);
            globalDTO.setTags(generateRandomTags());
            globalDTO.setMatchMode(invalidMode); // Invalid mode
            globalDTO.setEnabled(random.nextBoolean());
            
            BusinessException globalException = assertThrows(BusinessException.class,
                () -> service.createOrUpdateGlobalConfig(globalDTO),
                "Creating global config with invalid matchMode should throw exception (iteration " + i + ")");
            assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), globalException.getCode(),
                "Exception code should be VALIDATION_ERROR (-40006) (iteration " + i + ")");
            assertTrue(globalException.getMessage().contains("匹配模式"),
                "Exception message should mention matchMode (iteration " + i + ")");
            
            // Test channel config creation with invalid matchMode
            TagFilterConfigCreateDTO channelDTO = new TagFilterConfigCreateDTO();
            channelDTO.setChannelId(generateValidChannelId());
            channelDTO.setTags(generateRandomTags());
            channelDTO.setMatchMode(invalidMode); // Invalid mode
            channelDTO.setEnabled(random.nextBoolean());
            
            BusinessException channelException = assertThrows(BusinessException.class,
                () -> service.createChannelConfig(channelDTO),
                "Creating channel config with invalid matchMode should throw exception (iteration " + i + ")");
            assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), channelException.getCode(),
                "Exception code should be VALIDATION_ERROR (-40006) (iteration " + i + ")");
            assertTrue(channelException.getMessage().contains("匹配模式"),
                "Exception message should mention matchMode (iteration " + i + ")");
            
            // Test update with invalid matchMode
            // First create a valid config
            TagFilterConfigCreateDTO validDTO = createValidDTO(generateValidChannelId());
            TagFilterConfigVO created = service.createChannelConfig(validDTO);
            
            // Try to update with invalid matchMode
            TagFilterConfigUpdateDTO updateDTO = new TagFilterConfigUpdateDTO();
            updateDTO.setMatchMode(invalidMode); // Invalid mode
            
            BusinessException updateException = assertThrows(BusinessException.class,
                () -> service.updateConfig(created.getId(), updateDTO),
                "Updating config with invalid matchMode should throw exception (iteration " + i + ")");
            assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), updateException.getCode(),
                "Exception code should be VALIDATION_ERROR (-40006) (iteration " + i + ")");
            assertTrue(updateException.getMessage().contains("匹配模式"),
                "Exception message should mention matchMode (iteration " + i + ")");
            
            // Cleanup
            repository.deleteAll();
        }
        
        // Test null matchMode
        TagFilterConfigCreateDTO nullModeDTO = new TagFilterConfigCreateDTO();
        nullModeDTO.setChannelId(null);
        nullModeDTO.setTags(generateRandomTags());
        nullModeDTO.setMatchMode(null); // Invalid: null
        nullModeDTO.setEnabled(true);
        
        BusinessException nullException = assertThrows(BusinessException.class,
            () -> service.createOrUpdateGlobalConfig(nullModeDTO),
            "Creating config with null matchMode should throw exception");
        assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), nullException.getCode(),
            "Exception code should be VALIDATION_ERROR (-40006)");
    }
    
    /**
     * Property 12: 启用状态验证
     * 
     * 对于任何创建或更新配置的请求，如果enabled字段为null，
     * 系统必须拒绝该请求并返回错误码-40006。
     * 
     * Validates: Requirements 8.3, 8.5
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 12: 启用状态验证")
    void enabledNotNullValidation() {
        // Run 100 iterations
        for (int i = 0; i < 100; i++) {
            // Test global config creation with null enabled
            TagFilterConfigCreateDTO globalDTO = new TagFilterConfigCreateDTO();
            globalDTO.setChannelId(null);
            globalDTO.setTags(generateRandomTags());
            globalDTO.setMatchMode(random.nextBoolean() ? "whitelist" : "blacklist");
            globalDTO.setEnabled(null); // Invalid: null enabled
            
            BusinessException globalException = assertThrows(BusinessException.class,
                () -> service.createOrUpdateGlobalConfig(globalDTO),
                "Creating global config with null enabled should throw exception (iteration " + i + ")");
            assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), globalException.getCode(),
                "Exception code should be VALIDATION_ERROR (-40006) (iteration " + i + ")");
            assertTrue(globalException.getMessage().contains("启用状态"),
                "Exception message should mention enabled status (iteration " + i + ")");
            
            // Test channel config creation with null enabled
            TagFilterConfigCreateDTO channelDTO = new TagFilterConfigCreateDTO();
            channelDTO.setChannelId(generateValidChannelId());
            channelDTO.setTags(generateRandomTags());
            channelDTO.setMatchMode(random.nextBoolean() ? "whitelist" : "blacklist");
            channelDTO.setEnabled(null); // Invalid: null enabled
            
            BusinessException channelException = assertThrows(BusinessException.class,
                () -> service.createChannelConfig(channelDTO),
                "Creating channel config with null enabled should throw exception (iteration " + i + ")");
            assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), channelException.getCode(),
                "Exception code should be VALIDATION_ERROR (-40006) (iteration " + i + ")");
            assertTrue(channelException.getMessage().contains("启用状态"),
                "Exception message should mention enabled status (iteration " + i + ")");
        }
    }
    
    /**
     * Property 13: 频道ID验证
     * 
     * 对于任何创建频道配置的请求，如果channelId为null或不是有效的Telegram频道ID格式（负数Long），
     * 系统必须拒绝该请求并返回错误码-40006。
     * 
     * Validates: Requirements 8.4, 8.5
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 13: 频道ID验证")
    void channelIdValidation() {
        // Run 100 iterations
        for (int i = 0; i < 100; i++) {
            // Test 1: null channelId
            TagFilterConfigCreateDTO nullChannelDTO = new TagFilterConfigCreateDTO();
            nullChannelDTO.setChannelId(null); // Invalid: null for channel config
            nullChannelDTO.setTags(generateRandomTags());
            nullChannelDTO.setMatchMode(random.nextBoolean() ? "whitelist" : "blacklist");
            nullChannelDTO.setEnabled(random.nextBoolean());
            
            BusinessException nullException = assertThrows(BusinessException.class,
                () -> service.createChannelConfig(nullChannelDTO),
                "Creating channel config with null channelId should throw exception (iteration " + i + ")");
            assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), nullException.getCode(),
                "Exception code should be VALIDATION_ERROR (-40006) (iteration " + i + ")");
            assertTrue(nullException.getMessage().contains("channelId"),
                "Exception message should mention channelId (iteration " + i + ")");
            
            // Test 2: positive channelId (invalid Telegram ID)
            Long positiveChannelId = Math.abs(random.nextLong()) % 9000000000000L + 1000000000000L;
            TagFilterConfigCreateDTO positiveChannelDTO = new TagFilterConfigCreateDTO();
            positiveChannelDTO.setChannelId(positiveChannelId); // Invalid: positive
            positiveChannelDTO.setTags(generateRandomTags());
            positiveChannelDTO.setMatchMode(random.nextBoolean() ? "whitelist" : "blacklist");
            positiveChannelDTO.setEnabled(random.nextBoolean());
            
            BusinessException positiveException = assertThrows(BusinessException.class,
                () -> service.createChannelConfig(positiveChannelDTO),
                "Creating channel config with positive channelId should throw exception (iteration " + i + ")");
            assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), positiveException.getCode(),
                "Exception code should be VALIDATION_ERROR (-40006) (iteration " + i + ")");
            assertTrue(positiveException.getMessage().contains("负数") || 
                      positiveException.getMessage().contains("channelId"),
                "Exception message should mention negative number or channelId (iteration " + i + ")");
            
            // Test 3: zero channelId (invalid)
            TagFilterConfigCreateDTO zeroChannelDTO = new TagFilterConfigCreateDTO();
            zeroChannelDTO.setChannelId(0L); // Invalid: zero
            zeroChannelDTO.setTags(generateRandomTags());
            zeroChannelDTO.setMatchMode(random.nextBoolean() ? "whitelist" : "blacklist");
            zeroChannelDTO.setEnabled(random.nextBoolean());
            
            BusinessException zeroException = assertThrows(BusinessException.class,
                () -> service.createChannelConfig(zeroChannelDTO),
                "Creating channel config with zero channelId should throw exception (iteration " + i + ")");
            assertEquals(ResponseCode.VALIDATION_ERROR.getCode(), zeroException.getCode(),
                "Exception code should be VALIDATION_ERROR (-40006) (iteration " + i + ")");
            
            // Test 4: valid negative channelId should succeed
            Long validChannelId = generateValidChannelId();
            TagFilterConfigCreateDTO validDTO = new TagFilterConfigCreateDTO();
            validDTO.setChannelId(validChannelId);
            validDTO.setTags(generateRandomTags());
            validDTO.setMatchMode(random.nextBoolean() ? "whitelist" : "blacklist");
            validDTO.setEnabled(random.nextBoolean());
            
            TagFilterConfigVO created = service.createChannelConfig(validDTO);
            assertNotNull(created, "Valid negative channelId should succeed (iteration " + i + ")");
            assertEquals(validChannelId, created.getChannelId(),
                "Created config should have correct channelId (iteration " + i + ")");
            
            // Cleanup
            repository.deleteAll();
        }
    }

    /**
     * Property 14: MongoDB ID自动生成
     *
     * 对于任何新创建的配置，MongoDB必须自动生成一个唯一的文档ID，并且该ID在响应中可见
     *
     * Validates: Requirements 7.3
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 14: MongoDB ID自动生成")
    void mongodbIdAutoGeneration() {
        // 使用随机有效输入运行100次迭代
        for (int i = 0; i < 100; i++) {
            Long channelId = random.nextBoolean() ? null : generateValidChannelId();

            // 创建配置（不设置ID，让MongoDB自动生成）
            TagFilterConfigCreateDTO dto = createValidDTO(channelId);
            TagFilterConfigVO created = channelId == null
                ? service.createOrUpdateGlobalConfig(dto)
                : service.createChannelConfig(dto);

            // 验证ID已自动生成且不为null
            assertNotNull(created.getId(),
                "MongoDB ID应该自动生成且不为null (迭代 " + i + ")");

            // 验证ID不为空字符串
            assertFalse(created.getId().isEmpty(),
                "MongoDB ID不应该是空字符串 (迭代 " + i + ")");

            // 验证ID格式（MongoDB ObjectId是24位十六进制字符串）
            assertTrue(created.getId().matches("^[a-f0-9]{24}$"),
                "MongoDB ID应该是有效的24字符十六进制字符串 (迭代 " + i + ")");

            // 验证ID在数据库中可查询
            TagFilterConfigVO retrieved = service.getConfigById(created.getId());
            assertNotNull(retrieved,
                "配置应该可以通过自动生成的ID查询到 (迭代 " + i + ")");
            assertEquals(created.getId(), retrieved.getId(),
                "查询到的配置应该有相同的ID (迭代 " + i + ")");

            // 创建第二个配置，验证ID唯一性
            Long channelId2 = random.nextBoolean() ? null : generateValidChannelId();
            // 如果两次都是全局配置，第二次会更新第一次，所以确保至少有一个是频道配置
            if (channelId == null && channelId2 == null) {
                channelId2 = generateValidChannelId();
            }
            
            TagFilterConfigCreateDTO dto2 = createValidDTO(channelId2);
            TagFilterConfigVO created2 = channelId2 == null
                ? service.createOrUpdateGlobalConfig(dto2)
                : service.createChannelConfig(dto2);

            // 验证第二个配置也有自动生成的ID
            assertNotNull(created2.getId(),
                "第二个配置也应该有自动生成的ID (迭代 " + i + ")");

            // 验证两个ID不同（除非是全局配置更新的情况）
            if (channelId != null || channelId2 != null) {
                // 至少有一个是频道配置，ID应该不同
                if (channelId != null && channelId2 != null) {
                    assertNotEquals(created.getId(), created2.getId(),
                        "不同的配置应该有不同的ID (迭代 " + i + ")");
                }
            }

            // 清理
            repository.deleteAll();
        }
    }

    /**
     * Property 15: 创建时间自动设置
     *
     * 对于任何新创建的配置，createTime字段必须自动设置为创建时的当前时间戳。
     *
     * Validates: Requirements 7.4
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 15: 创建时间自动设置")
    void createTimeAutoSet() {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            Long channelId = random.nextBoolean() ? null : generateValidChannelId();

            // 记录创建前的时间
            java.time.LocalDateTime beforeCreate = java.time.LocalDateTime.now();

            // 创建配置
            TagFilterConfigCreateDTO dto = createValidDTO(channelId);
            TagFilterConfigVO created = channelId == null
                ? service.createOrUpdateGlobalConfig(dto)
                : service.createChannelConfig(dto);

            // 记录创建后的时间
            java.time.LocalDateTime afterCreate = java.time.LocalDateTime.now();

            // 验证createTime已设置
            assertNotNull(created.getCreateTime(),
                "CreateTime should not be null (iteration " + i + ")");

            // 验证createTime在合理范围内（在创建前后之间）
            assertTrue(
                !created.getCreateTime().isBefore(beforeCreate) &&
                !created.getCreateTime().isAfter(afterCreate),
                "CreateTime should be set to current time at creation (iteration " + i + ")");

            // 验证createTime也已持久化到数据库
            TagFilterConfigVO retrieved = service.getConfigById(created.getId());
            assertNotNull(retrieved.getCreateTime(),
                "CreateTime should be persisted in database (iteration " + i + ")");
            assertEquals(created.getCreateTime(), retrieved.getCreateTime(),
                "CreateTime should match between created and retrieved (iteration " + i + ")");

            // 清理
            repository.deleteAll();
        }
    }

    /**
     * Property 16: 更新时间自动更新
     *
     * 对于任何配置的更新操作，updateTime字段必须自动更新为更新时的当前时间戳，
     * 且新的updateTime必须晚于或等于原updateTime。
     *
     * Validates: Requirements 7.5
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 16: 更新时间自动更新")
    void updateTimeAutoUpdate() throws InterruptedException {
        // Run 100 iterations with random valid inputs
        for (int i = 0; i < 100; i++) {
            Long channelId = random.nextBoolean() ? null : generateValidChannelId();

            // 创建初始配置
            TagFilterConfigCreateDTO createDTO = createValidDTO(channelId);
            TagFilterConfigVO created = channelId == null
                ? service.createOrUpdateGlobalConfig(createDTO)
                : service.createChannelConfig(createDTO);

            // 验证初始updateTime已设置
            assertNotNull(created.getUpdateTime(),
                "Initial updateTime should not be null (iteration " + i + ")");

            java.time.LocalDateTime initialUpdateTime = created.getUpdateTime();

            // 等待一小段时间以确保时间差异
            Thread.sleep(10);

            // 记录更新前的时间
            java.time.LocalDateTime beforeUpdate = java.time.LocalDateTime.now();

            // 更新配置
            TagFilterConfigUpdateDTO updateDTO = new TagFilterConfigUpdateDTO();
            updateDTO.setEnabled(!created.getEnabled());
            TagFilterConfigVO updated = channelId == null
                ? service.createOrUpdateGlobalConfig(createDTO) // 对于全局配置，使用createOrUpdate
                : service.updateConfig(created.getId(), updateDTO);

            // 记录更新后的时间
            java.time.LocalDateTime afterUpdate = java.time.LocalDateTime.now();

            // 验证updateTime已更新
            assertNotNull(updated.getUpdateTime(),
                "UpdateTime should not be null after update (iteration " + i + ")");

            // 验证新的updateTime晚于或等于初始updateTime
            assertTrue(
                !updated.getUpdateTime().isBefore(initialUpdateTime),
                "New updateTime should be >= initial updateTime (iteration " + i + ")");

            // 验证updateTime在合理范围内（在更新前后之间）
            assertTrue(
                !updated.getUpdateTime().isBefore(beforeUpdate) &&
                !updated.getUpdateTime().isAfter(afterUpdate),
                "UpdateTime should be set to current time at update (iteration " + i + ")");

            // 验证updateTime也已持久化到数据库
            TagFilterConfigVO retrieved = service.getConfigById(updated.getId());
            assertNotNull(retrieved.getUpdateTime(),
                "UpdateTime should be persisted in database (iteration " + i + ")");
            assertEquals(updated.getUpdateTime(), retrieved.getUpdateTime(),
                "UpdateTime should match between updated and retrieved (iteration " + i + ")");

            // 验证createTime保持不变
            assertEquals(created.getCreateTime(), updated.getCreateTime(),
                "CreateTime should not change on update (iteration " + i + ")");

            // 清理
            repository.deleteAll();
        }
    }


    /**
     * Property 4: 配置类型识别
     * 
     * 对于任何TagFilterConfig，当且仅当其channelId为null时，它应被识别为全局配置；
     * 当且仅当其channelId非null时，它应被识别为频道配置
     * 
     * Validates: Requirements 1.7, 1.8
     */
    @Test
    @Tag("Feature: tag-filter-config, Property 4: 配置类型识别")
    void configTypeIdentification() {
        // 使用随机有效输入运行100次迭代
        for (int i = 0; i < 100; i++) {
            // 测试1: 创建全局配置 (channelId = null)
            TagFilterConfigCreateDTO globalDTO = createValidDTO(null);
            TagFilterConfigVO globalConfig = service.createOrUpdateGlobalConfig(globalDTO);
            
            // 验证全局配置被正确识别
            assertNull(globalConfig.getChannelId(), 
                "Global config should have null channelId (iteration " + i + ")");
            
            // 验证在数据库中正确存储
            TagFilterConfigVO retrievedGlobal = service.getConfigById(globalConfig.getId());
            assertNull(retrievedGlobal.getChannelId(), 
                "Retrieved global config should have null channelId (iteration " + i + ")");
            
            // 测试2: 创建频道配置 (channelId != null)
            Long channelId = generateValidChannelId();
            TagFilterConfigCreateDTO channelDTO = createValidDTO(channelId);
            TagFilterConfigVO channelConfig = service.createChannelConfig(channelDTO);
            
            // 验证频道配置被正确识别
            assertNotNull(channelConfig.getChannelId(), 
                "Channel config should have non-null channelId (iteration " + i + ")");
            assertEquals(channelId, channelConfig.getChannelId(), 
                "Channel config should have the specified channelId (iteration " + i + ")");
            
            // 验证在数据库中正确存储
            TagFilterConfigVO retrievedChannel = service.getConfigById(channelConfig.getId());
            assertNotNull(retrievedChannel.getChannelId(), 
                "Retrieved channel config should have non-null channelId (iteration " + i + ")");
            assertEquals(channelId, retrievedChannel.getChannelId(), 
                "Retrieved channel config should have the specified channelId (iteration " + i + ")");
            
            // 测试3: 验证它们是不同的类型
            assertNotEquals(globalConfig.getId(), channelConfig.getId(), 
                "Global and channel configs should have different IDs (iteration " + i + ")");
            
            // 测试4: 验证全局配置可以通过全局查询检索
            TagFilterConfigVO globalByQuery = service.getGlobalConfig();
            assertNotNull(globalByQuery, 
                "Global config should be retrievable by getGlobalConfig (iteration " + i + ")");
            assertEquals(globalConfig.getId(), globalByQuery.getId(), 
                "getGlobalConfig should return the global config (iteration " + i + ")");
            assertNull(globalByQuery.getChannelId(), 
                "Config from getGlobalConfig should have null channelId (iteration " + i + ")");
            
            // 测试5: 验证频道配置可以通过channelId查询检索
            TagFilterConfigVO channelByQuery = service.getConfigByChannelId(channelId);
            assertNotNull(channelByQuery, 
                "Channel config should be retrievable by channelId (iteration " + i + ")");
            assertEquals(channelConfig.getId(), channelByQuery.getId(), 
                "getConfigByChannelId should return the channel config (iteration " + i + ")");
            assertEquals(channelId, channelByQuery.getChannelId(), 
                "Config from getConfigByChannelId should have the specified channelId (iteration " + i + ")");
            
            // 测试6: 验证类型识别在更新后保持一致
            TagFilterConfigUpdateDTO updateDTO = new TagFilterConfigUpdateDTO();
            updateDTO.setEnabled(!channelConfig.getEnabled());
            TagFilterConfigVO updatedChannel = service.updateConfig(channelConfig.getId(), updateDTO);
            
            // 频道配置在更新后应保持为频道配置
            assertNotNull(updatedChannel.getChannelId(), 
                "Updated channel config should still have non-null channelId (iteration " + i + ")");
            assertEquals(channelId, updatedChannel.getChannelId(), 
                "Updated channel config should maintain the same channelId (iteration " + i + ")");
            
            // 全局配置在更新后应保持为全局配置
            TagFilterConfigCreateDTO updatedGlobalDTO = createValidDTO(null);
            TagFilterConfigVO updatedGlobal = service.createOrUpdateGlobalConfig(updatedGlobalDTO);
            assertNull(updatedGlobal.getChannelId(), 
                "Updated global config should still have null channelId (iteration " + i + ")");
            
            // 清理
            repository.deleteAll();
        }
    }
    
    
    // Helper methods
    
    /**
     * Creates a valid TagFilterConfigCreateDTO with random values
     */
    private TagFilterConfigCreateDTO createValidDTO(Long channelId) {
        TagFilterConfigCreateDTO dto = new TagFilterConfigCreateDTO();
        dto.setChannelId(channelId);
        dto.setTags(generateRandomTags());
        dto.setMatchMode(random.nextBoolean() ? "whitelist" : "blacklist");
        dto.setEnabled(random.nextBoolean());
        return dto;
    }
    
    /**
     * Generates a valid Telegram channel ID (negative Long)
     */
    private Long generateValidChannelId() {
        return -(Math.abs(random.nextLong()) % 9000000000000L + 1000000000000L);
    }
    
    /**
     * Generates a random list of tags
     */
    private List<String> generateRandomTags() {
        int count = random.nextInt(5) + 1; // 1 to 5 tags
        List<String> tags = new ArrayList<>();
        String[] possibleTags = {"tech", "news", "ai", "urgent", "important", "test", "dev", "prod"};
        for (int i = 0; i < count; i++) {
            tags.add(possibleTags[random.nextInt(possibleTags.length)]);
        }
        return tags;
    }
    
    /**
     * Test event listener component to capture published events
     */
    @Component
    public static class TestEventListener implements ApplicationListener<TagFilterConfigEvent> {
        private final List<TagFilterConfigEvent> events = new CopyOnWriteArrayList<>();
        
        @Override
        public void onApplicationEvent(TagFilterConfigEvent event) {
            events.add(event);
        }
        
        public List<TagFilterConfigEvent> getEvents() {
            return new ArrayList<>(events);
        }
        
        public void clear() {
            events.clear();
        }
    }
}
