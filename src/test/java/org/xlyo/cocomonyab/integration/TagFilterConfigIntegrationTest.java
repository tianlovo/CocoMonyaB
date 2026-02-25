package org.xlyo.cocomonyab.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.xlyo.cocomonyab.domain.dto.tag.*;
import org.xlyo.cocomonyab.domain.vo.tag.AuthorVO;
import org.xlyo.cocomonyab.domain.vo.tag.CharacterVO;
import org.xlyo.cocomonyab.domain.vo.tag.TagFilterConfigVO;
import org.xlyo.cocomonyab.domain.vo.tag.WorkVO;
import org.xlyo.cocomonyab.repository.tag.AuthorRepository;
import org.xlyo.cocomonyab.repository.tag.CharacterRepository;
import org.xlyo.cocomonyab.repository.tag.TagFilterConfigRepository;
import org.xlyo.cocomonyab.repository.tag.WorkRepository;
import org.xlyo.cocomonyab.service.tag.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 标签过滤配置集成测试
 * 
 * 测试全局配置的创建和更新
 * 测试标签展开的端到端流程
 * 测试配置与标签库的集成
 * 
 * 使用Testcontainers提供真实MongoDB环境
 * 
 * 需求: 5.1, 5.3, 5.4, 7.5
 */
@SpringBootTest
@Testcontainers
class TagFilterConfigIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);
    
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private TagFilterConfigService tagFilterConfigService;
    
    @Autowired
    private TagExpansionService tagExpansionService;
    
    @Autowired
    private AuthorService authorService;
    
    @Autowired
    private WorkService workService;
    
    @Autowired
    private CharacterService characterService;
    
    @Autowired
    private TagFilterConfigRepository tagFilterConfigRepository;
    
    @Autowired
    private AuthorRepository authorRepository;
    
    @Autowired
    private WorkRepository workRepository;
    
    @Autowired
    private CharacterRepository characterRepository;
    
    private AuthorVO testAuthor;
    private WorkVO testWork;
    private CharacterVO testCharacter;
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        tagFilterConfigRepository.deleteAll();
        characterRepository.deleteAll();
        authorRepository.deleteAll();
        workRepository.deleteAll();
        
        // 创建测试数据
        AuthorCreateDTO authorDTO = new AuthorCreateDTO();
        authorDTO.setName("测试作者");
        authorDTO.setAliases(Arrays.asList("作者别名1", "作者别名2"));
        testAuthor = authorService.create(authorDTO);
        
        WorkCreateDTO workDTO = new WorkCreateDTO();
        workDTO.setName("测试原作");
        workDTO.setAliases(Arrays.asList("原作别名1", "原作别名2"));
        testWork = workService.create(workDTO);
        
        CharacterCreateDTO characterDTO = new CharacterCreateDTO();
        characterDTO.setName("测试角色");
        characterDTO.setAliases(Arrays.asList("角色别名1", "角色别名2"));
        characterDTO.setWorkId(testWork.getId());
        testCharacter = characterService.create(characterDTO);
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试数据
        tagFilterConfigRepository.deleteAll();
        characterRepository.deleteAll();
        authorRepository.deleteAll();
        workRepository.deleteAll();
    }
    
    /**
     * 测试全局配置的创建和更新
     * 需求: 5.1, 5.3, 5.4
     */
    @Test
    void testGlobalConfigCreationAndUpdate() {
        // 1. 创建全局配置
        TagFilterConfigCreateDTO createDTO = new TagFilterConfigCreateDTO();
        createDTO.setAuthorIds(Arrays.asList(testAuthor.getId()));
        createDTO.setCharacterIds(Arrays.asList(testCharacter.getId()));
        createDTO.setWorkIds(Arrays.asList(testWork.getId()));
        
        Map<String, String> customTags = new HashMap<>();
        customTags.put("custom1", "自定义标签1");
        customTags.put("custom2", "自定义标签2");
        createDTO.setCustomTags(customTags);
        
        createDTO.setMatchMode("whitelist");
        createDTO.setEnabled(true);
        
        TagFilterConfigVO created = tagFilterConfigService.createOrUpdateGlobal(createDTO);
        
        // 验证创建结果
        assertNotNull(created.getId());
        assertEquals(1, created.getAuthorIds().size());
        assertEquals(1, created.getCharacterIds().size());
        assertEquals(1, created.getWorkIds().size());
        assertEquals(2, created.getCustomTags().size());
        assertEquals("whitelist", created.getMatchMode());
        assertTrue(created.getEnabled());
        assertNotNull(created.getCreateTime());
        assertNotNull(created.getUpdateTime());
        
        // 2. 获取全局配置
        TagFilterConfigVO retrieved = tagFilterConfigService.getGlobal();
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        
        // 3. 更新全局配置
        TagFilterConfigUpdateDTO updateDTO = new TagFilterConfigUpdateDTO();
        updateDTO.setMatchMode("blacklist");
        updateDTO.setEnabled(false);
        
        TagFilterConfigVO updated = tagFilterConfigService.update(created.getId(), updateDTO);
        assertEquals("blacklist", updated.getMatchMode());
        assertFalse(updated.getEnabled());
        assertEquals(1, updated.getAuthorIds().size()); // 未更新的字段应保持不变
    }
    
    /**
     * 测试标签展开的端到端流程
     * 需求: 7.5
     */
    @Test
    void testTagExpansionEndToEnd() {
        // 创建配置
        TagFilterConfigCreateDTO createDTO = new TagFilterConfigCreateDTO();
        createDTO.setAuthorIds(Arrays.asList(testAuthor.getId()));
        createDTO.setCharacterIds(Arrays.asList(testCharacter.getId()));
        createDTO.setWorkIds(Arrays.asList(testWork.getId()));
        
        Map<String, String> customTags = new HashMap<>();
        customTags.put("custom1", "自定义标签1");
        customTags.put("custom2", "自定义标签2");
        createDTO.setCustomTags(customTags);
        
        createDTO.setMatchMode("whitelist");
        createDTO.setEnabled(true);
        
        TagFilterConfigVO config = tagFilterConfigService.createOrUpdateGlobal(createDTO);
        
        // 展开所有标签
        List<String> expandedTags = tagExpansionService.expandAll(config);
        
        // 验证展开结果
        assertNotNull(expandedTags);
        assertFalse(expandedTags.isEmpty());
        
        // 应该包含作者名称和别名
        assertTrue(expandedTags.contains("测试作者"));
        assertTrue(expandedTags.contains("作者别名1"));
        assertTrue(expandedTags.contains("作者别名2"));
        
        // 应该包含原作名称和别名
        assertTrue(expandedTags.contains("测试原作"));
        assertTrue(expandedTags.contains("原作别名1"));
        assertTrue(expandedTags.contains("原作别名2"));
        
        // 应该包含角色名称和别名
        assertTrue(expandedTags.contains("测试角色"));
        assertTrue(expandedTags.contains("角色别名1"));
        assertTrue(expandedTags.contains("角色别名2"));
        
        // 应该包含自定义标签
        assertTrue(expandedTags.contains("自定义标签1"));
        assertTrue(expandedTags.contains("自定义标签2"));
        
        // 验证去重（不应有重复项）
        assertEquals(expandedTags.size(), expandedTags.stream().distinct().count());
    }
    
    /**
     * 测试单个标签类型的展开
     * 需求: 7.1, 7.2, 7.3, 7.4
     */
    @Test
    void testIndividualTagExpansion() {
        // 测试作者标签展开
        List<String> authorTags = tagExpansionService.expandAuthor(testAuthor.getId());
        assertEquals(3, authorTags.size()); // 名称 + 2个别名
        assertTrue(authorTags.contains("测试作者"));
        assertTrue(authorTags.contains("作者别名1"));
        assertTrue(authorTags.contains("作者别名2"));
        
        // 测试原作标签展开
        List<String> workTags = tagExpansionService.expandWork(testWork.getId());
        assertEquals(3, workTags.size()); // 名称 + 2个别名
        assertTrue(workTags.contains("测试原作"));
        assertTrue(workTags.contains("原作别名1"));
        assertTrue(workTags.contains("原作别名2"));
        
        // 测试角色标签展开
        List<String> characterTags = tagExpansionService.expandCharacter(testCharacter.getId());
        assertEquals(3, characterTags.size()); // 名称 + 2个别名
        assertTrue(characterTags.contains("测试角色"));
        assertTrue(characterTags.contains("角色别名1"));
        assertTrue(characterTags.contains("角色别名2"));
        
        // 测试自定义标签展开
        TagFilterConfigCreateDTO createDTO = new TagFilterConfigCreateDTO();
        createDTO.setAuthorIds(Arrays.asList());
        createDTO.setCharacterIds(Arrays.asList());
        createDTO.setWorkIds(Arrays.asList());
        
        Map<String, String> customTags = new HashMap<>();
        customTags.put("custom1", "自定义标签1");
        createDTO.setCustomTags(customTags);
        
        createDTO.setMatchMode("whitelist");
        createDTO.setEnabled(true);
        
        TagFilterConfigVO config = tagFilterConfigService.createOrUpdateGlobal(createDTO);
        
        String customTag = tagExpansionService.expandCustomTag("custom1", config);
        assertEquals("自定义标签1", customTag);
    }
    
    /**
     * 测试标签展开的容错性
     * 需求: 7.6
     */
    @Test
    void testTagExpansionErrorHandling() {
        // 创建包含不存在ID的配置
        TagFilterConfigCreateDTO createDTO = new TagFilterConfigCreateDTO();
        createDTO.setAuthorIds(Arrays.asList(testAuthor.getId(), "nonexistent-author-id"));
        createDTO.setCharacterIds(Arrays.asList("nonexistent-character-id"));
        createDTO.setWorkIds(Arrays.asList(testWork.getId()));
        createDTO.setCustomTags(new HashMap<>());
        createDTO.setMatchMode("whitelist");
        createDTO.setEnabled(true);
        
        TagFilterConfigVO config = tagFilterConfigService.createOrUpdateGlobal(createDTO);
        
        // 展开应该跳过不存在的ID，不抛出异常
        assertDoesNotThrow(() -> {
            List<String> expandedTags = tagExpansionService.expandAll(config);
            
            // 应该包含存在的标签
            assertTrue(expandedTags.contains("测试作者"));
            assertTrue(expandedTags.contains("测试原作"));
            
            // 不应该因为不存在的ID而失败
            assertNotNull(expandedTags);
        });
    }
    
    /**
     * 测试配置与标签库的集成
     * 需求: 5.1, 5.3, 5.4
     */
    @Test
    void testConfigurationIntegrationWithTagDatabases() {
        // 创建多个作者、原作、角色
        AuthorCreateDTO author2DTO = new AuthorCreateDTO();
        author2DTO.setName("作者2");
        author2DTO.setAliases(Arrays.asList());
        AuthorVO author2 = authorService.create(author2DTO);
        
        WorkCreateDTO work2DTO = new WorkCreateDTO();
        work2DTO.setName("原作2");
        work2DTO.setAliases(Arrays.asList());
        WorkVO work2 = workService.create(work2DTO);
        
        CharacterCreateDTO character2DTO = new CharacterCreateDTO();
        character2DTO.setName("角色2");
        character2DTO.setAliases(Arrays.asList());
        character2DTO.setWorkId(work2.getId());
        CharacterVO character2 = characterService.create(character2DTO);
        
        // 创建包含多个标签的配置
        TagFilterConfigCreateDTO createDTO = new TagFilterConfigCreateDTO();
        createDTO.setAuthorIds(Arrays.asList(testAuthor.getId(), author2.getId()));
        createDTO.setCharacterIds(Arrays.asList(testCharacter.getId(), character2.getId()));
        createDTO.setWorkIds(Arrays.asList(testWork.getId(), work2.getId()));
        createDTO.setCustomTags(new HashMap<>());
        createDTO.setMatchMode("whitelist");
        createDTO.setEnabled(true);
        
        TagFilterConfigVO config = tagFilterConfigService.createOrUpdateGlobal(createDTO);
        
        // 验证配置正确保存了所有ID
        assertEquals(2, config.getAuthorIds().size());
        assertEquals(2, config.getCharacterIds().size());
        assertEquals(2, config.getWorkIds().size());
        
        // 展开标签
        List<String> expandedTags = tagExpansionService.expandAll(config);
        
        // 验证所有标签都被展开
        assertTrue(expandedTags.contains("测试作者"));
        assertTrue(expandedTags.contains("作者2"));
        assertTrue(expandedTags.contains("测试原作"));
        assertTrue(expandedTags.contains("原作2"));
        assertTrue(expandedTags.contains("测试角色"));
        assertTrue(expandedTags.contains("角色2"));
    }
    
    /**
     * 测试空配置的处理
     * 需求: 5.1, 5.3
     */
    @Test
    void testEmptyConfiguration() {
        // 创建空配置
        TagFilterConfigCreateDTO createDTO = new TagFilterConfigCreateDTO();
        createDTO.setAuthorIds(Arrays.asList());
        createDTO.setCharacterIds(Arrays.asList());
        createDTO.setWorkIds(Arrays.asList());
        createDTO.setCustomTags(new HashMap<>());
        createDTO.setMatchMode("whitelist");
        createDTO.setEnabled(true);
        
        TagFilterConfigVO config = tagFilterConfigService.createOrUpdateGlobal(createDTO);
        
        // 展开空配置应该返回空列表
        List<String> expandedTags = tagExpansionService.expandAll(config);
        assertNotNull(expandedTags);
        assertTrue(expandedTags.isEmpty());
    }
}
